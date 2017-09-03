package com.mniip.bananapeel.ui;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
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

import java.util.ArrayList;
import java.util.Set;

public class MainScreen extends FragmentActivity
{
	private TabAdapter tabAdapter;
	private NickListAdapter nickListAdapter;
	private ChannelListAdapter channelListAdapter;

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
			channelListAdapter.onChannelListChanged();
		}

		@Override
		public void onTabRemoved(int tabId)
		{
			tabAdapter.onTabRemoved(tabId);
			channelListAdapter.onChannelListChanged();
		}

		@Override
		public void onTabTitleChanged(int tabId)
		{
			tabAdapter.onTabTitleChanged(tabId);
			channelListAdapter.onChannelListChanged();
		}

		@Override
		public void onTabNickListChanged(int tabId)
		{
			nickListAdapter.onTabNickListChanged(tabId);
		}
	};

	public IRCService getService()
	{
		return ServiceApplication.getService();
	}

	Bundle restoreState = null;
	private boolean serviceConnected = false;
	private final ServiceConnection conn = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName cls, IBinder binder)
		{
			IRCService service = getService();
			service.setListener(ircInterfaceListener);

			ViewGroup group = (ViewGroup)findViewById(R.id.pager_container);
			LayoutInflater.from(MainScreen.this).inflate(R.layout.pager, group, true);

			final ViewPager pager = (ViewPager)group.findViewById(R.id.view_pager);
			tabAdapter = new TabAdapter(getSupportFragmentManager(), MainScreen.this, service);
			pager.setAdapter(tabAdapter);

			ListView channelList = (ListView)findViewById(R.id.channel_list);
			channelListAdapter = new ChannelListAdapter();
			channelList.setAdapter(channelListAdapter);

			channelList.setOnItemClickListener(new AdapterView.OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id)
				{
					pager.setCurrentItem(position, false);
				}
			});

			if(service.getFrontTab().nickList != null)
			{
				View nickList = findViewById(R.id.nick_list);
				DrawerLayout drawerLayout = (DrawerLayout)findViewById(R.id.drawerLayout);
				drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, nickList);
			}

			serviceConnected = true;
			if(restoreState != null)
			{
				MainScreen.super.onRestoreInstanceState(restoreState);
				restoreState = null;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName cls)
		{
			getService().unsetListener(ircInterfaceListener);

			serviceConnected = false;
		}
	};

	public class ChannelListAdapter extends BaseAdapter
	{
		@Override
		public int getCount()
		{
			IRCService service = getService();
			if(service == null || service.getTabs() == null)
				return 0;
			return service.getTabsCount();
		}

		@Override
		public Object getItem(int position)
		{
			return getService().getTabByPosition(position);
		}

		@Override
		public long getItemId(int position)
		{
			return getService().getTabByPosition(position).id;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if(convertView == null)
				convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.channel_line, parent, false);

			((TextView)convertView).setText(getService().getTabByPosition(position).getTitle());
			return convertView;
		}

		public void onChannelListChanged()
		{
			notifyDataSetChanged();
		}
	}

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
			if(getService().getFrontTab().id == tabId)
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

	private static class CmdPair
	{
		public final String descr;
		public final String cmd;

		public CmdPair(String descr, String cmd)
		{
			this.descr = descr;
			this.cmd = cmd;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.d("BananaPeel", "MainActivity created");
		super.onCreate(savedInstanceState);

		((ServiceApplication)getApplication()).ensureServiceStarted();
		Intent i = new Intent(this, IRCService.class);

		int bindType = BIND_AUTO_CREATE;
		if(android.os.Build.VERSION.SDK_INT >= 14)
			try
			{
				bindType |= MainScreen.class.getField("BIND_IMPORTANT").getInt(null);
			}
			catch(NoSuchFieldException e) {}
			catch(IllegalAccessException e) {}
		bindService(i, conn, bindType);

		setContentView(R.layout.main_screen);

		int width = getResources().getDisplayMetrics().widthPixels/2;

		ListView channelList = (ListView)findViewById(R.id.channel_list);
		DrawerLayout.LayoutParams sParams = (DrawerLayout.LayoutParams) channelList.getLayoutParams();// android.support.v4.widget.
		sParams.width = width;
		channelList.setLayoutParams(sParams);

		final ListView nickList = (ListView)findViewById(R.id.nick_list);

		nickList.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				final String nick = getService().getFrontTab().nickList.getSecondary(position).nick;
				AlertDialog.Builder builder = new AlertDialog.Builder(MainScreen.this);

				final ArrayList<CmdPair> actions = new ArrayList<CmdPair>();
				actions.add(new CmdPair("Private message", "/query"));
				actions.add(new CmdPair("WhoIs", "/whois"));
				actions.add(new CmdPair("Kick", "/kick"));
				actions.add(new CmdPair("Ping", "/ping"));
				actions.add(new CmdPair("Give op", "/op"));
				actions.add(new CmdPair("Give voice", "/voice"));
				actions.add(new CmdPair("Take op", "/deop"));
				actions.add(new CmdPair("Take voice","/devoice"));
				actions.add(new CmdPair("Ban","/ban"));
				actions.add(new CmdPair("Unban","/unban"));

				builder.setTitle("Action list");
				String descriptions[] = new String[actions.size()];
				for (int i = 0; i < actions.size(); ++i)
					descriptions[i] = actions.get(i).descr;

				builder.setItems(descriptions, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if (which < actions.size())
						{
							int curTabId = tabAdapter.getCurTab().getTabId();
							getService().onTextEntered(curTabId, actions.get(which).cmd + ' ' + nick);
						}
					}
				});
				builder.show();
			}
		});

		nickList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
		{
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
			{
				String nick = getService().getFrontTab().nickList.getSecondary(position).nick;
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
	}

	@Override
	protected void onDestroy()
	{
		Log.d("BananaPeel", "MainActivity destroyed");
		super.onDestroy();

		unbindService(conn);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState)
	{
		if(serviceConnected)
		{
			restoreState = null;
			super.onRestoreInstanceState(savedInstanceState);
		}
		else
			restoreState = savedInstanceState;
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
