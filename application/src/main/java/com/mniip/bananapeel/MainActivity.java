package com.mniip.bananapeel;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class MainActivity extends Activity
{
	private final ServiceConnection conn = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName cls, IBinder binder) {}
		@Override
		public void onServiceDisconnected(ComponentName cls) {}
	};

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
		Log.d("BananaPeel", "MainActivity created");
        super.onCreate(savedInstanceState);

		((ServiceApplication)getApplicationContext()).ensureServiceStarted();
		Intent i = new Intent(this, IRCService.class);
		bindService(i, conn, BIND_IMPORTANT);

        setContentView(R.layout.activity_main);
    }

	@Override
	protected void onDestroy()
	{
		Log.d("BananaPeel", "MainActivity destroyed");
		super.onDestroy();

		unbindService(conn);
	}
}
