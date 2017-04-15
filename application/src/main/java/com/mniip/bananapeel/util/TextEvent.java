package com.mniip.bananapeel.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TextEvent
{
	public static final int ERROR = 0;
	public static final int RAW = 1;
	public static final int NUMERIC = 2;
	public static final int MESSAGE = 3;
	public static final int JOIN = 4;
	public static final int PART = 5;
	public static final int PART_WITH_REASON = 6;
	public static final int QUIT = 7;
	public static final int QUIT_WITH_REASON = 8;
	public static final int NICK_CHANGE = 9;
	public static final int MODE_CHANGE = 10;

	private Date timestamp;
	private int type;
	private String[] args;

	public TextEvent(int type, String... args)
	{
		timestamp = new Date();
		this.type = type;
		this.args = new String[4];
		for(int i = 0; i < 4; i++)
			this.args[i] = i < args.length ? args[i] : "";
	}

	public TextEvent(Date timestamp, int type, String... args)
	{
		this.timestamp = timestamp;
		this.type = type;
		this.args = new String[4];
		for(int i = 0; i < 4; i++)
			this.args[i] = i < args.length & args[i] != null ? args[i] : "";
	}

	public Date getTimestamp()
	{
		return timestamp;
	}

	public int getType()
	{
		return type;
	}

	public String[] getArgs()
	{
		return args;
	}

	private static final IntMap<String> templates = initializeTemplates();

	private static IntMap<String> initializeTemplates()
	{
		IntMap<String> templates = new IntMap<>();
		templates.put(ERROR, "%1$s");

		templates.put(RAW, "%1$s");
		templates.put(NUMERIC, "* (%1$s) %2$s");

		templates.put(MESSAGE, "<%3$s%1$s> %2$s");
		templates.put(JOIN, "* %1$s (%2$s) has joined %3$s");
		templates.put(PART, "* %1$s (%2$s) has left %3$s");
		templates.put(PART_WITH_REASON, "* %1$s (%2$s) has left %3$s (%4$s)");
		templates.put(QUIT, "* %1$s (%2$s) has quit");
		templates.put(QUIT_WITH_REASON, "* %1$s (%2$s) has quit (%3$s)");
		templates.put(NICK_CHANGE, "* %1$s has changed nick to %2$s");
		templates.put(MODE_CHANGE, "* %1$s has set mode %2$s %3$s");
		return templates;
	}

	private static final DateFormat timestampFormatter = new SimpleDateFormat("[HH:mm:ss] ");

	public String getText()
	{
		return timestampFormatter.format(timestamp) + String.format(templates.get(type), (Object[])args);
	}
}
