package com.mniip.bananapeel.service;

import com.mniip.bananapeel.ui.IRCInterfaceListener;
import com.mniip.bananapeel.util.BiSet;
import com.mniip.bananapeel.util.NickListEntry;
import com.mniip.bananapeel.util.TextEvent;

import java.util.ArrayList;
import java.util.List;

public class Tab
{
	public enum Type
	{
		SERVER,
		CHANNEL,
		QUERY
	}

	public final ServerTab serverTab;
	public final IRCService service;
	public final int id;
	public final Type type;
	private String title;
	public BiSet<NickListEntry> nickList;
	private List<TextEvent> textLines = new ArrayList<>();

	public Tab(IRCService service, ServerTab serverTab, int id, Type type, String title)
	{
		this.service = service;
		this.serverTab = serverTab;
		this.id = id;
		this.type = type;
		this.title = title;
	}

	protected Tab(IRCService service, int id, String title)
	{
		this.service = service;
		this.serverTab = (ServerTab)this;
		this.id = id;
		this.type = Type.SERVER;
		this.title = title;
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
