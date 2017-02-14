package com.mniip.bananapeel;

import android.support.v4.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

public class MainScreen extends FragmentActivity
{
	private final ServiceConnection conn = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName cls, IBinder binder) {}
		@Override
		public void onServiceDisconnected(ComponentName cls) {}
	};

	private class TabAdapter extends FragmentStatePagerAdapter
	{
		public TabAdapter(FragmentManager manager)
		{
			super(manager);
		}

		@Override
		public Fragment getItem(int position)
		{
			TabFragment fragment = new TabFragment();
			fragment.setPosition(position);
			return fragment;
		}

		@Override
		public int getCount()
		{
			return 100;
		}
	}

	@Override
    protected void onCreate(Bundle savedInstanceState)
    {
		Log.d("BananaPeel", "MainActivity created");
        super.onCreate(savedInstanceState);

		((ServiceApplication)getApplicationContext()).ensureServiceStarted();
		Intent i = new Intent(this, IRCService.class);
		bindService(i, conn, BIND_IMPORTANT);

        setContentView(R.layout.main_screen);

		ViewPager pager = (ViewPager)findViewById(R.id.view_pager);
		pager.setAdapter(new TabAdapter(getSupportFragmentManager()));
    }

	@Override
	protected void onDestroy()
	{
		Log.d("BananaPeel", "MainActivity destroyed");
		super.onDestroy();

		unbindService(conn);
	}
}
