package com.mniip.bananapeel.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
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
    private int tabId;
    private List<TextEvent> textLines;
    private int lastSize = 0;

    public TextLineAdapter(int tabId)
    {
        super();
        this.tabId = tabId;
        textLines = ServiceApplication.getService().getTabById(tabId).getTextLines();
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
