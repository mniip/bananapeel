package com.mniip.bananapeel.util;

import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import static com.mniip.bananapeel.util.TextEvent.Type.*;

public class TextEvent
{
	public enum Type
	{
		ERROR,
		RAW,
		NUMERIC,
		MESSAGE,
		OUR_MESSAGE,
		OUR_MSG,
		NOTICE,
		CHANNEL_NOTICE,
		OUR_NOTICE,
		JOIN,
		PART,
		PART_WITH_REASON,
		KICK,
		QUIT,
		QUIT_WITH_REASON,
		NICK_CHANGE,
		MODE_CHANGE,
		CTCP_ACTION,
		OUR_CTCP_ACTION,
		CTCP_PRIVATE,
		CTCP_CHANNEL,
		OUR_CTCP,
		CTCP_REPLY,
	}

	private final Date timestamp;
	private final Type type;
	private final String[] args;

	public TextEvent(Type type, String... args)
	{
		timestamp = new Date();
		this.type = type;
		this.args = args;
	}

	public TextEvent(Date timestamp, Type type, String... args)
	{
		this.timestamp = timestamp == null ? new Date() : timestamp;
		this.type = type;
		this.args = args;
	}

	public Date getTimestamp()
	{
		return timestamp;
	}

	public Type getType()
	{
		return type;
	}

	public String[] getArgs()
	{
		return args;
	}

	private static Editable.Factory editableFactory = new Editable.Factory();

	private static Editable formatColor(Spannable format, String... args)
	{
		Editable text = editableFactory.newEditable(format);
		for(int i = 0; i < text.length(); )
		{
			if(text.charAt(i) == '$' && i + 1 < text.length())
			{
				if(Character.isDigit(text.charAt(i + 1)))
				{
					int digit = Character.digit(text.charAt(i + 1), 10);
					Spannable arg = digit < args.length ? IRCFormatting.parse(args[digit]) : new SpannableString("");
					text.replace(i, i + 2, arg);
					i += arg.length();
					continue;
				}
				if(text.charAt(i + 1) == '$')
				{
					text.replace(i, i + 2, "$");
					i++;
					continue;
				}
			}
			i++;
		}
		return text;
	}

	private static final Map<Type, Spannable> templates = new TreeMap<>();

	static
	{
		templates.put(ERROR, IRCFormatting.parse("$0"));
		templates.put(RAW, IRCFormatting.parse("$0"));
		templates.put(NUMERIC, IRCFormatting.parse("\00304* ($0)\017 $1"));
		templates.put(MESSAGE, IRCFormatting.parse("\00310<$2$0>\017 $1"));
		templates.put(OUR_MESSAGE, IRCFormatting.parse("\00304<$2$0>\017 $1"));
		templates.put(OUR_MSG, IRCFormatting.parse("\00304>$0<\017 $1"));
		templates.put(NOTICE, IRCFormatting.parse("-\00310$0\017- $1"));
		templates.put(CHANNEL_NOTICE, IRCFormatting.parse("-\00310$0/\00306$1\017- $2"));
		templates.put(OUR_NOTICE, IRCFormatting.parse("-\00304>$0<\017- $1"));
		templates.put(JOIN, IRCFormatting.parse("\00303* \00310$0\00303 ($1) has joined \00306$2"));
		templates.put(PART, IRCFormatting.parse("\00305* \00310$0\00305 ($1) has left \00306$2"));
		templates.put(PART_WITH_REASON, IRCFormatting.parse("\00305* \00310$0\00305 ($1) has left \00306$2\00305 ($3)"));
		templates.put(KICK, IRCFormatting.parse("\00307* \00310$0\00307 has kicked \00304$1\00307 from \00306$2\00307 ($3)"));
		templates.put(QUIT, IRCFormatting.parse("\00304* \00310$0\00304 ($1) has quit"));
		templates.put(QUIT_WITH_REASON, IRCFormatting.parse("\00304* \00310$0\00304 ($1) has quit ($2)"));
		templates.put(NICK_CHANGE, IRCFormatting.parse("\00302* \00310$0\00302 has changed nick to \00310$1"));
		templates.put(MODE_CHANGE, IRCFormatting.parse("\00307* \00310$0\00307 has set mode \00306$1\017 $2"));
		templates.put(CTCP_ACTION, IRCFormatting.parse("* \00310$2$0\017 $1"));
		templates.put(OUR_CTCP_ACTION, IRCFormatting.parse("* \00304$2$0\017 $1"));
		templates.put(CTCP_PRIVATE, IRCFormatting.parse("* $0 requested CTCP $1 $2"));
		templates.put(CTCP_CHANNEL, IRCFormatting.parse("* $0 requested CTCP $1 $2"));
		templates.put(OUR_CTCP, IRCFormatting.parse("* Requested CTCP $2 from $1"));
		templates.put(CTCP_REPLY, IRCFormatting.parse("* CTCP reply $2 from $1"));
	}

	private static final DateFormat timestampFormatter = new SimpleDateFormat("[HH:mm:ss] ");

	public Spannable getText()
	{
		Editable text = formatColor(templates.get(type), args);
		text.insert(0, timestampFormatter.format(timestamp));
		return text;
	}
}
