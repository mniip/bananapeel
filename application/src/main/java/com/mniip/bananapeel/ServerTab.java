package com.mniip.bananapeel;

public class ServerTab extends Tab
{
	public Server server;

	public ServerTab(IRCService srv, int tabId)
	{
		super(srv, null, tabId, "");
		serverTab = this;
	}
}
