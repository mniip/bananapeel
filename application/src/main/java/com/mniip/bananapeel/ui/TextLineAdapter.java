package com.mniip.bananapeel.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mniip.bananapeel.R;
import com.mniip.bananapeel.ServiceApplication;
import com.mniip.bananapeel.util.LinkFinder;
import com.mniip.bananapeel.util.TextEvent;

import java.util.List;

public class TextLineAdapter extends RecyclerView.Adapter<TextLineAdapter.ViewHolder>
{
	private final SelectableScrollbackView scrollbackView;
	private List<TextEvent> textLines;
	private int lastSize = 0;

	private boolean lastSelecting = false;
	private int lastStartLine;
	private int lastEndLine;

	public void notifyFromToChanged(int start, int end)
	{
		notifyItemRangeChanged(start, end - start + 1);
	}

	public TextLineAdapter(SelectableScrollbackView scrollbackView, int tabId)
	{
		super();
		this.scrollbackView = scrollbackView;
		textLines = ServiceApplication.getService().getTabById(tabId).getTextLines();
		scrollbackView.addOnSelectionChangedListener(new SelectableScrollbackView.OnSelectionChangedListener()
		{
			@Override
			public void onSelectionChanged()
			{
				SelectableScrollbackView scrollbackView = TextLineAdapter.this.scrollbackView;

				boolean selecting = scrollbackView.isSelecting();
				int start = scrollbackView.getSelectionStart()[0];
				int end = scrollbackView.getSelectionEnd()[0];
				if(lastSelecting)
					if(selecting)
					{
						if(lastStartLine < start)
							notifyFromToChanged(lastStartLine, start);
						else
							notifyFromToChanged(start, lastStartLine);
						if(lastEndLine < end)
							notifyFromToChanged(lastEndLine, end);
						else
							notifyFromToChanged(end, lastEndLine);
					}
					else
						notifyItemRangeChanged(lastStartLine, lastEndLine);
				else
					notifyFromToChanged(start, end);

				lastSelecting = selecting;
				lastStartLine = start;
				lastEndLine = end;
			}
		});
	}

	@Override
	public TextLineAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		TextView view = (TextView)LayoutInflater.from(parent.getContext()).inflate(R.layout.text_line_fragment, parent, false);
		view.setMovementMethod(new LinkMovementMethod());
		ViewHolder holder = new TextLineAdapter.ViewHolder(view);
		return holder;
	}

	private static class LinkSpan extends ClickableSpan
	{
		private final String link;

		private LinkSpan(String link)
		{
			this.link = link;
		}

		@Override
		public void onClick(View view)
		{
			try
			{
				view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
			}
			catch(ActivityNotFoundException e) {}
		}

		private static final UnderlineSpan underline = new UnderlineSpan();

		@Override
		public void updateDrawState(TextPaint ds)
		{
			underline.updateDrawState(ds);
		}
	}

	private static CharacterStyle selectionSpan = new CharacterStyle()
	{
		@Override
		public void updateDrawState(TextPaint ds)
		{
			ds.bgColor = 0x3F007FFF;
		}
	};

	@Override
	public void onBindViewHolder(ViewHolder holder, int lineNum)
	{
		Spannable spannable = textLines.get(lineNum).getText();
		for(LinkFinder.Link link : spannable.getSpans(0, spannable.length(), LinkFinder.Link.class))
		{
			int begin = spannable.getSpanStart(link);
			int end = spannable.getSpanEnd(link);
			String url = spannable.subSequence(begin, end).toString();
			if(!link.hasSchema)
				url = "http://" + url;
			spannable.setSpan(new LinkSpan(url), begin, end, spannable.getSpanFlags(link));
			spannable.removeSpan(link);
		}

		if(scrollbackView.isSelecting())
		{
			int[] start = scrollbackView.getSelectionStart();
			int[] end = scrollbackView.getSelectionEnd();
			if(start[0] <= lineNum && end[0] >= lineNum)
				spannable.setSpan(selectionSpan, start[0] == lineNum ? start[1] : 0, end[0] == lineNum ? end[1] : spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			else
				spannable.removeSpan(selectionSpan);
		}
		else
			spannable.removeSpan(selectionSpan);

		holder.view.setText(spannable);
	}

	@Override
	public int getItemCount()
	{
		lastSize = textLines.size();
		return textLines.size();
	}

	static class ViewHolder extends RecyclerView.ViewHolder
	{
		private TextView view;

		private ViewHolder(TextView view)
		{
			super(view);
			this.view = view;
		}
	}

	public void onLinesAdded()
	{
		int newSize = textLines.size();
		notifyItemRangeInserted(lastSize, newSize - lastSize);
		lastSize = newSize;
	}

	public void onCleared()
	{
		notifyItemRangeRemoved(0, lastSize);
		lastSize = 0;
	}
}
