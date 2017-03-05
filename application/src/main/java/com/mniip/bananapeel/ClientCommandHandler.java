package com.mniip.bananapeel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ClientCommandHandler
{
	@Retention(RetentionPolicy.RUNTIME)
	private @interface Hook { }

	public static void handle(Tab tab, String command, List<String> words, List<String> wordEols)
	{
		String methodName = "command" + command;
		Method m;
		try
		{
			m = ClientCommandHandler.class.getDeclaredMethod(methodName, Tab.class, List.class, List.class);
			if(m.getAnnotation(Hook.class) != null)
				m.invoke(null, tab, words, wordEols);
		}
		catch(NoSuchMethodException e)
		{
			unhandledCommand(tab, command, words, wordEols);
		}
		catch(IllegalAccessException e) { }
		catch(InvocationTargetException e) { }
	}

	@ClientCommandHandler.Hook
	private static void commandSERVER(Tab tab, List<String> words, List<String> wordEols)
	{
		Server server = new Server(tab.getService(), tab);
		tab.setServer(server);
		server.connect(words.get(1), 6667);
	}

	private static void unhandledCommand(Tab tab, String command, List<String> words, List<String> wordEols)
	{
		if(tab.getServer() != null)
			tab.getServer().send(IRCMessage.fromIRC(wordEols.get(0)));
	}
}