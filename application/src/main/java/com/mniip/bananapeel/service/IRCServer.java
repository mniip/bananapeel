package com.mniip.bananapeel.service;

import com.mniip.bananapeel.util.IRCMessage;
import com.mniip.bananapeel.util.IRCServerConfig;
import com.mniip.bananapeel.util.NickListEntry;
import com.mniip.bananapeel.util.TextEvent;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class IRCServer
{
	private IRCService service;
	private ServerTab serverTab;
	private IRCConnection connection;
	private IRCServerPreferences preferences;

	private boolean registered = false;
	public String ourNick = "";
	private List<String> waitingNames = new ArrayList<>();

	public IRCServerConfig config = IRCServerConfig.rfc1459();

	private static class ComposedComparator<T> implements Comparator<T>
	{
		Comparator<T> x, y;

		public ComposedComparator(Comparator<T> x, Comparator<T> y)
		{
			this.x = x;
			this.y = y;
		}

		@Override
		public int compare(T a, T b)
		{
			int result = x.compare(a, b);
			if(result == 0)
				return y.compare(a, b);
			return result;
		}
	}

	public IRCServer(IRCService service, ServerTab serverTab, IRCServerPreferences preferences)
	{
		this.service = service;
		this.serverTab = serverTab;
		this.preferences = preferences;
	}

	public IRCService getService()
	{
		return service;
	}

	public ServerTab getTab()
	{
		return serverTab;
	}

	public void setPreferences(IRCServerPreferences preferences)
	{
		this.preferences = preferences;
		serverTab.setTitle(preferences.getName());
	}

	public void connect()
	{
		if(connection != null)
			connection.disconnect();
		connection = new IRCConnection(this);
		connection.connect(preferences.getHost(), preferences.getPort());
	}

	public void send(IRCMessage msg)
	{
		if(connection != null)
			connection.send(msg);
	}

	public void onIRCMessageReceived(IRCMessage msg)
	{
		IRCCommandHandler.handle(this, msg);
	}

	public void onConnected()
	{
		ourNick = preferences.getNick();
		send(new IRCMessage("NICK", ourNick));
		send(new IRCMessage("USER", preferences.getUser(), "*", "*", preferences.getRealName()));
	}

	public void onError(Exception e)
	{
		getTab().putLine(new TextEvent(TextEvent.ERROR, e.toString()));
		connection = null;
		registered = false;
	}

	public void onRegistered()
	{
		if(!registered)
		{
			registered = true;
		}
	}

	private Comparator<NickListEntry> nickListEntryComparator = new ComposedComparator<>(NickListEntry.statusComparator(config.statusChars), NickListEntry.nickComparator(config.nickCollator));

	private void updateComparator()
	{
		nickListEntryComparator = new ComposedComparator<>(NickListEntry.statusComparator(config.statusChars), NickListEntry.nickComparator(config.nickCollator));
		for(Tab tab : service.tabs)
			if(tab.getServerTab().server == this)
			{
				tab.nickList.setComparator(nickListEntryComparator);
				service.changeNickList(tab);
			}
	}

	public static class IRCCommandHandler
	{
		@Retention(RetentionPolicy.RUNTIME)
		private @interface Hook
		{
			String command();
			int minParams() default 0;
			boolean requireSource() default false;
		}

		@Retention(RetentionPolicy.RUNTIME)
		private @interface Hooks
		{
			Hook[] value();
		}

		private static boolean matchesHook(Hook hook, IRCMessage msg)
		{
			// Collation in command names?
			if(!hook.command().equalsIgnoreCase(msg.command))
				return false;
			if(hook.requireSource() && msg.source == null)
				return false;
			return hook.minParams() <= msg.args.length;
		}

		public static void handle(IRCServer srv, IRCMessage msg)
		{
			try
			{
				for(Method m : IRCCommandHandler.class.getDeclaredMethods())
					for(Annotation a : m.getDeclaredAnnotations())
						if(a instanceof Hook)
						{
							if(matchesHook((Hook)a, msg))
								if((Boolean)m.invoke(null, srv, msg))
									return;
						}
						else if(a instanceof Hooks)
							for(Hook h : ((Hooks)a).value())
								if(matchesHook(h, msg))
									if((Boolean)m.invoke(null, srv, msg))
										return;
				handleUnhandled(srv, msg);
			}
			catch(IllegalAccessException e) { }
			catch(InvocationTargetException e)
			{
				if(e.getCause() instanceof RuntimeException)
					throw (RuntimeException)e.getCause();
			}
		}

		@Hook(command = "001")
		private static boolean onWelcome(IRCServer srv, IRCMessage msg)
		{
			if(msg.args.length >= 1)
				srv.ourNick = msg.args[0];
			srv.onRegistered();
			return false;
		}

		@Hook(command = "005", minParams = 2)
		private static boolean onISupport(IRCServer srv, IRCMessage msg)
		{
			srv.config.parseISupport(Arrays.asList(msg.args).subList(1, msg.args.length - 1));
			srv.updateComparator();
			return false;
		}

		@Hook(command = "353", minParams = 4)
		private static boolean onNamesList(IRCServer srv, IRCMessage msg)
		{
			String channel = msg.args[2];
			String names = msg.args[3];
			if(srv.waitingNames.contains(channel))
			{
				Tab tab = srv.getService().findTab(srv.getTab(), channel);
				if(tab != null)
				{
					for(String nick : names.split(" "))
					{
						NickListEntry entry = new NickListEntry();
						while(nick.length() > 0 && srv.config.statusChars.contains(nick.charAt(0)))
						{
							entry.status.add(nick.charAt(0));
							nick = nick.substring(1);
						}
						entry.updateStatus(srv);
						entry.nick = nick;
						tab.nickList.addOrdered(entry);
					}
					srv.getService().changeNickList(tab);
					return true;
				}
			}
			return false;
		}

		@Hook(command = "366", minParams = 2)
		private static boolean onEndOfNames(IRCServer srv, IRCMessage msg)
		{
			String channel = msg.args[1];
			if(srv.waitingNames.contains(channel))
			{
				srv.waitingNames.remove(channel);
				return true;
			}
			return false;
		}

		@Hooks({@Hook(command = "376"), @Hook(command = "422")})
		private static boolean onMotd(IRCServer srv, IRCMessage msg)
		{
			srv.onRegistered();
			return false;
		}

		@Hook(command = "JOIN", minParams = 1, requireSource = true)
		private static boolean onJoin(IRCServer srv, IRCMessage msg)
		{
			String nick = msg.getNick();
			String channel = msg.args[0];
			Tab tab;
			if(srv.config.nickCollator.equals(nick, srv.ourNick))
			{
				tab = srv.getService().createTab(srv.getTab(), channel);
				tab.nickList.setComparator(srv.nickListEntryComparator);
				srv.waitingNames.add(channel);
			}
			else
			{
				tab = srv.getService().findTab(srv.getTab(), channel);
				if(tab != null)
				{
					tab.nickList.addOrdered(new NickListEntry(nick));
					srv.getService().changeNickList(tab);
				}
			}
			if(tab != null)
				tab.putLine(new TextEvent(TextEvent.JOIN, nick, msg.getUserHost(), channel));
			return tab != null;
		}

		@Hook(command = "MODE", minParams = 2, requireSource = true)
		private static boolean onMode(IRCServer srv, IRCMessage msg)
		{
			String from = msg.getNick();
			String channel = msg.args[0];
			String mode = msg.args[1];
			Tab tab = srv.getService().findTab(srv.getTab(), channel);
			if(tab != null)
			{
				boolean changed = false;
				for(IRCServerConfig.Mode m : srv.config.parseModes(mode, Arrays.asList(msg.args).subList(2, msg.args.length)))
					if(m.isStatus())
						for(int j = 0; j < tab.nickList.size(); j++)
						{
							NickListEntry entry = tab.nickList.get(j);
							if(srv.config.nickCollator.equals(entry.nick, m.getArgument()))
							{
								if(m.isSet())
									entry.status.add(m.getStatus());
								else
									entry.status.remove(m.getStatus());
								entry.updateStatus(srv);
								tab.nickList.setOrdered(j, entry);
								changed = true;
								break;
							}
						}
				String modes = mode;
				for(int i = 2; i < msg.args.length; i++)
					modes += " " + msg.args[i];
				tab.putLine(new TextEvent(TextEvent.MODE_CHANGE, from, channel, modes));
				if(changed)
					srv.getService().changeNickList(tab);
			}
			return tab != null;
		}

		@Hook(command = "NICK", minParams = 1, requireSource = true)
		private static boolean onNick(IRCServer srv, IRCMessage msg)
		{
			String from = msg.getNick();
			String to = msg.args[0];
			boolean seen = false;
			if(srv.config.nickCollator.equals(from, srv.ourNick))
			{
				srv.ourNick = to;
				srv.getTab().putLine(new TextEvent(TextEvent.NICK_CHANGE, from, to));
				seen = true;
			}
			for(Tab tab : srv.getService().tabs)
				for(int i = 0; i < tab.nickList.size(); i++)
				{
					NickListEntry entry = tab.nickList.get(i);
					if(srv.config.nickCollator.equals(entry.nick, from))
					{
						entry.nick = to;
						tab.nickList.setOrdered(i, entry);
						srv.getService().changeNickList(tab);
						tab.putLine(new TextEvent(TextEvent.NICK_CHANGE, from, to));
						seen = true;
						break;
					}
				}
			return seen;
		}

		@Hook(command = "PART", minParams = 1, requireSource = true)
		private static boolean onPart(IRCServer srv, IRCMessage msg)
		{
			String nick = msg.getNick();
			String channel = msg.args[0];
			String reason = msg.args.length >= 2 ? msg.args[1] : null;
			Tab tab = srv.getService().findTab(srv.getTab(), channel);
			if(srv.config.nickCollator.equals(nick, srv.ourNick))
			{
				if(tab != null)
					srv.getService().deleteTab(tab.getId());
				if(reason == null)
					srv.getTab().putLine(new TextEvent(TextEvent.PART, nick, msg.getUserHost(), channel));
				else
					srv.getTab().putLine(new TextEvent(TextEvent.PART_WITH_REASON, nick, msg.getUserHost(), channel, reason));
				return true;
			}
			else
				if(tab != null)
				{
					boolean found = false;
					for(int i = 0; i < tab.nickList.size(); )
						if(srv.config.nickCollator.equals(tab.nickList.get(i).nick, nick))
						{
							tab.nickList.remove(i);
							found = true;
						}
						else
							i++;
					if(found)
					{
						srv.getService().changeNickList(tab);
						if(reason == null)
							tab.putLine(new TextEvent(TextEvent.PART, nick, msg.getUserHost(), channel));
						else
							tab.putLine(new TextEvent(TextEvent.PART_WITH_REASON, nick, msg.getUserHost(), channel, reason));
					}
				}
			return tab != null;
		}

		@Hook(command = "PING")
		private static boolean onPing(IRCServer srv, IRCMessage msg)
		{
			srv.send(new IRCMessage("PONG", msg.args));
			return true;
		}

		@Hook(command = "PRIVMSG", minParams = 2, requireSource = true)
		private static boolean onPrivmsg(IRCServer srv, IRCMessage msg)
		{
			String nick = msg.getNick();
			String target = msg.args[0];
			String text = msg.args[1];
			if(srv.config.nickCollator.equals(target, srv.ourNick))
			{
				Tab tab = srv.getService().findTab(srv.getTab(), nick);
				if(tab == null)
					tab = srv.getService().createTab(srv.getTab(), nick);
				tab.putLine(new TextEvent(TextEvent.MESSAGE, nick, text));
				return true;
			}
			else
			{
				Tab tab = srv.getService().findTab(srv.getTab(), target);
				if(tab != null)
					tab.putLine(new TextEvent(TextEvent.MESSAGE, nick, text));
				return tab != null;
			}
		}

		@Hook(command = "QUIT", requireSource = true)
		private static boolean onQuit(IRCServer srv, IRCMessage msg)
		{
			String nick = msg.getNick();
			String reason = msg.args.length >= 1 ? msg.args[0] : null;
			boolean seen = false;
			for(Tab tab : srv.getService().tabs)
			{
				boolean found = false;
				for(int i = 0; i < tab.nickList.size(); )
					if(srv.config.nickCollator.equals(tab.nickList.get(i).nick, nick))
					{
						tab.nickList.remove(i);
						found = true;
					}
					else
						i++;
				if(found)
				{
					seen = true;
					srv.getService().changeNickList(tab);
					if(reason == null)
						tab.putLine(new TextEvent(TextEvent.QUIT, nick, msg.getUserHost()));
					else
						tab.putLine(new TextEvent(TextEvent.QUIT_WITH_REASON, nick, msg.getUserHost(), reason));
				}
			}
			return seen;
		}

		private static void handleUnhandled(IRCServer srv, IRCMessage msg)
		{
			if(msg.command.matches("[0-9]*"))
			{
				String args = "";
				for(int i = 1; i < msg.args.length; i++)
				{
					if(i != 1)
						args += " ";
					args += msg.args[i];
				}
				srv.getTab().putLine(new TextEvent(TextEvent.NUMERIC, msg.command, args));
			}
			else
				srv.getTab().putLine(new TextEvent(TextEvent.RAW, msg.toIRC()));
		}
	}
}
