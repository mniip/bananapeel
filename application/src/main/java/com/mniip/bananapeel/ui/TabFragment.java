package com.mniip.bananapeel.ui;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mniip.bananapeel.R;
import com.mniip.bananapeel.service.Tab;
import com.mniip.bananapeel.util.NickListEntry;
import com.mniip.bananapeel.util.TextEvent;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

public class TabFragment extends Fragment
{
    private int tabId = -1;
    private boolean sticky = true;
    private boolean active = true;
    private TextLineAdapter adapter;
    private SelectableScrollbackView scrollback;
    private MainScreen mainScreen;

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

        mainScreen = (MainScreen)getActivity();
        scrollback = (SelectableScrollbackView)view.findViewById(R.id.recycler);
        LinearLayoutManager manager = new LinearLayoutManager(scrollback.getContext());
        manager.setStackFromEnd(true);
        scrollback.setLayoutManager(manager);
        adapter = new TextLineAdapter(scrollback, tabId);
        scrollback.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        scrollback.setOnScrollListener(new RecyclerView.OnScrollListener()
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

        ImageButton sendButton = (ImageButton)view.findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendInput(inputText);
            }
        });

        ImageButton searchButton = (ImageButton)view.findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Tab tab = mainScreen.getService().getTabById(tabId);

                if(tab.nickList == null) //todo: channel completion etc
                    return;

                String text = inputText.getText().toString();
                int cur = inputText.getSelectionEnd();
                int begin = cur;

                while (begin > 0 && !Character.isSpaceChar(text.charAt(begin - 1)) )
                    --begin;

                String word = text.substring(begin, cur);

                ArrayList<String> list = new ArrayList<String>();

                if (tab.serverTab.server.config.isChannel(word));// todo: channellist
                else if (!word.isEmpty() && begin == 0 && word.charAt(begin) == '/');// todo: commandlist
                else
                {
                    for(NickListEntry nick : tab.nickList)
                        list.add(nick.nick);
                }

                Collator collator = tab.serverTab.server.config.nickCollator;
                ArrayList<String> result = new ArrayList<String>();

                String minStr = null;
                int wordLen = word.length();

                for(String nick : list)
                {
                    int nickLen = nick.length();

                    String nickStr = (nickLen > wordLen)? nick.substring(0, word.length()): nick    ;
                    if(collator.equals(nickStr, word))
                    {
                         result.add(nick);

                        if(minStr == null || nickLen < minStr.length())
                            minStr = nick;
                    }
                }

                if(result.size() == 1)
                    inputText.getText().replace(begin, cur, result.get(0) + (begin == 0 ? ", " : " "));

                else if(result.size() > 1)
                {
                    String toSend = "";

                    for(String nick : result)
                        toSend += (toSend.isEmpty())? nick : " " + nick;

                    if (wordLen == minStr.length())
                        inputText.getText().replace(begin, cur, minStr);
                    else

                    OUTER : for(int i = minStr.length(); i > wordLen; --i)
                    {
                        for (String nick : result)
                        {
                            if (! collator.equals(nick.substring(wordLen, i), minStr.substring(wordLen, i)))
                                continue OUTER;
                        }
                        inputText.getText().replace(begin, cur, minStr.substring(0, i));
                        break;
                    }
                    tab.putLine(new TextEvent(TextEvent.Type.ERROR, toSend));
                }
            }
        });

        if(tabId == -1)
            mainScreen.getTabAdapter().notifyDataSetChanged();

        return view;
    }

    private void sendInput(TextView v)
    {
        mainScreen.getService().onTextEntered(tabId, v.getText().toString());
        v.setText("");
    }

    public void onLinesAdded()
    {
        adapter.onLinesAdded();
        if(sticky)
            scrollback.scrollToPosition(adapter.getItemCount() - 1);
    }

    public void onCleared()
    {
        adapter.onCleared();
    }
}
