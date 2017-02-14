package com.mniip.bananapeel;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

class TextLineAdapter extends RecyclerView.Adapter<TextLineAdapter.ViewHolder>
{
    private int tabNumber;

    public TextLineAdapter(int position)
    {
        super();
        tabNumber = position;
    }

    @Override
    public TextLineAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        TextView view = (TextView)LayoutInflater.from(parent.getContext()).inflate(R.layout.text_line_fragment, parent, false);
        ViewHolder holder = new TextLineAdapter.ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        holder.view.setText("Hello" + position + "-" + tabNumber);
    }

    @Override
    public int getItemCount()
    {
        return 500000;
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
}
