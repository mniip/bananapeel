package com.mniip.bananapeel.service;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class IRCServerPreferences
{
	abstract public String getName();
	abstract public String getHost();
	abstract public int getPort();
	abstract public String getNick();
	abstract public String getNickAlt();
	abstract public String getUser();
	abstract public String getRealName();
	abstract public String getAuthMode();
	abstract public String getPassword();

	public static List<String> listServers(SharedPreferences p)
	{
		ArrayList<String> list = new ArrayList<>();
		for(String key : p.getAll().keySet())
		{
			int idx = key.indexOf(';');
			if(idx != -1)
			{
				String server = key.substring(0, idx);
				if(!list.contains(server))
					list.add(server);
			}
		}
		return list;
	}

	public static boolean hasServer(SharedPreferences p, String server)
	{
		for(String key : p.getAll().keySet())
			if(key.startsWith(server + ";"))
				return true;
		return false;
	}

	public static class Concrete extends IRCServerPreferences
	{

		private IRCPreferences parent;
		private String name;
		private SharedPreferences preferences;

		public static final String PREF_FILE = "servers";

		private static final String PREF_HOST = ";host";
		private static final String PREF_PORT = ";port";
		private static final String PREF_NICK = ";nick";
		private static final String PREF_NICK_ALT = ";nickAlt";
		private static final String PREF_USER = ";user";
		private static final String PREF_REAL_NAME = ";realName";
		private static final String PREF_AUTH_MODE = ";authMode";
		private static final String PREF_PASSWORD = ";password";

		private void setDefaults(SharedPreferences p)
		{
			SharedPreferences.Editor e = p.edit();
			if(p.getString(name + PREF_HOST, null) == null)
				e.putString(name + PREF_HOST, "localhost");
			if(p.getInt(name + PREF_PORT, -1) == -1)
				e.putInt(name + PREF_PORT, 6667);
			if(p.getString(name + PREF_AUTH_MODE, null) == null)
				e.putString(name + PREF_AUTH_MODE, "none");
			e.apply();
		}

		public Concrete(SharedPreferences preferences, String name, IRCPreferences parent)
		{
			this.parent = parent;
			this.name = name;
			this.preferences = preferences;
			setDefaults(preferences);
		}

		public String getName()
		{
			return name;
		}

		public void rename(String newName)
		{
			newName = newName.replace(";", "");
			SharedPreferences.Editor editor = preferences.edit();
			for(Map.Entry<String,?> entry : preferences.getAll().entrySet())
			{
				if(entry.getKey().startsWith(newName + ";"))
					return;
				if(entry.getKey().startsWith(name + ";"))
				{
					String newKey = newName + entry.getKey().substring(name.length());
					Object v = entry.getValue();
					if(v instanceof Boolean)
						editor.putBoolean(newKey, (Boolean)v);
					if(v instanceof Float)
						editor.putFloat(newKey, (Float)v);
					if(v instanceof Integer)
						editor.putInt(newKey, (Integer)v);
					if(v instanceof Long)
						editor.putLong(newKey, (Long)v);
					if(v instanceof String)
						editor.putString(newKey, (String)v);
					editor.remove(entry.getKey());
				}
			}
			editor.apply();
			name = newName;
		}

		public void remove()
		{
			SharedPreferences.Editor editor = preferences.edit();
			for(String key : preferences.getAll().keySet())
				if(key.startsWith(name + ";"))
					editor.remove(key);
			editor.apply();
		}

		public String getHost()
		{
			return preferences.getString(name + PREF_HOST, null);
		}

		public int getPort()
		{
			return preferences.getInt(name + PREF_PORT, -1);
		}

		public String getNick()
		{
			String nick = preferences.getString(name + PREF_NICK, "");
			if(nick.isEmpty())
				nick = parent.getDefaultNick();
			return nick;
		}

		public String getNickAlt()
		{
			String nick = preferences.getString(name + PREF_NICK_ALT, "");
			if(nick.isEmpty())
				nick = parent.getDefaultNickAlt();
			return nick;
		}

		public String getUser()
		{
			String user = preferences.getString(name + PREF_USER, "");
			if(user.isEmpty())
				user = parent.getDefaultNick();
			return user;
		}

		public String getRealName()
		{
			String realName = preferences.getString(name + PREF_REAL_NAME, "");
			if(realName.isEmpty())
				realName = parent.getDefaultNick();
			return realName;
		}

		public String getAuthMode()
		{
			return preferences.getString(name + PREF_AUTH_MODE, "");
		}

		public String getPassword()
		{
			return preferences.getString(name + PREF_PASSWORD, "");
		}
	}

	public static class Dummy extends IRCServerPreferences
	{
		private IRCPreferences parent;
		private String name;

		private String host;
		private int port;

		public Dummy(String name, IRCPreferences parent, String host, int port)
		{
			this.name = name;
			this.parent = parent;
			this.host = host;
			this.port = port;
		}

		public String getName()
		{
			return name;
		}

		public String getHost()
		{
			return host;
		}

		public int getPort()
		{
			return port;
		}

		public String getNick()
		{
			return parent.getDefaultNick();
		}

		public String getNickAlt()
		{
			return parent.getDefaultNickAlt();
		}

		public String getUser()
		{
			return parent.getDefaultUser();
		}

		public String getRealName()
		{
			return parent.getDefaultRealName();
		}

		public String getAuthMode()
		{
			return "none";
		}

		public String getPassword()
		{
			return "";
		}
	}
}