package com.mniip.bananapeel;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.support.v7.widget.RecyclerView;

public class TabFragment extends Fragment
{
    private int tabId = -1;
    TextLineAdapter adapter;

    public void setId(int id)
    {
        tabId = id;
    }

    @Override
    public void onSaveInstanceState(Bundle out)
    {
        super.onSaveInstanceState(out);
        out.putInt("tabId", tabId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState)
    {
        if(savedInstanceState != null)
            tabId = savedInstanceState.getInt("tabId");
        ((MainScreen)getActivity()).getTabAdapter().onTabViewCreated(this, tabId);

        View view = inflater.inflate(R.layout.tab_fragment, parent, false);

        RecyclerView recycler = (RecyclerView) view.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TextLineAdapter(tabId);
        recycler.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        ImageButton edit = (ImageButton) view.findViewById(R.id.send_button);
        edit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                EditText edit = (EditText)((ViewGroup) v.getParent()).findViewById(R.id.input_box);
                ServiceApplication.getService().onTextEntered(tabId, edit.getText().toString());
                edit.setText("");
            }
        });

        if(tabId == -1)
            ((MainScreen)getActivity()).getTabAdapter().notifyDataSetChanged();

        return view;
    }

    @Override
    public void onDestroyView()
    {
        ((MainScreen)getActivity()).getTabAdapter().onTabViewDestroyed(tabId);

        super.onDestroyView();
    }

    public void onLinesAdded()
    {
        adapter.onLinesAdded();
    }

    public void onCleared()
    {
        adapter.onCleared();
    }
}
