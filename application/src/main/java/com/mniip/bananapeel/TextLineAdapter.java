package com.mniip.bananapeel;

import android.app.Service;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

class TextLineAdapter extends RecyclerView.Adapter<TextLineAdapter.ViewHolder>
{
    private int tabId;
    private ArrayList<String> textLines;
    private int lastSize = 0;

    public TextLineAdapter(int id)
    {
        super();
        tabId = id;
        textLines = ServiceApplication.getService().tabs.get(id).getTextLines();
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
        holder.view.setText(textLines.get(lineNum));
    }

    @Override
    public int getItemCount()
    {
        return textLines.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        public TextView view;

        public ViewHolder(TextView v)
        {
            super(v);
            view = v;
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
