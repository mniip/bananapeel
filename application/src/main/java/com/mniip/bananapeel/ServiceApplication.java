package com.mniip.bananapeel;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.mniip.bananapeel.service.IRCService;

public class ServiceApplication extends Application
{
	private static IRCService service;

	public void onServiceStarted(IRCService s)
	{
		service = s;
	}

	public void onServiceStopped()
	{
		service = null;
	}

	public static IRCService getService()
	{
		return service;
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
