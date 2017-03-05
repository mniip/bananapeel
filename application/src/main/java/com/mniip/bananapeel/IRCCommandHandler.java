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
		catch(InvocationTargetException e) { }
	}

	private static void handleUnhandled(Server srv, IRCMessage msg)
	{
	}
}
