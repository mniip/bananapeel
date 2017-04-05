package com.mniip.bananapeel;

import java.util.ArrayList;

public class Tab
{
	protected ServerTab serverTab;
	private IRCService service;
	private int id;
	private String title;
	public ArrayList<NickListEntry> nickList = new ArrayList<>();
	private ArrayList<String> textLines = new ArrayList<>();

	public Tab(IRCService s, ServerTab sTab, int tabId, String ttl)
	{
		service = s;
		serverTab = sTab;
		id = tabId;
		title = ttl;
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

	public ServerTab getServerTab()
	{
		return serverTab;
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
