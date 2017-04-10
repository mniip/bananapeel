package com.mniip.bananapeel.service;

import com.mniip.bananapeel.ui.IRCInterfaceListener;
import com.mniip.bananapeel.util.Collators;
import com.mniip.bananapeel.util.NickListEntry;
import com.mniip.bananapeel.util.OrderedList;

import java.util.ArrayList;

public class Tab
{
	protected ServerTab serverTab;
	private IRCService service;
	private int id;
	private String title;
	public OrderedList<NickListEntry> nickList = new OrderedList<>(NickListEntry.nickComparator(Collators.rfc1459()));
	private ArrayList<String> textLines = new ArrayList<>();

	public Tab(IRCService service, ServerTab serverTab, int id, String title)
	{
		this.service = service;
		this.serverTab = serverTab;
		this.id = id;
		this.title = title;
	}

	public int getId()
	{
		return id;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
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
