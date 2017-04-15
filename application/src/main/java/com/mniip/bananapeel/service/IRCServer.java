package com.mniip.bananapeel.service;

import com.mniip.bananapeel.util.Collators;
import com.mniip.bananapeel.util.IRCMessage;
import com.mniip.bananapeel.util.NickListEntry;
import com.mniip.bananapeel.util.TextEvent;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class IRCServer
{
	private IRCService service;
	private ServerTab serverTab;
	private IRCConnection connection;

	private boolean registered = false;
	public String ourNick = "";
	public List<Character> statusChars = new ArrayList<>(Arrays.asList('@', '+'));
	public List<Character> statusModes = new ArrayList<>(Arrays.asList('o', 'v'));
	public Set<Character> listModes = new TreeSet<>(Arrays.asList('b', 'e', 'I'));
	public Set<Character> alwaysArgumentModes = new TreeSet<>(Arrays.asList('k'));
	public Set<Character> setArgumentModes = new TreeSet<>(Arrays.asList('f', 'l', 'j'));
	public Set<Character> noArgumentModes = new TreeSet<>(Arrays.asList('i', 'm', 'n', 's', 't'));
	private List<String> waitingNames = new ArrayList<>();

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

	public IRCServer(IRCService service, ServerTab serverTab)
	{
		this.service = service;
		this.serverTab = serverTab;
	}

	public IRCService getService()
	{
		return service;
	}

	public ServerTab getTab()
	{
		return serverTab;
	}

	public void connect(String host, int port)
	{
		if(connection != null)
			connection.disconnect();
		connection = new IRCConnection(this);
		connection.connect(host, port);
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
		ourNick = service.preferences.getDefaultNick();
		send(new IRCMessage("NICK", ourNick));
		send(new IRCMessage("USER", service.preferences.getDefaultUser(), "*", "*", service.preferences.getDefaultRealName()));
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

	private Collator nickCollator = Collators.rfc1459();
	private Comparator<NickListEntry> nickListEntryComparator = new ComposedComparator<>(NickListEntry.statusComparator(statusChars), NickListEntry.nickComparator(nickCollator));

	private void updateComparator()
	{
		nickListEntryComparator = new ComposedComparator<>(NickListEntry.statusComparator(statusChars), NickListEntry.nickComparator(nickCollator));
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

		public static void handle(IRCServer srv, IRCMessage msg)
		{
			try
			{
				for(Method m : IRCCommandHandler.class.getDeclaredMethods())
					for(Annotation a : m.getDeclaredAnnotations())
						if(a instanceof Hook)
							if(((Hook)a).command().equals(msg.command) && ((Hook)a).minParams() <= msg.args.length && (!((Hook)a).requireSource() || msg.source != null))
							{
								m.invoke(null, srv, msg);
								return;
							}
						else if(a instanceof Hooks)
							for(Hook h : ((Hooks)a).value())
								if(h.command().equals(msg.command) && h.minParams() <= msg.args.length && (!h.requireSource() || msg.source != null))
								{
									m.invoke(null, srv, msg);
									return;
								}
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
		private static void onWelcome(IRCServer srv, IRCMessage msg)
		{
			if(msg.args.length >= 1)
				srv.ourNick = msg.args[0];
			srv.onRegistered();
			handleUnhandled(srv, msg);
		}

		@Hook(command = "353", minParams = 4)
		private static void onNamesList(IRCServer srv, IRCMessage msg)
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
						while(nick.length() > 0 && srv.statusChars.contains(nick.charAt(0)))
						{
							entry.status.add(nick.charAt(0));
							nick = nick.substring(1);
						}
						entry.updateStatus(srv);
						entry.nick = nick;
						tab.nickList.addOrdered(entry);
					}
					srv.getService().changeNickList(tab);
					return;
				}
			}
			handleUnhandled(srv, msg);
		}

		@Hook(command = "366", minParams = 2)
		private static void onEndOfNames(IRCServer srv, IRCMessage msg)
		{
			String channel = msg.args[1];
			if(srv.waitingNames.contains(channel))
				srv.waitingNames.remove(channel);
			else
				handleUnhandled(srv, msg);
		}

		@Hooks({@Hook(command = "376"), @Hook(command = "422")})
		private static void onMotd(IRCServer srv, IRCMessage msg)
		{
			srv.onRegistered();
			handleUnhandled(srv, msg);
		}

		@Hook(command = "JOIN", minParams = 1, requireSource = true)
		private static void onJoin(IRCServer srv, IRCMessage msg)
		{
			String nick = msg.getNick();
			String channel = msg.args[0];
			Tab tab;
			if(nick.equals(srv.ourNick))
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
		}

		@Hook(command = "MODE", minParams = 2, requireSource = true)
		private static void onMode(IRCServer srv, IRCMessage msg)
		{
			String from = msg.getNick();
			String channel = msg.args[0];
			String mode = msg.args[1];
			Tab tab = srv.getService().findTab(srv.getTab(), channel);
			if(tab != null)
			{
				boolean changed = false;
				int argument = 2;
				boolean set = true;
				for(int i = 0; i < mode.length(); i++)
				{
					char ch = mode.charAt(i);
					if(ch == '+')
						set = true;
					else if(ch == '-')
						set = false;
					else if(srv.statusModes.contains(ch) && argument < msg.args.length)
					{
						char status = srv.statusChars.get(srv.statusModes.indexOf(ch));
						String target = msg.args[argument++];
						for(int j = 0; j < tab.nickList.size(); j++)
						{
							NickListEntry entry = tab.nickList.get(j);
							if(entry.nick.equals(target))
							{
								if(set)
									entry.status.add(status);
								else
									entry.status.remove(status);
								entry.updateStatus(srv);
								tab.nickList.setOrdered(j, entry);
								changed = true;
								break;
							}
						}
					}
					else if(srv.alwaysArgumentModes.contains(ch) || srv.listModes.contains(ch))
						argument++;
					else if(srv.setArgumentModes.contains(ch))
						argument += set ? 1 : 0;
				}
				String modes = mode;
				for(int i = 2; i < msg.args.length; i++)
					modes += " " + msg.args[i];
				tab.putLine(new TextEvent(TextEvent.MODE_CHANGE, from, channel, modes));
				if(changed)
					srv.getService().changeNickList(tab);
			}
		}

		@Hook(command = "NICK", minParams = 1, requireSource = true)
		private static void onNick(IRCServer srv, IRCMessage msg)
		{
			String from = msg.getNick();
			String to = msg.args[0];
			if(from.equals(srv.ourNick))
				srv.ourNick = to;
			for(Tab tab : srv.getService().tabs)
				for(int i = 0; i < tab.nickList.size(); i++)
				{
					NickListEntry entry = tab.nickList.get(i);
					if(entry.nick.equals(from))
					{
						entry.nick = to;
						tab.nickList.setOrdered(i, entry);
						srv.getService().changeNickList(tab);
						tab.putLine(new TextEvent(TextEvent.MODE_CHANGE, from, to));
						break;
					}
				}
		}

		@Hook(command = "PART", minParams = 1, requireSource = true)
		private static void onPart(IRCServer srv, IRCMessage msg)
		{
			String nick = msg.getNick();
			String channel = msg.args[0];
			String reason = msg.args.length >= 2 ? msg.args[1] : null;
			Tab tab = srv.getService().findTab(srv.getTab(), channel);
			if(nick.equals(srv.ourNick))
			{
				if(tab != null)
					srv.getService().deleteTab(tab.getId());
				if(reason == null)
					srv.getTab().putLine(new TextEvent(TextEvent.PART, nick, msg.getUserHost(), channel));
				else
					srv.getTab().putLine(new TextEvent(TextEvent.PART_WITH_REASON, nick, msg.getUserHost(), channel, reason));
			}
			else
				if(tab != null)
				{
					boolean found = false;
					for(int i = 0; i < tab.nickList.size(); )
						if(tab.nickList.get(i).nick.equals(nick))
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
		}

		@Hook(command = "PING")
		private static void onPing(IRCServer srv, IRCMessage msg)
		{
			srv.send(new IRCMessage("PONG", msg.args));
		}

		@Hook(command = "PRIVMSG", minParams = 2, requireSource = true)
		private static void onPrivmsg(IRCServer srv, IRCMessage msg)
		{
			String nick = msg.getNick();
			String target = msg.args[0];
			String text = msg.args[1];
			if(target.equals(srv.ourNick))
			{
				Tab tab = srv.getService().findTab(srv.getTab(), nick);
				if(tab == null)
					tab = srv.getService().createTab(srv.getTab(), nick);
				tab.putLine(new TextEvent(TextEvent.MESSAGE, nick, text));
			}
			else
			{
				Tab tab = srv.getService().findTab(srv.getTab(), target);
				if(tab != null)
					tab.putLine(new TextEvent(TextEvent.MESSAGE, nick, text));
			}
		}

		@Hook(command = "QUIT", requireSource = true)
		private static void onQuit(IRCServer srv, IRCMessage msg)
		{
			String nick = msg.getNick();
			String reason = msg.args.length >= 1 ? msg.args[0] : null;
			for(Tab tab : srv.getService().tabs)
			{
				boolean found = false;
				for(int i = 0; i < tab.nickList.size(); )
					if(tab.nickList.get(i).nick.equals(nick))
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
						tab.putLine(new TextEvent(TextEvent.QUIT, nick, msg.getUserHost()));
					else
						tab.putLine(new TextEvent(TextEvent.QUIT_WITH_REASON, nick, msg.getUserHost(), reason));
				}
			}
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
