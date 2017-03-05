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
import android.util.SparseArray;

import java.util.ArrayList;

public class MainScreen extends FragmentActivity
{
	private TabAdapter tabAdapter;

	public TabAdapter getTabAdapter()
	{
		return tabAdapter;
	}

	private final IRCInterfaceListener ircInterfaceListener = new IRCInterfaceListener()
	{
		@Override
		public void onTabLinesAdded(int tabId)
		{
			getTabAdapter().onTabLinesAdded(tabId);
		}

		@Override
		public void onTabCleared(int tabId)
		{
			getTabAdapter().onTabCleared(tabId);
		}

		@Override
		public void onTabAdded(int tabId)
		{
			getTabAdapter().onTabAdded(tabId);
		}

		@Override
		public void onTabRemoved(int tabId)
		{
			getTabAdapter().onTabRemoved(tabId);
		}

		@Override
		public void onTabTitleChanged(int tabId)
		{
			getTabAdapter().onTabTitleChanged(tabId);
		}

		@Override
		public void onTabNicklistChanged(int tabId)
		{

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
		private SparseArray<Integer> tabPositions = new SparseArray<>();
		private ArrayList<Integer> tabIds = new ArrayList<>();
		private SparseArray<TabFragment> tabFragments = new SparseArray<>();

		public TabAdapter(FragmentManager manager)
		{
			super(manager);
		}

		public void setService(IRCService s)
		{
			service = s;
			tabPositions.clear();
			tabIds.clear();

			for(int i = 0; i < service.tabs.size(); i++)
			{
				tabPositions.put(service.tabs.keyAt(i), i);
				tabIds.add(i, service.tabs.keyAt(i));
			}
			notifyDataSetChanged();
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
				for(int i = 0; i < tabPositions.size(); i++)
					if(tabPositions.valueAt(i) >= pos)
						tabPositions.setValueAt(i, tabPositions.valueAt(i) + 1);
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
				for(int i = 0; i < tabPositions.size(); i++)
					if(tabPositions.valueAt(i) > tabPos)
						tabPositions.setValueAt(i, tabPositions.valueAt(i) - 1);
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

	@Override
    protected void onCreate(Bundle savedInstanceState)
    {
		Log.d("BananaPeel", "MainActivity created");
        super.onCreate(savedInstanceState);

		((ServiceApplication)getApplication()).ensureServiceStarted();
		Intent i = new Intent(this, IRCService.class);
		bindService(i, conn, BIND_IMPORTANT);

        setContentView(R.layout.main_screen);

		ViewPager pager = (ViewPager)findViewById(R.id.view_pager);
		tabAdapter = new TabAdapter(getSupportFragmentManager());
		pager.setAdapter(tabAdapter);
    }

	@Override
	protected void onDestroy()
	{
		Log.d("BananaPeel", "MainActivity destroyed");
		super.onDestroy();

		unbindService(conn);
	}
}
