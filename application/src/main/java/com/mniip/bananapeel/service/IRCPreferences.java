package com.mniip.bananapeel.service;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

public class IRCPreferences
{
	private Context context;
	private SharedPreferences preferences;
	private SharedPreferences servers;

	public static final String PREF_FILE = "main";

	private static final String PREF_DEFAULT_NICK = "defaultNick";
	private static final String PREF_DEFAULT_NICK_ALT = "defaultNickAlt";
	private static final String PREF_DEFAULT_USER = "defaultUser";
	private static final String PREF_DEFAULT_REAL_NAME = "defaultRealName";

	private void setDefaults(SharedPreferences p)
	{
		SharedPreferences.Editor e = p.edit();
		if(p.getString(PREF_DEFAULT_NICK, null) == null)
			e.putString(PREF_DEFAULT_NICK, "BananaPeel");
		if(p.getString(PREF_DEFAULT_NICK_ALT, null) == null)
			e.putString(PREF_DEFAULT_NICK_ALT, "BananaPeel_");
		if(p.getString(PREF_DEFAULT_USER, null) == null)
			e.putString(PREF_DEFAULT_USER, "android");
		if(p.getString(PREF_DEFAULT_REAL_NAME, null) == null)
			e.putString(PREF_DEFAULT_REAL_NAME, "Banana Peel");
		e.apply();
	}

	public IRCPreferences(Context context)
	{
		this.context = context;
		preferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
		servers = context.getSharedPreferences(IRCServerPreferences.Concrete.PREF_FILE, Context.MODE_PRIVATE);
		setDefaults(preferences);
	}

	public List<String> listServers()
	{
		return IRCServerPreferences.listServers(servers);
	}

	public IRCServerPreferences.Concrete getServer(String name)
	{
		if(IRCServerPreferences.hasServer(servers, name))
			return new IRCServerPreferences.Concrete(servers, name, this);
		else
			return null;
	}

	public IRCServerPreferences.Concrete newServer(String name)
	{
		return new IRCServerPreferences.Concrete(servers, name, this);
	}

	public String getDefaultNick()
	{
		return preferences.getString(PREF_DEFAULT_NICK, null);
	}

	public String getDefaultNickAlt()
	{
		return preferences.getString(PREF_DEFAULT_NICK_ALT, null);
	}

	public String getDefaultUser()
	{
		return preferences.getString(PREF_DEFAULT_USER, null);
	}

	public String getDefaultRealName()
	{
		return preferences.getString(PREF_DEFAULT_REAL_NAME, null);
	}
}
