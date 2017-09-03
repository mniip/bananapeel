package com.mniip.bananapeel.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.mniip.bananapeel.R;

import java.util.ArrayList;
import java.util.List;

public class SelectableScrollbackView extends RecyclerView
{
	GestureDetector gestureDetector;

	public SelectableScrollbackView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initialize();
	}

	public SelectableScrollbackView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initialize();
	}

	public SelectableScrollbackView(Context context)
	{
		super(context);
		initialize();
	}

	private void initialize()
	{
		gestureDetector = new GestureDetector(getContext(), new GestureDetector.OnGestureListener()
		{
			@Override
			public boolean onDown(MotionEvent ev)
			{
				return false;
			}

			@Override
			public void onShowPress(MotionEvent ev) { }

			@Override
			public boolean onSingleTapUp(MotionEvent ev)
			{
				if(selecting)
				{
					stopSelecting();
					return true;
				}
				return false;
			}

			@Override
			public boolean onScroll(MotionEvent ev1, MotionEvent ev2, float distanceX, float distanceY)
			{
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e)
			{
				onLongPressed(e);
			}

			@Override
			public boolean onFling(MotionEvent ev1, MotionEvent ev2, float velocityX, float velocityY)
			{
				return false;
			}
		});

		startHandle = new CursorHandle(this, getContext().getResources().getDrawable(R.drawable.cursor_left));
		endHandle = new CursorHandle(this, getContext().getResources().getDrawable(R.drawable.cursor_right));
	}

	private boolean selecting = false;

	private static class CursorHandle extends View
	{
		SelectableScrollbackView view;
		private PopupWindow window;
		private Drawable drawable;

		private int line, pos;

		private CursorHandle(SelectableScrollbackView view, Drawable drawable)
		{
			super(view.getContext());
			this.view = view;
			this.window = new PopupWindow(view.getContext());
			this.drawable = drawable;
			window.setContentView(this);
			window.setBackgroundDrawable(null);
			window.setClippingEnabled(false);
		}

		@Override
		protected void onMeasure(int widthSpec, int heightSpec)
		{
			setMeasuredDimension(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
		}

		@Override
		protected void onDraw(Canvas canvas)
		{
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			drawable.draw(canvas);
		}

		boolean dragging = false;
		float dragX, dragY;

		@Override
		public boolean onTouchEvent(MotionEvent ev)
		{
			float rawX = ev.getRawX();
			float rawY = ev.getRawY();
			switch(ev.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					float[] screenCoords = view.getScreenCoordsForPos(line, pos);
					dragX = screenCoords[0] - rawX;
					dragY = screenCoords[1] - rawY;
					dragging = true;
					return true;
				case MotionEvent.ACTION_MOVE:
					if(dragging)
					{
						float newScreenX = dragX + rawX;
						float newScreenY = dragY + rawY;
						int[] position = view.getPosForScreenCoords(newScreenX, newScreenY - yOffset);
						if(position != null)
						{
							line = position[0];
							pos = position[1];
							for(OnSelectionChangedListener listener : view.selectionChangedListeners)
								listener.onSelectionChanged();
							view.updateHandles();
						}
						return true;
					}
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					if(dragging)
					{
						dragging = false;
						return true;
					}
			}
			return false;
		}

		private float yOffset;

		private void updateWindow(float x, float y, float yOffset)
		{
			if(view.selecting)
			{
				int screenCoords[] = new int[2];
				view.getLocationOnScreen(screenCoords);
				x += screenCoords[0];
				y += screenCoords[1];
				if(!window.isShowing())
					window.showAtLocation(view, Gravity.NO_GRAVITY, (int)x, (int)(y + yOffset));
				else
					window.update((int)x, (int)(y + yOffset), -1, -1, true);
				this.yOffset = yOffset;
			}
			else
				if(window.isShowing())
					window.dismiss();
		}
	}

	private CursorHandle startHandle, endHandle;

	private float[] getScreenCoordsForPos(int line, int pos)
	{
		LayoutManager manager = getLayoutManager();
		ViewHolder holder = findViewHolderForAdapterPosition(line);
		if(holder == null)
		{
			if(getChildCount() > 0 && getChildAdapterPosition(getChildAt(getChildCount() - 1)) < line)
				return new float[]{getWidth() / 2, getHeight(), 0};
			return new float[]{getWidth() / 2, 0, 0};
		}
		View view = manager.findViewByPosition(findViewHolderForAdapterPosition(line).getLayoutPosition());
		Layout layout = ((TextView)view).getLayout();
		float x = layout.getPrimaryHorizontal(pos);
		int ln = layout.getLineForOffset(pos);
		float y = (float)(layout.getLineTop(ln) + layout.getLineBottom(ln)) / 2;
		float screenX = x + view.getLeft() + getScrollX();
		float screenY = y + view.getTop() + getScrollY();
		if(screenY < 0)
			screenY = 0;
		if(screenY > getBottom())
			screenY = getBottom();
		return new float[]{screenX, screenY, layout.getLineBottom(ln) - y};
	}

	private int[] getPosForScreenCoords(float x, float y)
	{
		x -= getScrollX();
		y -= getScrollY();
		View view = findChildViewUnder(x, y);
		if(view == null)
			return null;
		Layout layout = ((TextView)view).getLayout();
		int line = layout.getLineForVertical((int)(y - view.getTop()));
		return new int[]{findContainingViewHolder(view).getAdapterPosition(), layout.getOffsetForHorizontal(line, (int)(x - view.getLeft()))};
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev)
	{
		return gestureDetector.onTouchEvent(ev) || super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		return gestureDetector.onTouchEvent(ev) || super.onTouchEvent(ev);
	}

	@Override
	public void onScrolled(int dx, int dy)
	{
		updateHandles();
	}

	private void onLongPressed(MotionEvent ev)
	{
		int[] pos = getPosForScreenCoords(ev.getX(), ev.getY());
		if(pos != null)
		{
			selecting = true;
			startHandle.line = endHandle.line = pos[0];
			startHandle.pos = endHandle.pos = pos[1];
			updateHandles();
			for(OnSelectionChangedListener listener : selectionChangedListeners)
				listener.onSelectionChanged();
		}
	}

	private void stopSelecting()
	{
		if(selecting)
		{
			selecting = false;
			updateHandles();
			for(OnSelectionChangedListener listener : selectionChangedListeners)
				listener.onSelectionChanged();
		}
	}

	private void updateHandles()
	{
		if(selecting)
		{
			if(endHandle.line < startHandle.line || (endHandle.line == startHandle.line && endHandle.pos < startHandle.pos))
			{
				int tmpLine = endHandle.line;
				int tmpPos = endHandle.pos;
				endHandle.line = startHandle.line;
				endHandle.pos = startHandle.pos;
				startHandle.line = tmpLine;
				startHandle.pos = tmpPos;
			}

			float startCoords[] = getScreenCoordsForPos(startHandle.line, startHandle.pos);
			startHandle.updateWindow(startCoords[0] - startHandle.drawable.getIntrinsicWidth(), startCoords[1], startCoords[2]);

			float endCoords[] = getScreenCoordsForPos(endHandle.line, endHandle.pos);
			endHandle.updateWindow(endCoords[0], endCoords[1], endCoords[2]);
		}
		else
		{
			startHandle.updateWindow(0, 0, 0);
			endHandle.updateWindow(0, 0, 0);
		}
	}

	public static abstract class OnSelectionChangedListener
	{
		public abstract void onSelectionChanged();
	}

	List<OnSelectionChangedListener> selectionChangedListeners = new ArrayList<>();

	public void addOnSelectionChangedListener(OnSelectionChangedListener listener)
	{
		selectionChangedListeners.add(listener);
	}

	public void removeOnSelectionChangedListener(OnSelectionChangedListener listener)
	{
		selectionChangedListeners.remove(listener);
	}

	public boolean isSelecting()
	{
		return selecting;
	}

	public int[] getSelectionStart()
	{
		return new int[]{startHandle.line, startHandle.pos};
	}

	public int[] getSelectionEnd()
	{
		return new int[]{endHandle.line, endHandle.pos};
	}
}