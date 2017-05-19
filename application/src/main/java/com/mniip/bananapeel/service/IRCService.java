package com.mniip.bananapeel.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.mniip.bananapeel.R;
import com.mniip.bananapeel.ServiceApplication;
import com.mniip.bananapeel.ui.MainScreen;
import com.mniip.bananapeel.ui.IRCInterfaceListener;
import com.mniip.bananapeel.util.Hook;
import com.mniip.bananapeel.util.IRCMessage;
import com.mniip.bananapeel.util.IntMap;
import com.mniip.bananapeel.util.TextEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IRCService extends Service
{
	private IRCInterfaceListener listener;
	public IRCPreferences preferences;
	private Tab frontTab;

	private IntMap<Integer> tabPositions = new IntMap<>(); // tabId -> tabPosition
	private ArrayList<Integer> tabIds = new ArrayList<>(); // position -> tabId
	private IntMap<Tab> tabs = new IntMap<>(); // tabId -> tab

	private int unusedTabId = 0;

	public Tab getTabById(int id)
	{
		return tabs.get(id);
	}

	public Tab getTabByPosition(int position)
	{
		try
		{
			return getTabById(tabIds.get(position));
		}
		catch(IndexOutOfBoundsException e)
		{
			return null;
		}
	}

	public int getPosById(int id)
	{
		return tabPositions.get(id);
	}

	public Iterable<Tab> getTabs()
	{
		return tabs;
	}

	public int getTabsCount()
	{
		return tabs.size();
	}

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

		ServerTab tab = createServerTab();
		tab.server = new IRCServer(this, tab, null);
		frontTab = tab;

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

	private void updatePositions(Tab tab)
	{
		int tabId = tab.getId();

		tabs.put(tabId, tab);

		int pos = 0;
		for(int p = 0; p < tabIds.size(); p++)
		{
			Tab t = tabs.get(tabIds.get(p));
			if(tab.getServerTab() == t)
				pos = p + 1;
			if(t != null && t.getTitle().compareToIgnoreCase(tab.getTitle()) < 0)
				pos = p + 1;
		}
		tabIds.add(pos, tabId);
		for(IntMap.KV<Integer> kv : tabPositions.pairs())
			if(kv.getValue() >= pos)
				kv.setValue(kv.getValue() + 1);
		tabPositions.put(tabId, pos);
	}

	public ServerTab createServerTab()
	{
		ServerTab tab = new ServerTab(this, unusedTabId++);

		updatePositions(tab);

		if(listener != null)
			listener.onTabAdded(tab.getId());
		return tab;
	}

	public Tab createTab(ServerTab parent, String title)
	{
		Tab tab = new Tab(this, parent, unusedTabId++, title);

		updatePositions(tab);

		if(listener != null)
			listener.onTabAdded(tab.getId());
		return tab;
	}

	public void deleteTab(int tabId)
	{
		if(tabs.get(tabId) != null)
		{
			Integer tabPos = tabPositions.get(tabId);

			if(tabPos != null)
			{
				tabIds.remove((int)tabPos);
				tabPositions.delete(tabId);
				for(IntMap.KV<Integer> kv : tabPositions.pairs())
					if(kv.getValue() > tabPos)
						kv.setValue(kv.getValue() - 1);
			}
			if(listener != null)
				listener.onTabRemoved(tabId);
			tabs.delete(tabId);
		}
	}

	public Tab findTab(ServerTab sTab, String title)
	{
		for(Tab tab : tabs)
			if(tab.getServerTab() == sTab && tab.getTitle().equalsIgnoreCase(title)) // TODO: casemapping here
				return tab;
		return null;
	}

	public Tab getFrontTab()
	{
		return frontTab;
	}

	public void setFrontTab(int tabId)
	{
		Tab tab = tabs.get(tabId);
		if(tab != null)
			frontTab = tab;
	}

	public void changeNickList(Tab tab)
	{
		if(listener != null)
			listener.onTabNickListChanged(tab.getId());
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
					t.putLine(new TextEvent(TextEvent.Type.OUR_MESSAGE, t.getServerTab().server.ourNick, str));
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
			commandHandler.invoke(tab, new CommandData(words, wordEols));
	}

	private class CommandData
	{
		private CommandData(List<String> words, List<String> wordEols)
		{
			this.words = words;
			this.wordEols = wordEols;
		}

		private final List<String> words;
		private final List<String> wordEols;
	}

	abstract private static class Command implements Hook<Tab, CommandData> {}

	private static Hook.Binned<String, Tab, CommandData> commandBins = new Hook.Binned<String, Tab, CommandData>()
	{
		public String resolve(CommandData data)
		{
			if(data.words.size() > 0)
				return data.words.get(0).toUpperCase();
			return "";
		}
	};

	private static Command rawHook = new Command()
	{
		public boolean invoke(Tab tab, CommandData data)
		{
			tab.getServerTab().server.send(IRCMessage.fromIRC(data.wordEols.get(0)));
			return true;
		}
	};

	private static Hook<Tab, CommandData> commandHandler = new Hook.Sequence<>(Arrays.asList(commandBins, rawHook));

	static
	{
		commandBins.add("SERVER", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() >= 1)
				{
					String server = data.words.get(1);
					IRCServerPreferences preferences = tab.getService().preferences.getServer(server);
					if(preferences == null)
						preferences = new IRCServerPreferences.Dummy(data.words.get(1), tab.getService().preferences, data.words.get(1), 6667);
					IRCServer srv = tab.getServerTab().server;
					srv.setPreferences(preferences);
					srv.connect();
				}
				return true;
			}
		});

		commandBins.add("ME", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				tab.getServerTab().server.send(new IRCMessage("PRIVMSG", tab.getTitle(), "\001ACTION " + data.wordEols.get(1) + "\001"));
				tab.putLine(new TextEvent(TextEvent.Type.OUR_CTCP_ACTION, tab.getServerTab().server.ourNick, data.wordEols.get(1)));
				return true;
			}
		});
	}
}