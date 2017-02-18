package com.mniip.bananapeel;

public interface IRCInterfaceListener
{
	void onTabLinesAdded(int tabId);
	void onTabCleared(int tabId);
	void onTabAdded(int tabId);
	void onTabRemoved(int tabId);
	void onTabTitleChanged(int tabId);
	void onTabNicklistChanged(int tabId);
}
