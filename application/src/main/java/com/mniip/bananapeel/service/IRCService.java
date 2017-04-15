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
import com.mniip.bananapeel.util.IRCMessage;
import com.mniip.bananapeel.util.IntMap;
import com.mniip.bananapeel.util.TextEvent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class IRCService extends Service
{
	private IRCInterfaceListener listener;
	public IRCPreferences preferences;
	private Tab frontTab;

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
		tab.server = new IRCServer(this, tab);
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

	public IntMap<Tab> tabs = new IntMap<>();
	private int unusedTabId = 0;

	public ServerTab createServerTab()
	{
		ServerTab t = new ServerTab(this, unusedTabId++);
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
		if(tabs.get(tabId) != null)
		{
			if(listener != null)
				listener.onTabRemoved(tabId);
			tabs.delete(tabId);
		}
	}

	public Tab findTab(ServerTab sTab, String title)
	{
		for(Tab tab : tabs)
			if(tab.getServerTab() == sTab && tab.getTitle().equals(title))
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
					t.putLine(new TextEvent(TextEvent.MESSAGE, t.getServerTab().server.ourNick, str));
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
			ClientCommandHandler.handle(tab, words.get(0), words, wordEols);
	}

	public static class ClientCommandHandler
	{
		@Retention(RetentionPolicy.RUNTIME)
		private @interface Hook { }

		public static void handle(Tab tab, String command, List<String> words, List<String> wordEols)
		{
			String methodName = "command" + command.toUpperCase();
			try
			{
				Method m = ClientCommandHandler.class.getDeclaredMethod(methodName, Tab.class, List.class, List.class);
				if(m.getAnnotation(Hook.class) != null)
					m.invoke(null, tab, words, wordEols);
			}
			catch(NoSuchMethodException e)
			{
				unhandledCommand(tab, command, words, wordEols);
			}
			catch(IllegalAccessException e) { }
			catch(InvocationTargetException e)
			{
				if(e.getCause() instanceof RuntimeException)
					throw (RuntimeException)e.getCause();
			}
		}

		@ClientCommandHandler.Hook
		private static void commandSERVER(Tab tab, List<String> words, List<String> wordEols)
		{
			if(words.size() >= 1)
			{
				String server = words.get(1);
				IRCServerPreferences preferences = tab.getService().preferences.getServer(server);
				if(preferences != null)
					tab.getServerTab().server.connect(preferences.getHost(), preferences.getPort());
				else
					tab.getServerTab().server.connect(words.get(1), 6667);
				tab.getServerTab().setTitle(server);
			}
		}

		private static void unhandledCommand(Tab tab, String command, List<String> words, List<String> wordEols)
		{
			tab.getServerTab().server.send(IRCMessage.fromIRC(wordEols.get(0)));
		}
	}
}