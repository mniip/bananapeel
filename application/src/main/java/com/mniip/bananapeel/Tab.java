package com.mniip.bananapeel;

import java.util.ArrayList;

public class Tab
{
	private Server server;
	private IRCService service;
	private int id;
	private String title = "";
	private ArrayList<String> textLines = new ArrayList<>();

	public Tab(IRCService s, int tabId)
	{
		service = s;
		id = tabId;
	}

	public int getId()
	{
		return id;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String t)
	{
		title = t;
		IRCInterfaceListener listener = service.getListener();
		if(listener != null)
			listener.onTabTitleChanged(id);
	}

	public IRCService getService()
	{
		return service;
	}

	public Server getServer()
	{
		return server;
	}

	public void setServer(Server srv)
	{
		server = srv;
	}

	public ArrayList<String> getTextLines()
	{
		return textLines;
	}

	public void putLine(String line)
	{
		textLines.add(line);
		IRCInterfaceListener listener = service.getListener();
		if(listener != null)
			listener.onTabLinesAdded(id);
	}
}
