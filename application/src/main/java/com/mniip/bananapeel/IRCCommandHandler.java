package com.mniip.bananapeel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class IRCCommandHandler
{
	@Retention(RetentionPolicy.RUNTIME)
	private @interface Hook { }

	public static void handle(Server srv, IRCMessage msg)
	{
		String methodName = "handle" + msg.command;
		Method m;
		try
		{
			m = IRCCommandHandler.class.getDeclaredMethod(methodName, Server.class, IRCMessage.class);
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
	private static void handleJOIN(Server srv, IRCMessage msg)
	{
		if(msg.args.length >= 1 && msg.source != null)
		{
			String nick = msg.getNick();
			String channel = msg.args[0];
			Tab tab;
			if(nick.equals(srv.ourNick))
			{
				tab = srv.getService().createTab();
				tab.setServer(srv);
				tab.setTitle(channel);
			}
			else
				tab = srv.getService().findTab(srv, channel);
			if(tab != null)
				tab.putLine("* " + nick + " joined " + channel);

		}
	}

	@IRCCommandHandler.Hook
	private static void handlePRIVMSG(Server srv, IRCMessage msg)
	{
		if(msg.args.length >= 2)
		{
			String nick = msg.getNick();
			String target = msg.args[0];
			String text = msg.args[1];
			String location = target.equals(srv.ourNick) ? nick : target;
			Tab tab = srv.getService().findTab(srv, location);
			if(tab == null)
			{
				tab = srv.getService().createTab();
				tab.setServer(srv);
				tab.setTitle(nick); /* TODO */
			}
			tab.putLine("<" + nick + "> " + text);
		}
	}

	private static void handleUnhandled(Server srv, IRCMessage msg)
	{
		srv.getTab().putLine(msg.toIRC());
	}
}
