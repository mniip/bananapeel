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
		JOIN,
		PART,
		PART_WITH_REASON,
		QUIT,
		QUIT_WITH_REASON,
		NICK_CHANGE,
		MODE_CHANGE,
		CTCP_ACTION,
		OUR_CTCP_ACTION,
		CTCP_PRIVATE,
		CTCP_CHANNEL
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
		templates.put(NUMERIC, IRCFormatting.parse("\00313* ($0)\017 $1"));
		templates.put(MESSAGE, IRCFormatting.parse("\00311<$2$0>\017 $1"));
		templates.put(OUR_MESSAGE, IRCFormatting.parse("\00313<$2$0>\017 $1"));
		templates.put(JOIN, IRCFormatting.parse("\00308* $0 ($1) has joined \00312$2"));
		templates.put(PART, IRCFormatting.parse("\00311* $0 ($1) has left \00312$2"));
		templates.put(PART_WITH_REASON, IRCFormatting.parse("\00311* $0 ($1) has left \00312$2\00311 ($3)"));
		templates.put(QUIT, IRCFormatting.parse("\00312* $0 ($1) has quit"));
		templates.put(QUIT_WITH_REASON, IRCFormatting.parse("\00312* $0 ($1) has quit ($2)"));
		templates.put(NICK_CHANGE, IRCFormatting.parse("* \00311$0\017 has changed nick to \00311$1\017"));
		templates.put(MODE_CHANGE, IRCFormatting.parse("* \00311$0\017 has set mode \00312$1\017 $2"));
		templates.put(CTCP_ACTION, IRCFormatting.parse("* \00311$2$0\017 $1"));
		templates.put(OUR_CTCP_ACTION, IRCFormatting.parse("* \00313$2$0\017 $1"));
		templates.put(CTCP_PRIVATE, IRCFormatting.parse("* $0 requested CTCP $1 $2"));
		templates.put(CTCP_CHANNEL, IRCFormatting.parse("* $0 requested CTCP $1 $2"));
	}

	private static final DateFormat timestampFormatter = new SimpleDateFormat("[HH:mm:ss] ");

	public Spannable getText()
	{
		Editable text = formatColor(templates.get(type), args);
		text.insert(0, timestampFormatter.format(timestamp));
		return text;
	}
}
