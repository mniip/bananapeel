package com.mniip.bananapeel.service;

import android.text.Editable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.mniip.bananapeel.ui.IRCInterfaceListener;
import com.mniip.bananapeel.util.BiSet;
import com.mniip.bananapeel.util.IRCFormatting;
import com.mniip.bananapeel.util.NickListEntry;
import com.mniip.bananapeel.util.TextEvent;

import java.util.ArrayList;
import java.util.List;

import static com.mniip.bananapeel.service.Tab.ActivityStatus.*;

public class Tab
{
	public enum Type
	{
		SERVER,
		CHANNEL,
		QUERY
	}

	public enum ActivityStatus
	{
		NONE,
		ACTIVITY,
		MESSAGE,
		HIGHLIGHT
	}

	public final ServerTab serverTab;
	public final IRCService service;
	public final int id;
	public final Type type;
	private ActivityStatus activityStatus;
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

	private static Editable.Factory editableFactory = new Editable.Factory();

	public Editable getTitleColored()
	{
		Editable editable = editableFactory.newEditable(title);
		if(activityStatus == HIGHLIGHT)
			editable.setSpan(new ForegroundColorSpan(IRCFormatting.colors[9]), 0, editable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		else if(activityStatus == MESSAGE)
			editable.setSpan(new ForegroundColorSpan(IRCFormatting.colors[7]), 0, editable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		else if(activityStatus == ACTIVITY)
			editable.setSpan(new ForegroundColorSpan(IRCFormatting.colors[11]), 0, editable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		return editable;
	}

	public List<TextEvent> getTextLines()
	{
		return textLines;
	}

	public ActivityStatus getActivityStatus()
	{
		return activityStatus;
	}

	public void setActivityStatus(ActivityStatus activityStatus)
	{
		if(this.activityStatus != activityStatus)
		{
			this.activityStatus = activityStatus;

			IRCInterfaceListener listener = service.getListener();
			if(listener != null)
				listener.onTabTitleChanged(id);
		}
	}

	public void putLine(TextEvent event)
	{
		textLines.add(event);

		if(service.getFrontTab() != this)
		{
			if(event.isHighlight())
				setActivityStatus(HIGHLIGHT);
			else if(event.isMessage() && activityStatus != HIGHLIGHT)
				setActivityStatus(MESSAGE);
			else if(activityStatus != HIGHLIGHT && activityStatus != MESSAGE)
				setActivityStatus(ACTIVITY);
		}

		IRCInterfaceListener listener = service.getListener();
		if(listener != null)
			listener.onTabLinesAdded(id);
	}
}
