package com.mniip.bananapeel.ui;

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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.mniip.bananapeel.service.IRCService;
import com.mniip.bananapeel.util.NickListEntry;
import com.mniip.bananapeel.R;
import com.mniip.bananapeel.ServiceApplication;

public class MainScreen extends FragmentActivity
{
	private TabAdapter tabAdapter;
	private NickListAdapter nickListAdapter;

	Bundle mSavedInstanceState;

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
			IRCService service = getService();
			service.setListener(ircInterfaceListener);

			ViewGroup group = (ViewGroup)findViewById(R.id.pager_container);
			LayoutInflater.from(MainScreen.this).inflate(R.layout.pager, group, true);

			ViewPager pager = (ViewPager)group.findViewById(R.id.view_pager);
			tabAdapter = new TabAdapter(getSupportFragmentManager(), MainScreen.this, service);
			if (mSavedInstanceState != null)
				pager.onRestoreInstanceState(mSavedInstanceState.getParcelable(((Integer)R.id.view_pager).toString()));
			pager.setAdapter(tabAdapter);

			if (service.getFrontTab().nickList != null)
			{
				View nickList = (View)findViewById(R.id.nick_list);
				DrawerLayout drawerLayout = (DrawerLayout)findViewById(R.id.drawerLayout);
				drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, nickList);
			}
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
			if(service == null || service.getFrontTab().nickList == null)
				return 0;
			return service.getFrontTab().nickList.size();
		}

		@Override
		public Object getItem(int position)
		{
			return getService().getFrontTab().nickList.getSecondary(position);
		}

		@Override
		public long getItemId(int position)
		{
			return getService().getFrontTab().nickList.getSecondary(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if(convertView == null)
				convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.nick_line, parent, false);

			NickListEntry entry = getService().getFrontTab().nickList.getSecondary(position);
			((TextView)convertView).setText((entry.highestStatus == null ? "" : entry.highestStatus.toString()) + entry.nick);
			return convertView;
		}

		public void onTabNickListChanged(int tabId)
		{
			if(getService().getFrontTab().getId() == tabId)
			{
				View nickList = (View)findViewById(R.id.nick_list);
				DrawerLayout drawerLayout = (DrawerLayout)findViewById(R.id.drawerLayout);
				if (getService().getFrontTab().nickList == null)
				{
					drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, nickList);
				}
				else if (drawerLayout.getDrawerLockMode(nickList) == DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
					drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, nickList);
				notifyDataSetChanged();
			}
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

		ListView channelList = (ListView)findViewById(R.id.channel_list);
		DrawerLayout.LayoutParams sParams = (DrawerLayout.LayoutParams) channelList.getLayoutParams();// android.support.v4.widget.
		sParams.width = width;
		channelList.setLayoutParams(sParams);

		ListView nickList = (ListView)findViewById(R.id.nick_list);

		nickList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
		{
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
			{
				String nick = ((TextView)view).getText().toString();
				EditText inputText = tabAdapter.getCurTab().getInputText();
				int start = Math.max(inputText.getSelectionStart(), 0);
				int end = Math.max(inputText.getSelectionEnd(), 0);
				nick += (start == 0 && end == 0)? ", " : " ";
				inputText.getText().replace(Math.min(start, end), Math.max(start, end), nick, 0, nick.length());
				return true;
			}
		});

		nickListAdapter = new NickListAdapter();
		nickList.setAdapter(nickListAdapter);
		DrawerLayout.LayoutParams Nparams = (DrawerLayout.LayoutParams) nickList.getLayoutParams();// android.support.v4.widget.
		Nparams.width = width;
		nickList.setLayoutParams(Nparams);

		DrawerLayout drawerLayout = (DrawerLayout)findViewById(R.id.drawerLayout);
		drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, nickList);

		mSavedInstanceState = savedInstanceState;
	}

	@Override
	protected void onDestroy()
	{
		Log.d("BananaPeel", "MainActivity destroyed");
		super.onDestroy();

		unbindService(conn);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);
		ViewPager pager = (ViewPager)findViewById(R.id.view_pager);
		if (pager != null)
		{
			savedInstanceState.putParcelable(((Integer)R.id.view_pager).toString(), pager.onSaveInstanceState());
		}

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
