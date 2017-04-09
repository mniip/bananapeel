package com.mniip.bananapeel.service;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IRCServerPreferences
{
	private Context context;
	private String name;
	private SharedPreferences preferences;

	public static final String PREF_FILE = "servers";

	private static final String PREF_HOST = ";host";
	private static final String PREF_PORT = ";port";

	private void setDefaults(SharedPreferences p)
	{
		SharedPreferences.Editor e = p.edit();
		if(p.getString(name + PREF_HOST, null) == null)
			e.putString(name + PREF_HOST, "localhost");
		if(p.getInt(name + PREF_PORT, -1) == -1)
			e.putInt(name + PREF_PORT, 6667);
		e.apply();
	}

	public IRCServerPreferences(SharedPreferences p, String n)
	{
		name = n;
		preferences = p;
		setDefaults(p);
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
}