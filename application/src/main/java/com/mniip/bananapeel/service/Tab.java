package com.mniip.bananapeel.service;

import com.mniip.bananapeel.ui.IRCInterfaceListener;
import com.mniip.bananapeel.util.BiSet;
import com.mniip.bananapeel.util.NickListEntry;
import com.mniip.bananapeel.util.TextEvent;

import java.util.ArrayList;
import java.util.List;

public class Tab
{
	protected ServerTab serverTab;
	private IRCService service;
	private int id;
	private String title;
	public BiSet<NickListEntry> nickList;
	private List<TextEvent> textLines = new ArrayList<>();

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

	public List<TextEvent> getTextLines()
	{
		return textLines;
	}

	public void putLine(TextEvent event)
	{
		textLines.add(event);
		IRCInterfaceListener listener = service.getListener();
		if(listener != null)
			listener.onTabLinesAdded(id);
	}
}
