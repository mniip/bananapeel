package com.mniip.bananapeel;

import android.support.v4.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainScreen extends FragmentActivity
{
	private TabAdapter tabAdapter;
	private NickListAdapter nickListAdapter;

	public TabAdapter getTabAdapter()
	{
		return tabAdapter;
	}
	public NickListAdapter getNickListAdapter()
	{
		return nickListAdapter;
	}

	private final IRCInterfaceListener ircInterfaceListener = new IRCInterfaceListener()
	{
		@Override
		public void onTabLinesAdded(int tabId)
		{
			tabAdapter.onTabLinesAdded(tabId);
		}

		@Override
		public void onTabCleared(int tabId)
		{
			tabAdapter.onTabCleared(tabId);
		}

		@Override
		public void onTabAdded(int tabId)
		{
			tabAdapter.onTabAdded(tabId);
		}

		@Override
		public void onTabRemoved(int tabId)
		{
			tabAdapter.onTabRemoved(tabId);
		}

		@Override
		public void onTabTitleChanged(int tabId)
		{
			tabAdapter.onTabTitleChanged(tabId);
		}

		@Override
		public void onTabNickListChanged(int tabId)
		{
			nickListAdapter.onTabNickListChanged(tabId);
		}
	};

	private IRCService getService()
	{
		return ServiceApplication.getService();
	}

	private final ServiceConnection conn = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName cls, IBinder binder)
		{
			getService().setListener(ircInterfaceListener);

			ViewGroup group = (ViewGroup)findViewById(R.id.pager_container);
			LayoutInflater.from(MainScreen.this).inflate(R.layout.pager, group, true);

			ViewPager pager = (ViewPager)group.findViewById(R.id.view_pager);
			tabAdapter = new TabAdapter(getSupportFragmentManager(), MainScreen.this, getService());
			pager.setAdapter(tabAdapter);
		}

		@Override
		public void onServiceDisconnected(ComponentName cls)
		{
			getService().unsetListener(ircInterfaceListener);
		}
	};

	public class NickListAdapter extends BaseAdapter
	{
		@Override
		public int getCount()
		{
			IRCService service = getService();
			if(service == null)
				return 0;
			return service.getFrontTab().nickList.size();
		}

		@Override
		public Object getItem(int position)
		{
			return getService().getFrontTab().nickList.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return getService().getFrontTab().nickList.get(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if(convertView == null)
				convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.nick_line, parent, false);

			NickListEntry entry = getService().getFrontTab().nickList.get(position);
			((TextView)convertView).setText((entry.highestStatus == null ? "" : entry.highestStatus.toString()) + entry.nick);
			return convertView;
		}

		public void onTabNickListChanged(int tabId)
		{
			if(getService().getFrontTab().getId() == tabId)
				notifyDataSetChanged();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.d("BananaPeel", "MainActivity created");
		super.onCreate(savedInstanceState);

		((ServiceApplication)getApplication()).ensureServiceStarted();
		Intent i = new Intent(this, IRCService.class);
		bindService(i, conn, BIND_IMPORTANT);

		setContentView(R.layout.main_screen);

		int width = getResources().getDisplayMetrics().widthPixels/2;

		ListView serverList = (ListView)findViewById(R.id.server_list);
		DrawerLayout.LayoutParams sParams = (DrawerLayout.LayoutParams) serverList.getLayoutParams();// android.support.v4.widget.
		sParams.width = width;
		serverList.setLayoutParams(sParams);

		ListView nickList = (ListView)findViewById(R.id.nick_list);
		nickListAdapter = new NickListAdapter();
		nickList.setAdapter(nickListAdapter);
		DrawerLayout.LayoutParams Nparams = (DrawerLayout.LayoutParams) nickList.getLayoutParams();// android.support.v4.widget.
		Nparams.width = width;
		nickList.setLayoutParams(Nparams);
	}

	@Override
	protected void onDestroy()
	{
		Log.d("BananaPeel", "MainActivity destroyed");
		super.onDestroy();

		unbindService(conn);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.xml.main_screen_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getItemId() == R.id.menu_settings)
		{
			startActivity(new Intent(this, PreferencesScreen.class));
			return true;
		}
		return false;
	}
}
