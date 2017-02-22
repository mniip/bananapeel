package com.mniip.bananapeel;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.SparseArray;

import java.io.FileDescriptor;
import java.util.ArrayList;

public class IRCService extends Service
{
	private IRCInterfaceListener listener;

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
		Tab tab = createTab();
		tab.setTitle("title");

		Log.d("BananaPeel", "Service created");
		super.onCreate();

		Notification n = new NotificationCompat.Builder(this)
			.setSmallIcon(R.drawable.app_icon)
			.setContentTitle("Notification")
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

	public Tab createTab()
	{
		Tab t = new Tab(this, unusedTabId++);
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

	public void onTextEntered(int tabId, String str)
	{
		Tab t = tabs.get(tabId);
		if (t != null)
		{
			if(str.length() > 0 && str.charAt(0) == '/')
			{
				onCommand(t, str.substring(1));
			}
			else
			{
				t.putLine(str);
			}
		}
	}

	public void onCommand(Tab tab, String str)
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
				words.add(str.substring(0));
			}
		}
		while(idx != -1);

		if(words.size() > 0)
		{
			if(words.get(0).toLowerCase().equals("server"))
			{
				tab.server = new Server(this);
				tab.server.connect(words.get(1), 6667);
			}
			if(words.get(0).toLowerCase().equals("quote"))
			{
				if(tab.server != null)
					tab.server.send(IRCMessage.fromIRC(wordEols.get(1)));
			}
		}
	}
}