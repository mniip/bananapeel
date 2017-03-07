package com.mniip.bananapeel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class IRCCommandHandler
{
	@Retention(RetentionPolicy.RUNTIME)
	private @interface Hook { }

	public static void handle(IRCServer srv, IRCMessage msg)
	{
		String methodName = "handle" + msg.command.toUpperCase();
		Method m;
		try
		{
			m = IRCCommandHandler.class.getDeclaredMethod(methodName, IRCServer.class, IRCMessage.class);
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
	}

	@IRCCommandHandler.Hook
	private static void handlePING(IRCServer srv, IRCMessage msg)
	{
		srv.send(new IRCMessage("PONG", msg.args));
	}

	@IRCCommandHandler.Hook
	private static void handlePRIVMSG(IRCServer srv, IRCMessage msg)
	{
		if(msg.args.length >= 2)
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
	}

	private static void handleUnhandled(IRCServer srv, IRCMessage msg)
	{
		srv.getTab().putLine(msg.toIRC());
	}
}
