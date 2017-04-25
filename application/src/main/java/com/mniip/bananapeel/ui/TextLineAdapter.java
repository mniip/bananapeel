package com.mniip.bananapeel.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mniip.bananapeel.R;
import com.mniip.bananapeel.ServiceApplication;
import com.mniip.bananapeel.util.IRCFormatting;
import com.mniip.bananapeel.util.TextEvent;

import java.util.List;

public class TextLineAdapter extends RecyclerView.Adapter<TextLineAdapter.ViewHolder>
{
    private int tabId;
    private List<TextEvent> textLines;
    private int lastSize = 0;

    public TextLineAdapter(int tabId)
    {
        super();
        this.tabId = tabId;
        textLines = ServiceApplication.getService().tabs.get(tabId).getTextLines();
    }

    @Override
    public TextLineAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        TextView view = (TextView)LayoutInflater.from(parent.getContext()).inflate(R.layout.text_line_fragment, parent, false);
        ViewHolder holder = new TextLineAdapter.ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int lineNum)
    {
        holder.view.setText(textLines.get(lineNum).getText());
    }

    @Override
    public int getItemCount()
    {
        lastSize = textLines.size();
        return textLines.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        public TextView view;

        public ViewHolder(TextView view)
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
