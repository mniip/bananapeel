package com.mniip.bananapeel.ui;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup;

import com.mniip.bananapeel.R;
import com.mniip.bananapeel.service.IRCService;
import com.mniip.bananapeel.service.Tab;
import com.mniip.bananapeel.util.IntMap;

import java.util.ArrayList;

public class TabAdapter extends PagerAdapter
{
    private IRCService service;
    private MainScreen mainScreen;

    private TabFragment curTab;
    private IntMap<TabFragment> tabFragments = new IntMap<>();
    private ArrayList<Fragment.SavedState> savedStates = new ArrayList<>();

    private final FragmentManager fragmentManager;
    private FragmentTransaction curTransaction;

    public TabFragment getCurTab()
    {
        return curTab;
    }

    public TabAdapter(FragmentManager fm, MainScreen mScreen, IRCService service)
    {
        super();
        fragmentManager = fm;
        mainScreen = mScreen;
        setService(service);
    }

    public boolean isViewFromObject(View view, Object object)
    {
        if(object instanceof Fragment)
            return ((Fragment)object).getView() == view;
        else return object == view;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position)
    {
        if(service == null)
            return new View(mainScreen);
        int tabId = service.getTabByPosition(position).getId();
        TabFragment fragment = tabFragments.get(tabId);
        if(fragment == null)
        {
            Fragment.SavedState fss = savedStates.get(position);
            fragment = new TabFragment();
            fragment.setTabId(tabId);
            fragment.setInitialSavedState(fss);
            if(curTransaction == null)
                curTransaction = fragmentManager.beginTransaction();
            curTransaction.add(container.getId(), fragment);
            tabFragments.put(tabId, fragment);
        }
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object)
    {
        if(object instanceof Fragment)
        {
            TabFragment fragment = (TabFragment)object;
            if(curTransaction == null)
                curTransaction = fragmentManager.beginTransaction();
            if(fragment.isActive())
                savedStates.set(position, fragment.isAdded() ? fragmentManager.saveFragmentInstanceState(fragment) : null);
            curTransaction.remove(fragment);
            tabFragments.remove(fragment.getTabId());
        }
    }

    @Override
    public int getItemPosition(Object object)
    {
        if(object instanceof Fragment)
        {
            Integer pos = service.getPosById(((TabFragment)object).getTabId());
            return pos == null ? POSITION_NONE : pos;
        }
        return service == null ? POSITION_UNCHANGED : POSITION_NONE;
    }

    @Override
    public void finishUpdate(ViewGroup container)
    {
        if(curTransaction != null)
        {
            curTransaction.commitNowAllowingStateLoss();
            curTransaction = null;
        }
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object)
    {
        if(service != null)
        {
            int id = service.getTabByPosition(position).getId();
            service.setFrontTab(id);

            View nickList = (View)mainScreen.findViewById(R.id.nick_list);
            DrawerLayout drawerLayout = (DrawerLayout)mainScreen.findViewById(R.id.drawerLayout);
            if (service.getFrontTab().nickList == null)
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, nickList);
            else if (drawerLayout.getDrawerLockMode(nickList) == DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, nickList);
            mainScreen.getNickListAdapter().notifyDataSetChanged();
        }
        curTab = (TabFragment)object;
    }

    public void setService(IRCService service)
    {
        this.service = service;

        for(Tab tab : service.getTabs())
            onTabAdded(tab.getId());
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
        Tab tab = service.getTabById(tabId);
        savedStates.add(service.getPosById(tabId), null);
        notifyDataSetChanged();
    }

    public void onTabRemoved(int tabId)
    {
        TabFragment fragment = tabFragments.get(tabId);
        if(fragment != null)
        {
            fragment.setInactive();
            savedStates.remove((int)service.getPosById(tabId));
            notifyDataSetChanged();
        }
    }

    public void onTabTitleChanged(int tabId)
    {
        notifyDataSetChanged();
    }

    @Override
    public String getPageTitle(int position)
    {
        if(service == null)
            return mainScreen.getText(R.string.app_name).toString();
        Tab tab = service.getTabByPosition(position);
        if(tab != null)
            return tab.getTitle();
        else
            return "";
    }

    @Override
    public int getCount()
    {
        if(service == null)
            return 1;
        else
            return service.getTabsCount();
    }

    @Override
    public Parcelable saveState()
    {
        for(IntMap.KV<TabFragment> kv : tabFragments.pairs())
        {
            TabFragment fragment = kv.getValue();
            if(curTransaction == null)
                curTransaction = fragmentManager.beginTransaction();
            savedStates.set(getItemPosition(fragment), fragment.isAdded() ? fragmentManager.saveFragmentInstanceState(fragment) : null);
            curTransaction.remove(fragment);
            tabFragments.remove(fragment.getTabId());
        }
        Bundle state = new Bundle();
        Fragment.SavedState[] fss = new Fragment.SavedState[savedStates.size()];
        savedStates.toArray(fss);
        state.putParcelableArray("states", fss);
        return state;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader)
    {
        if(state != null)
        {
            Bundle bundle = (Bundle)state;
            bundle.setClassLoader(loader);
            Parcelable[] fss = bundle.getParcelableArray("states");
            savedStates.clear();
            tabFragments.clear();
            if(fss != null)
            {
                for(int i = 0; i < fss.length; i++)
                {
                    savedStates.add((Fragment.SavedState)fss[i]);
                }
            }
        }
    }

}
