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

	public IntMap<Tab> tabs = new IntMap<>();
	private int unusedTabId = 0;

	public ServerTab createServerTab()
	{
		ServerTab t = new ServerTab(this, unusedTabId++);
		tabs.put(t.id, t);
		if(listener != null)
			listener.onTabAdded(t.id);
		return t;
	}

	public Tab createTab(ServerTab parent, Tab.Type type, String title)
	{
		Tab t = new Tab(this, parent, unusedTabId++, type, title);
		tabs.put(t.id, t);
		if(listener != null)
			listener.onTabAdded(t.id);
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
			if(tab.serverTab == sTab && tab.getTitle().equalsIgnoreCase(title)) // TODO: casemapping here
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
			listener.onTabNickListChanged(tab.id);
	}


	public void onTextEntered(int tabId, String str)
	{
		Tab t = tabs.get(tabId);
		if (t != null)
		{
			if(str.length() > 0 && str.charAt(0) == '/')
				onCommandEntered(t, str.substring(1));
			else
				if(t.serverTab.server != null)
				{
					t.serverTab.server.send(new IRCMessage("PRIVMSG", t.getTitle(), str));
					t.putLine(new TextEvent(TextEvent.Type.OUR_MESSAGE, t.serverTab.server.ourNick, str));
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
			tab.serverTab.server.send(IRCMessage.fromIRC(data.wordEols.get(0)));
			return true;
		}
	};

	private static Hook<Tab, CommandData> commandHandler = new Hook.Sequence<>(Arrays.asList(commandBins, rawHook));

	static
	{
		commandBins.add("AWAY", new Command()
		{
			@Override
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() > 1)
					tab.serverTab.server.send(new IRCMessage("AWAY", data.wordEols.get(1)));
				else
					tab.serverTab.server.send(new IRCMessage("AWAY"));
				return true;
			}
		});

		commandBins.add("BACK", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				tab.serverTab.server.send(new IRCMessage("AWAY"));
				return true;
			}
		});

		commandBins.add("BAN", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(tab.type == Tab.Type.CHANNEL)
					for(int i = 1; i < data.words.size(); i++)
						tab.serverTab.server.send(new IRCMessage("MODE", tab.getTitle(), "+b", data.words.get(i)));
				else
					tab.putLine(new TextEvent(TextEvent.Type.ERROR, "/BAN needs to be executed in a channel"));
				return true;
			}
		});

		commandBins.add("CLOSE", new Command()
		{
			@Override
			public boolean invoke(Tab tab, CommandData data)
			{
				if(tab.type == Tab.Type.QUERY)
					tab.service.deleteTab(tab.id);
				else if(tab.type == Tab.Type.CHANNEL)
				{
					tab.serverTab.server.send(new IRCMessage("PART", tab.getTitle()));
					tab.service.deleteTab(tab.id);
				}
				else if(tab.type == Tab.Type.SERVER)
				{
					// TODO
				}
				return true;
			}
		});

		commandBins.add("CTCP", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() >= 2)
				{
					tab.serverTab.server.send(new IRCMessage("PRIVMSG", data.words.get(1), "\001ACTION " + data.wordEols.get(2) + "\001"));
					tab.putLine(new TextEvent(TextEvent.Type.OUR_CTCP, data.words.get(1), data.wordEols.get(2)));
				}
				return true;
			}
		});

		commandBins.add("DEHOP", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(tab.type == Tab.Type.CHANNEL)
					for(int i = 1; i < data.words.size(); i++)
						tab.serverTab.server.send(new IRCMessage("MODE", tab.getTitle(), "-h", data.words.get(i)));
				else
					tab.putLine(new TextEvent(TextEvent.Type.ERROR, "/DEHOP needs to be executed in a channel"));
				return true;
			}
		});

		commandBins.add("DEOP", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(tab.type == Tab.Type.CHANNEL)
					for(int i = 1; i < data.words.size(); i++)
						tab.serverTab.server.send(new IRCMessage("MODE", tab.getTitle(), "-o", data.words.get(i)));
				else
					tab.putLine(new TextEvent(TextEvent.Type.ERROR, "/DEOP needs to be executed in a channel"));
				return true;
			}
		});

		commandBins.add("DEVOICE", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(tab.type == Tab.Type.CHANNEL)
					for(int i = 1; i < data.words.size(); i++)
						tab.serverTab.server.send(new IRCMessage("MODE", tab.getTitle(), "-v", data.words.get(i)));
				else
					tab.putLine(new TextEvent(TextEvent.Type.ERROR, "/DEVOICE needs to be executed in a channel"));
				return true;
			}
		});

		commandBins.add("DISCON", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				tab.serverTab.server.disconnect("");
				return true;
			}
		});

		commandBins.add("HOP", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(tab.type == Tab.Type.CHANNEL)
					for(int i = 1; i < data.words.size(); i++)
						tab.serverTab.server.send(new IRCMessage("MODE", tab.getTitle(), "+h", data.words.get(i)));
				else
					tab.putLine(new TextEvent(TextEvent.Type.ERROR, "/HOP needs to be executed in a channel"));
				return true;
			}
		});

		commandBins.add("INVITE", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() > 1)
				{
					if(data.words.size() > 2)
						tab.serverTab.server.send(new IRCMessage("INVITE", data.words.get(1), data.words.get(2)));
					else
						if(tab.type == Tab.Type.CHANNEL)
							tab.serverTab.server.send(new IRCMessage("INVITE", data.words.get(1), tab.getTitle()));
						else
							tab.putLine(new TextEvent(TextEvent.Type.ERROR, "No channel specified"));
				}
				return true;
			}
		});

		commandBins.add("JOIN", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				tab.serverTab.server.send(IRCMessage.fromIRC(data.wordEols.get(0)));
				return true;
			}
		});

		commandBins.add("KICK", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() > 1)
					if(data.words.size() > 2 && tab.serverTab.server.config.isChannel(data.words.get(1)))
						if(data.words.size() > 3)
							tab.serverTab.server.send(new IRCMessage("KICK", data.words.get(1), data.words.get(2), data.wordEols.get(3)));
						else
							tab.serverTab.server.send(new IRCMessage("KICK", data.words.get(1), data.words.get(2)));
					else if(tab.type == Tab.Type.CHANNEL)
						if(data.words.size() > 2)
							tab.serverTab.server.send(new IRCMessage("KICK", tab.getTitle(), data.words.get(1), data.wordEols.get(2)));
						else
							tab.serverTab.server.send(new IRCMessage("KICK", tab.getTitle(), data.words.get(1)));
					else
						tab.putLine(new TextEvent(TextEvent.Type.ERROR, "No channel specified"));
				return true;
			}
		});

		commandBins.add("ME", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() > 1)
				{
					tab.serverTab.server.send(new IRCMessage("PRIVMSG", tab.getTitle(), "\001ACTION " + data.wordEols.get(1) + "\001"));
					tab.putLine(new TextEvent(TextEvent.Type.OUR_CTCP_ACTION, tab.serverTab.server.ourNick, data.wordEols.get(1)));
				}
				return true;
			}
		});

		commandBins.add("MODE", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() > 1 && tab.serverTab.server.config.isChannel(data.words.get(1)))
					if(data.words.size() > 2)
						tab.serverTab.server.send(new IRCMessage("MODE", data.words.get(1), data.wordEols.get(2)));
					else
						tab.serverTab.server.send(new IRCMessage("MODE", data.words.get(1)));
				else if(tab.type == Tab.Type.CHANNEL)
					if(data.words.size() > 1)
						tab.serverTab.server.send(new IRCMessage("MODE", tab.getTitle(), data.wordEols.get(1)));
					else
						tab.serverTab.server.send(new IRCMessage("MODE", tab.getTitle()));
				else
					tab.putLine(new TextEvent(TextEvent.Type.ERROR, "No channel specified"));
				return true;
			}
		});

		commandBins.add("MSG", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() > 2)
				{
					tab.serverTab.server.send(new IRCMessage("PRIVMSG", data.words.get(1), data.wordEols.get(2)));
					tab.putLine(new TextEvent(TextEvent.Type.OUR_MSG, data.words.get(1), data.wordEols.get(2)));
				}
				return true;
			}
		});

		commandBins.add("NICK", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() > 1)
					tab.serverTab.server.send(new IRCMessage("NICK", data.words.get(1)));
				return true;
			}
		});

		commandBins.add("NOTICE", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() > 2)
				{
					tab.serverTab.server.send(new IRCMessage("NOTICE", data.words.get(1), data.wordEols.get(2)));
					tab.putLine(new TextEvent(TextEvent.Type.OUR_NOTICE, data.words.get(1), data.wordEols.get(2)));
				}
				return true;
			}
		});

		commandBins.add("OP", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(tab.type == Tab.Type.CHANNEL)
					for(int i = 1; i < data.words.size(); i++)
						tab.serverTab.server.send(new IRCMessage("MODE", tab.getTitle(), "+o", data.words.get(i)));
				else
					tab.putLine(new TextEvent(TextEvent.Type.ERROR, "/OP needs to be executed in a channel"));
				return true;
			}
		});

		commandBins.add("PART", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() > 1 && tab.serverTab.server.config.isChannel(data.words.get(1)))
					if(data.words.size() > 2)
						tab.serverTab.server.send(new IRCMessage("PART", data.words.get(1), data.wordEols.get(2)));
					else
						tab.serverTab.server.send(new IRCMessage("PART", data.words.get(1)));
				else if(tab.type == Tab.Type.CHANNEL)
					if(data.words.size() > 1)
						tab.serverTab.server.send(new IRCMessage("PART", tab.getTitle(), data.wordEols.get(1)));
					else
						tab.serverTab.server.send(new IRCMessage("PART", tab.getTitle()));
				else
					tab.putLine(new TextEvent(TextEvent.Type.ERROR, "No channel specified"));
				return true;
			}
		});

		commandBins.add("QUERY", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() > 1)
				{
					Tab t = tab.service.findTab(tab.serverTab, data.words.get(1));
					if(t == null)
						t = tab.service.createTab(tab.serverTab, Tab.Type.QUERY, data.words.get(1));
					if(data.words.size() > 2)
					{
						t.serverTab.server.send(new IRCMessage("PRIVMSG", data.words.get(1), data.wordEols.get(2)));
						t.putLine(new TextEvent(TextEvent.Type.OUR_MESSAGE, t.serverTab.server.ourNick, data.wordEols.get(2)));
					}
				}
				return true;
			}
		});

		commandBins.add("QUIT", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() > 1)
					tab.serverTab.server.send(new IRCMessage("QUIT", data.wordEols.get(1)));
				else
					tab.serverTab.server.send(new IRCMessage("QUIT"));
				return true;
			}
		});

		commandBins.add("QUOTE", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() > 1)
					tab.serverTab.server.send(IRCMessage.fromIRC(data.wordEols.get(1)));
				return true;
			}
		});

		commandBins.add("SERVER", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() >= 1)
				{
					String server = data.words.get(1);
					IRCServerPreferences preferences = tab.service.preferences.getServer(server);
					if(preferences == null)
						preferences = new IRCServerPreferences.Dummy(data.words.get(1), tab.service.preferences, data.words.get(1), 6667);
					IRCServer srv = tab.serverTab.server;
					srv.setPreferences(preferences);
					srv.connect();
				}
				return true;
			}
		});

		commandBins.add("TOPIC", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(data.words.size() > 1 && tab.serverTab.server.config.isChannel(data.words.get(1)))
					tab.serverTab.server.send(new IRCMessage("TOPIC", data.words.get(1), data.wordEols.get(2)));
				else if(tab.type == Tab.Type.CHANNEL)
					tab.serverTab.server.send(new IRCMessage("TOPIC", tab.getTitle(), data.wordEols.get(1)));
				else
					tab.putLine(new TextEvent(TextEvent.Type.ERROR, "No channel specified"));
				return true;
			}
		});

		commandBins.add("UNBAN", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(tab.type == Tab.Type.CHANNEL)
					for(int i = 1; i < data.words.size(); i++)
						tab.serverTab.server.send(new IRCMessage("MODE", tab.getTitle(), "-b", data.words.get(i)));
				else
					tab.putLine(new TextEvent(TextEvent.Type.ERROR, "/UNBAN needs to be executed in a channel"));
				return true;
			}
		});

		commandBins.add("VOICE", new Command()
		{
			public boolean invoke(Tab tab, CommandData data)
			{
				if(tab.type == Tab.Type.CHANNEL)
					for(int i = 1; i < data.words.size(); i++)
						tab.serverTab.server.send(new IRCMessage("MODE", tab.getTitle(), "+v", data.words.get(i)));
				else
					tab.putLine(new TextEvent(TextEvent.Type.ERROR, "/VOICE needs to be executed in a channel"));
				return true;
			}
		});
	}
}