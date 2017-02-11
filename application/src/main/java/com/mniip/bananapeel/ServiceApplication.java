package com.mniip.bananapeel;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

public class ServiceApplication extends Application
{
	IRCService service;

	public void onServiceStarted(IRCService s)
	{
		service = s;
	}

	public void onServiceStopped()
	{
		service = null;
	}

	public void ensureServiceStarted()
	{
		Intent i = new Intent(this, IRCService.class);
		startService(i);
	}

	@Override
	public void onCreate()
	{
		Log.d("BananaPeel", "App created");
		super.onCreate();

		ensureServiceStarted();
	}
}
