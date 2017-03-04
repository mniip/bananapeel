package com.mniip.bananapeel;

import android.content.Context;
import android.content.SharedPreferences;

public class IRCPreferences
{
	private Context context;
	private SharedPreferences preferences;

	private static final String PREF_DEFAULT_NICK = "defaultNick";
	private static final String PREF_DEFAULT_USER = "defaultUser";
	private static final String PREF_DEFAULT_REAL_NAME = "defaultRealName";

	private void setDefaults(SharedPreferences p)
	{
		SharedPreferences.Editor e = p.edit();
		if(p.getString(PREF_DEFAULT_NICK, null) == null)
			e.putString(PREF_DEFAULT_NICK, "BananaPeel");
		if(p.getString(PREF_DEFAULT_USER, null) == null)
			e.putString(PREF_DEFAULT_USER, "bananapeel");
		if(p.getString(PREF_DEFAULT_REAL_NAME, null) == null)
			e.putString(PREF_DEFAULT_REAL_NAME, "Banana Peel");
		e.apply();
	}

	public IRCPreferences(Context ctx)
	{
		context = ctx;
		preferences = context.getSharedPreferences("main", Context.MODE_PRIVATE);
		setDefaults(preferences);
	}

	public String getDefaultNick()
	{
		return preferences.getString(PREF_DEFAULT_NICK, null);
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
