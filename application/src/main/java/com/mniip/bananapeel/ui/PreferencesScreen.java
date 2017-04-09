package com.mniip.bananapeel.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.mniip.bananapeel.service.IRCPreferences;
import com.mniip.bananapeel.R;

public class PreferencesScreen extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// TODO: add a non-deprecated implementation for a newer API levels
		getPreferenceManager().setSharedPreferencesName(IRCPreferences.PREF_FILE);
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		addPreferencesFromResource(R.xml.preferences);

		setDescriptions(getPreferenceScreen());
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
