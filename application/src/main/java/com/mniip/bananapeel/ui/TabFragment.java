package com.mniip.bananapeel.ui;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.mniip.bananapeel.R;
import com.mniip.bananapeel.ServiceApplication;

public class TabFragment extends Fragment
{
    private int tabId = -1;
    private boolean sticky = true;
    private boolean active = true;
    private TextLineAdapter adapter;
    private RecyclerView recycler;

    private EditText inputText;

    public void setTabId(int tabId)
    {
        this.tabId = tabId;
    }

    public EditText getInputText()
    {
        return inputText;
    }
    public int getTabId()
    {
        return tabId;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setInactive()
    {
        active = false;
    }

    @Override
    public void onSaveInstanceState(Bundle out)
    {
        Log.d("BananaPeel","fragment.onSaveInstanceState");
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

        View view = inflater.inflate(R.layout.tab_fragment, parent, false);

        recycler = (RecyclerView)view.findViewById(R.id.recycler);
        LinearLayoutManager manager = new LinearLayoutManager(recycler.getContext());
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

        inputText = (EditText)view.findViewById(R.id.input_box);
        inputText.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if(actionId == EditorInfo.IME_ACTION_SEND ||
                        event != null &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    sendInput(v);
                    return true;
                }
                    return false;
            }
        });

        ImageButton button = (ImageButton)view.findViewById(R.id.send_button);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                EditText edit = (EditText)((ViewGroup)v.getParent()).findViewById(R.id.input_box);
                sendInput(edit);
            }
        });

        if(tabId == -1)
            ((MainScreen)getActivity()).getTabAdapter().notifyDataSetChanged();

        return view;
    }

    private void sendInput(TextView v)
    {
        ServiceApplication.getService().onTextEntered(tabId, v.getText().toString());
        v.setText("");
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
