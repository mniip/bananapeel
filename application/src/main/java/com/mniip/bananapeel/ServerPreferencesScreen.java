package com.mniip.bananapeel;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class ServerPreferencesScreen extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private String server;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		server = getIntent().getStringExtra("server");

		getPreferenceManager().setSharedPreferencesName(IRCServerPreferences.PREF_FILE);
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		addPreferencesFromResource(R.xml.server_preferences);

		setKeys(getPreferenceScreen());
		setDescriptions(getPreferenceScreen());
	}

	private void setKeys(PreferenceScreen screen)
	{
		for(int i = 0; i < screen.getPreferenceCount(); i++)
		{
			Preference preference = screen.getPreference(i);
			String key = server + ";" + preference.getKey();
			preference.setKey(key);
			if(preference instanceof EditIntPreference)
				((EditIntPreference)preference).setInt(preference.getSharedPreferences().getInt(key, -1));
			else if(preference instanceof EditTextPreference)
				((EditTextPreference)preference).setText(preference.getSharedPreferences().getString(key, null));
			if(preference instanceof PreferenceScreen)
				setKeys((PreferenceScreen)preference);
		}
	}

	private void setDescriptions(PreferenceScreen screen)
	{
		for(int i = 0; i < screen.getPreferenceCount(); i++)
		{
			Preference preference = screen.getPreference(i);
			if(preference instanceof EditTextPreference)
				preference.setSummary(((EditTextPreference)preference).getText());
			if(preference instanceof PreferenceScreen)
				setDescriptions((PreferenceScreen)preference);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key)
	{
		Preference preference = findPreference(key);
		if(preference instanceof EditTextPreference)
			preference.setSummary(((EditTextPreference)preference).getText());
	}
}
