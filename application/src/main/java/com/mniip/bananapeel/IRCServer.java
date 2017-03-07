package com.mniip.bananapeel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class IRCServer
{
	private IRCService service;
	private ServerTab serverTab;
	private IRCConnection connection;
	private boolean registered = false;
	public String ourNick = "";

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
		private @interface Hook { }

		public static void handle(IRCServer srv, IRCMessage msg)
		{
			String methodName = "handle" + msg.command.toUpperCase();
			try
			{
				Method m = IRCCommandHandler.class.getDeclaredMethod(methodName, IRCServer.class, IRCMessage.class);
				if(m.getAnnotation(Hook.class) != null)
					m.invoke(null, srv, msg);
			}
			catch(NoSuchMethodException e)
			{
				handleUnhandled(srv, msg);
			}
			catch(IllegalAccessException e) { }
			catch(InvocationTargetException e)
			{
				if(e.getCause() instanceof RuntimeException)
					throw (RuntimeException)e.getCause();
			}
		}

		@IRCCommandHandler.Hook
		private static void handle001(IRCServer srv, IRCMessage msg)
		{
			if(msg.args.length >= 1)
				srv.ourNick = msg.args[0];
			srv.onRegistered();
			handleUnhandled(srv, msg);
		}

		@IRCCommandHandler.Hook
		private static void handle376(IRCServer srv, IRCMessage msg)
		{
			srv.onRegistered();
			handleUnhandled(srv, msg);
		}

		@IRCCommandHandler.Hook
		private static void handl422(IRCServer srv, IRCMessage msg)
		{
			srv.onRegistered();
			handleUnhandled(srv, msg);
		}

		@IRCCommandHandler.Hook
		private static void handleJOIN(IRCServer srv, IRCMessage msg)
		{
			if(msg.args.length >= 1 && msg.source != null)
			{
				String nick = msg.getNick();
				String channel = msg.args[0];
				Tab tab;
				if(nick.equals(srv.ourNick))
					tab = srv.getService().createTab(srv.getTab(), channel);
				else
					tab = srv.getService().findTab(srv.getTab(), channel);
				if(tab != null)
					tab.putLine("* " + nick + " joined " + channel);
			}
			else
				handleUnhandled(srv, msg);
		}

		@IRCCommandHandler.Hook
		private static void handleNICK(IRCServer srv, IRCMessage msg)
		{
			if(msg.args.length >= 1 && msg.source != null)
			{
				String from = msg.getNick();
				String to = msg.args[0];
				if(from.equals(srv.ourNick))
					srv.ourNick = to;
			}
		}

		@IRCCommandHandler.Hook
		private static void handlePING(IRCServer srv, IRCMessage msg)
		{
			srv.send(new IRCMessage("PONG", msg.args));
		}

		@IRCCommandHandler.Hook
		private static void handlePRIVMSG(IRCServer srv, IRCMessage msg)
		{
			if(msg.args.length >= 2 && msg.source != null)
			{
				String nick = msg.getNick();
				String target = msg.args[0];
				String text = msg.args[1];
				String location = target.equals(srv.ourNick) ? nick : target;
				Tab tab = srv.getService().findTab(srv.getTab(), location);
				if(tab == null)
					tab = srv.getService().createTab(srv.getTab(), nick);
				tab.putLine("<" + nick + "> " + text);
			}
			else
				handleUnhandled(srv, msg);
		}

		private static void handleUnhandled(IRCServer srv, IRCMessage msg)
		{
			srv.getTab().putLine(msg.toIRC());
		}
	}
}
