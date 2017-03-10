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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainScreen extends FragmentActivity
{
	private TabAdapter tabAdapter;
	private NickListAdapter nickListAdapter;

	public TabAdapter getTabAdapter()
	{
		return tabAdapter;
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
			tabAdapter.setService(getService());
		}

		@Override
		public void onServiceDisconnected(ComponentName cls)
		{
			getService().unsetListener(ircInterfaceListener);
		}
	};

	public class TabAdapter extends FragmentStatePagerAdapter
	{
		private IRCService service;
		private IntMap<Integer> tabPositions = new IntMap<>();
		private ArrayList<Integer> tabIds = new ArrayList<>();
		private IntMap<TabFragment> tabFragments = new IntMap<>();

		public TabAdapter(FragmentManager manager)
		{
			super(manager);
		}

		public void setService(IRCService s)
		{
			service = s;
			tabPositions.clear();
			tabIds.clear();

			for(IntMap.KV<Tab> kv : service.tabs.pairs())
				onTabAdded(kv.getKey());
		}

		public void onTabViewCreated(TabFragment view, int tabNumber)
		{
			tabFragments.put(tabNumber, view);
		}

		public void onTabViewDestroyed(int tabNumber)
		{
			tabFragments.delete(tabNumber);
		}

		public void onTabLinesAdded(int tabId)
		{
			TabFragment fragment = tabFragments.get(tabId);
			if(fragment != null)
				fragment.onLinesAdded();
		}

		public void onTabCleared(int tabId)
		{
			TabFragment fragment = tabFragments.get(tabId);
			if(fragment != null)
				fragment.onCleared();
		}

		public void onTabAdded(int tabId)
		{
			Tab tab = service.tabs.get(tabId);
			if(tab != null)
			{
				int pos = 0;
				for(int p = 0; p < tabIds.size(); p++)
				{
					Tab t = service.tabs.get(tabIds.get(p));
					if(tab.getServerTab() == t)
						pos = p + 1;
					if(t != null && t.getTitle().compareToIgnoreCase(tab.getTitle()) < 0)
						pos = p + 1;
				}
				tabIds.add(pos, tabId);
				for(IntMap.KV<Integer> kv : tabPositions.pairs())
					if(kv.getValue() >= pos)
						kv.setValue(kv.getValue() + 1);
				tabPositions.put(tabId, pos);
				notifyDataSetChanged();
			}
		}

		public void onTabRemoved(int tabId)
		{
			Integer tabPos = tabPositions.get(tabId);
			if (tabPos != null)
			{
				tabIds.remove(tabPos);
				tabPositions.delete(tabId);
				for(IntMap.KV<Integer> kv : tabPositions.pairs())
					if(kv.getValue() > tabPos)
						kv.setValue(kv.getValue() - 1);
				notifyDataSetChanged();
			}
		}

		public void onTabTitleChanged(int tabId)
		{
			notifyDataSetChanged();
		}

		@Override
		public Fragment getItem(int position)
		{
			Integer tabId = tabIds.get(position);
			TabFragment fragment = new TabFragment();
			if (tabId == null)
				tabId = -1;
			fragment.setId(tabId);
			return fragment;
		}

		@Override
		public String getPageTitle(int position)
		{
			Integer tabId = tabIds.get(position);
			if (tabId != null)
				return service.tabs.get(tabId).getTitle();
			else return "";
		}

		@Override
		public int getCount()
		{
			return tabIds.size();
		}
	}

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
		public View getView(int position, View reuse, ViewGroup parent)
		{
			TextView view = (TextView)LayoutInflater.from(parent.getContext()).inflate(R.layout.nick_line, parent, false);
			view.setText(getService().getFrontTab().nickList.get(position));
			return view;
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

		ListView nickList = (ListView)findViewById(R.id.nick_list);
		nickListAdapter = new NickListAdapter();
		nickList.setAdapter(nickListAdapter);

		ViewPager pager = (ViewPager)findViewById(R.id.view_pager);
		tabAdapter = new TabAdapter(getSupportFragmentManager());
		pager.setAdapter(tabAdapter);
		pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
		{
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
			@Override
			public void onPageScrollStateChanged(int state)	{ }
			@Override
			public void onPageSelected(int position)
			{
				Integer id = tabAdapter.tabIds.get(position);
				if(id != null)
					getService().setFrontTab(id);
				nickListAdapter.notifyDataSetChanged();
			}
		});
    }

	@Override
	protected void onDestroy()
	{
		Log.d("BananaPeel", "MainActivity destroyed");
		super.onDestroy();

		unbindService(conn);
	}
}
