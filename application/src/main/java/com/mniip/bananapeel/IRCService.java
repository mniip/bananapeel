package com.mniip.bananapeel;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class IRCService extends Service
{
	@Override
	public void onCreate()
	{
		Log.d("BananaPeel", "Service created");
		super.onCreate();

		Notification n = new Notification.Builder(this)
			.setSmallIcon(R.drawable.app_icon)
			.setContentTitle("Notification")
			.build();
		startForeground(1, n);

		((ServiceApplication)getApplicationContext()).onServiceStarted(this);
	}

	@Override
	public void onDestroy()
	{
		Log.d("BananaPeel", "Service destroyed");
		super.onDestroy();
		((ServiceApplication)getApplicationContext()).onServiceStopped();
	}

	@Override
	public IBinder onBind(Intent i) { return null; }
}
