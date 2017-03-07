package com.mniip.bananapeel;

public class ServerTab extends Tab
{
	public IRCServer server;

	public ServerTab(IRCService srv, int tabId)
	{
		super(srv, null, tabId, "");
		serverTab = this;
	}
}
