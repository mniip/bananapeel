package com.mniip.bananapeel.service;

public class ServerTab extends Tab
{
	public IRCServer server;

	public ServerTab(IRCService server, int tabId)
	{
		super(server, tabId, "");
	}
}
