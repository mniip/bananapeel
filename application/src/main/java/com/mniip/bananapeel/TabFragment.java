package com.mniip.bananapeel;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.tab_fragment, container, false);
        if(savedInstanceState != null)
            position = savedInstanceState.getInt("position");
        ((TextView)view.findViewById(R.id.text_area)).setText(position + "");
        return view;
    }
}
