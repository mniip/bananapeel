package com.mniip.bananapeel;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;

public class IRCService extends Service
{
	private IRCInterfaceListener listener;
	public IRCPreferences preferences;

	public void setListener(IRCInterfaceListener l)
	{
		listener = l;
	}

	public void unsetListener(IRCInterfaceListener l)
	{
		if(listener == l)
			listener = null;
	}

	public IRCInterfaceListener getListener()
	{
		return listener;
	}

	@Override
	public void onCreate()
	{
		preferences = new IRCPreferences(this);

		createServerTab();

		Log.d("BananaPeel", "Service created");
		super.onCreate();

		Notification n = new NotificationCompat.Builder(this)
			.setSmallIcon(R.drawable.app_icon)
			.setContentTitle(getString(R.string.app_name))
			.setContentIntent(PendingIntent.getActivity(this, 0, (new Intent(this, MainScreen.class)).setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT), 0))
			.build();
		startForeground(1, n);

		((ServiceApplication)getApplicationContext()).onServiceStarted(this);
	}

	@Override
	public void onDestroy()
	{
		Log.d("BananaPeel", "Service destroyed");
		super.onDestroy();
		((ServiceApplication)getApplicationContext()).onServiceStopped();
	}

	@Override
	public IBinder onBind(Intent i) { return new Binder(); }

	SparseArray<Tab> tabs = new SparseArray<>();
	private int unusedTabId = 0;

	public Tab createServerTab()
	{
		Tab t = new ServerTab(this, unusedTabId++);
		tabs.put(t.getId(), t);
		if(listener != null)
			listener.onTabAdded(t.getId());
		return t;
	}

	public Tab createTab(ServerTab parent, String title)
	{
		Tab t = new Tab(this, parent, unusedTabId++, title);
		tabs.put(t.getId(), t);
		if(listener != null)
			listener.onTabAdded(t.getId());
		return t;
	}

	public void deleteTab(int tabId)
	{
		if(listener != null)
			listener.onTabRemoved(tabId);
		tabs.delete(tabId);
	}

	public Tab findTab(ServerTab sTab, String title)
	{
		for(int i = 0; i < tabs.size(); i++)
		{
			Tab tab = tabs.valueAt(i);
			if(tab.getServerTab() == sTab && tab.getTitle().equals(title))
				return tab;
		}
		return null;
	}

	public void onTextEntered(int tabId, String str)
	{
		Tab t = tabs.get(tabId);
		if (t != null)
		{
			if(str.length() > 0 && str.charAt(0) == '/')
				onCommandEntered(t, str.substring(1));
			else
				if(t.getServerTab().server != null)
				{
					t.getServerTab().server.send(new IRCMessage("PRIVMSG", t.getTitle(), str));
					t.putLine("<" + t.getServerTab().server.ourNick + "> " + str);
				}
		}
	}

	public void onCommandEntered(Tab tab, String str)
	{
		ArrayList<String> words = new ArrayList<>();
		ArrayList<String> wordEols = new ArrayList<>();
		int idx;
		do
		{
			wordEols.add(str);
			idx = str.indexOf(' ');
			if(idx != -1)
			{
				words.add(str.substring(0, idx));
				str = str.substring(idx + 1);
			}
			else
			{
				words.add(str);
			}
		}
		while(idx != -1);

		if(words.size() > 0)
			ClientCommandHandler.handle(tab, words.get(0).toUpperCase(), words, wordEols);
	}

	public void onServerConnected(Server srv)
	{
		srv.ourNick = preferences.getDefaultNick();
		srv.send(new IRCMessage("NICK", srv.ourNick));
		srv.send(new IRCMessage("USER", preferences.getDefaultUser(), "*", "*", preferences.getDefaultRealName()));
	}

	public void onIRCMessageReceived(Server srv, IRCMessage msg)
	{
		IRCCommandHandler.handle(srv, msg);
	}
}