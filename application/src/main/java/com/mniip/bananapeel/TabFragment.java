package com.mniip.bananapeel;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.support.v7.widget.RecyclerView;

public class TabFragment extends Fragment
{
    private int position;

    public void setPosition(int pos)
    {
        position = pos;
    }

    @Override
    public void onSaveInstanceState(Bundle out)
    {
        super.onSaveInstanceState(out);
        out.putInt("position", position);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.tab_fragment, parent, false);
        if(savedInstanceState != null)
            position = savedInstanceState.getInt("position");

        RecyclerView recycler = (RecyclerView)view.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(new TextLineAdapter(position));
        if(savedInstanceState == null)
            recycler.scrollToPosition(recycler.getAdapter().getItemCount() - 1);
        return view;
    }

}
