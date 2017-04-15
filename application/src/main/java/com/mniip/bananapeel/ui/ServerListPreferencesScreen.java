package com.mniip.bananapeel.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;

import com.mniip.bananapeel.service.IRCPreferences;
import com.mniip.bananapeel.service.IRCServerPreferences;

import java.util.Collections;
import java.util.List;

public class ServerListPreferencesScreen extends PreferenceActivity
{
	private PreferenceCategory category;
	IRCPreferences preferences;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesName(IRCServerPreferences.Concrete.PREF_FILE);
		preferences = new IRCPreferences(this);

		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this));

		Preference addButton = new Preference(this);
		addButton.setTitle("Add server");
		addButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
		{
			@Override
			public boolean onPreferenceClick(Preference preference)
			{
				preferences.newServer(getUniqueName());
				updateServers();
				return true;
			}
		});
		getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
		{
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
			{
				Object obj = parent.getItemAtPosition(position);
				if(obj != null && obj instanceof Preference)
				{
					final IRCServerPreferences.Concrete server = preferences.getServer(((Preference)obj).getTitle().toString());
					AlertDialog.Builder builder = new AlertDialog.Builder(ServerListPreferencesScreen.this);
					final EditText input = new EditText(ServerListPreferencesScreen.this);
					input.setInputType(InputType.TYPE_CLASS_TEXT);
					input.setText(server.getName());
					builder.setView(input);
					builder.setPositiveButton("Rename", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							server.rename(input.getText().toString());
							updateServers();
						}
					});
					builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.cancel();
						}
					});
					builder.setNegativeButton("Delete", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							server.remove();
							updateServers();
						}
					});
					builder.show();
					return true;
				}
				return false;
			}
		});
		getPreferenceScreen().addPreference(addButton);

		category = new PreferenceCategory(this);
		category.setTitle("Servers");
		getPreferenceScreen().addPreference(category);
		updateServers();
	}

	private void updateServers()
	{
		category.removeAll();
		List<String> servers = preferences.listServers();
		Collections.sort(servers);
		for(final String server : servers)
		{
			Preference preference = new Preference(this);
			preference.setTitle(server);
			Intent intent = new Intent(this, ServerPreferencesScreen.class);
			intent.putExtra("server", server);
			preference.setIntent(intent);
			category.addPreference(preference);
		}
	}

	private String getUniqueName()
	{
		String name = "new server";
		if(preferences.getServer(name) == null)
			return name;
		int i = 1;
		while(preferences.getServer(name + i) != null)
			i++;
		return name + i;
	}
}
