package com.mniip.bananapeel;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class IRCServer
{
	private IRCService service;
	private ServerTab serverTab;
	private IRCConnection connection;

	private boolean registered = false;
	public String ourNick = "";
	private ArrayList<String> waitingNames = new ArrayList<>();

	public IRCServer(IRCService srv, ServerTab sTab)
	{
		service = srv;
		serverTab = sTab;
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
		getTab().putLine(e.toString());
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
						while(nick.length() > 0 && (nick.charAt(0) == '@' || nick.charAt(0) == '+'))
							nick = nick.substring(1);
						tab.nickList.add(nick);
						srv.getService().changeNickList(tab);
					}
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
				srv.waitingNames.add(channel);
			}
			else
			{
				tab = srv.getService().findTab(srv.getTab(), channel);
				if(tab != null)
				{
					tab.nickList.add(nick);
					srv.getService().changeNickList(tab);
				}
			}
			if(tab != null)
				tab.putLine("* " + nick + " joined " + channel);
		}

		@Hook(command = "NICK", minParams = 1, requireSource = true)
		private static void onNick(IRCServer srv, IRCMessage msg)
		{
			String from = msg.getNick();
			String to = msg.args[0];
			if(from.equals(srv.ourNick))
				srv.ourNick = to;
			for(Tab tab : srv.getService().tabs)
				if(tab.nickList.contains(from))
				{
					tab.nickList.remove(from);
					tab.nickList.add(to);
					srv.getService().changeNickList(tab);
					tab.putLine("* " + from + " changed nick to " + to);
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
			}
			else
				if(tab != null)
				{
					tab.nickList.remove(nick);
					srv.getService().changeNickList(tab);
					tab.putLine("* " + nick + " left " + channel + (reason == null ? "" : " (" + reason + ")"));
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
				tab.putLine("<" + nick + "> " + text);
				return;
			}
			else
			{
				Tab tab = srv.getService().findTab(srv.getTab(), target);
				if(tab != null)
					tab.putLine("<" + nick + "> " + text);
				return;
			}
		}

		@Hook(command = "QUIT", requireSource = true)
		private static void onQuit(IRCServer srv, IRCMessage msg)
		{
			String nick = msg.getNick();
			String reason = msg.args.length >= 1 ? msg.args[0] : null;
			for(Tab tab : srv.getService().tabs)
				if(tab.nickList.contains(nick))
				{
					tab.nickList.remove(nick);
					srv.getService().changeNickList(tab);
					tab.putLine("* " + nick + " quit" + (reason == null ? "" : " (" + reason + ")"));
				}
		}

		private static void handleUnhandled(IRCServer srv, IRCMessage msg)
		{
			srv.getTab().putLine(msg.toIRC());
		}
	}
}
