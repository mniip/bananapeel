package com.mniip.bananapeel.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.mniip.bananapeel.service.IRCServerPreferences;
import com.mniip.bananapeel.R;

public class ServerPreferencesScreen extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private String server;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		server = getIntent().getStringExtra("server");

		getPreferenceManager().setSharedPreferencesName(IRCServerPreferences.Concrete.PREF_FILE);
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
			else if(preference instanceof ListPreference)
				((ListPreference)preference).setValue(preference.getSharedPreferences().getString(key, null));
			else if(preference instanceof CheckBoxPreference)
				((CheckBoxPreference)preference).setChecked(preference.getSharedPreferences().getBoolean(key, false));
			if(preference instanceof PreferenceScreen)
				setKeys((PreferenceScreen)preference);
		}
	}

	private void setDescriptions(PreferenceScreen screen)
	{
		for(int i = 0; i < screen.getPreferenceCount(); i++)
		{
			Preference preference = screen.getPreference(i);
			setSingleDescription(preference);
			if(preference instanceof PreferenceScreen)
				setDescriptions((PreferenceScreen)preference);
		}
	}

	private void setSingleDescription(Preference preference)
	{
		if(preference.getKey().endsWith(";password"))
			return;
		if(preference instanceof EditTextPreference)
			preference.setSummary(((EditTextPreference)preference).getText());
		if(preference instanceof ListPreference)
			preference.setSummary(((ListPreference)preference).getEntry());
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key)
	{
		Preference preference = findPreference(key);
		if(preference != null)
			setSingleDescription(preference);
	}
}
