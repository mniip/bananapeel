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
    private boolean sticky = true;
    private TextLineAdapter adapter;
    private RecyclerView recycler;


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
        {
            tabId = savedInstanceState.getInt("tabId");
            sticky = savedInstanceState.getBoolean("sticky");
        }
        ((MainScreen)getActivity()).getTabAdapter().onTabViewCreated(this, tabId);

        View view = inflater.inflate(R.layout.tab_fragment, parent, false);

        recycler = (RecyclerView)view.findViewById(R.id.recycler);
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        manager.setStackFromEnd(true);
        recycler.setLayoutManager(manager);
        adapter = new TextLineAdapter(tabId);
        recycler.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        recycler.setOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
            {
                LinearLayoutManager manager = (LinearLayoutManager)recyclerView.getLayoutManager();
                sticky = manager.findLastCompletelyVisibleItemPosition() == manager.getItemCount() - 1;
            }
        });

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
        if(sticky)
            recycler.scrollToPosition(adapter.getItemCount() - 1);
    }

    public void onCleared()
    {
        adapter.onCleared();
    }
}
