package com.mniip.bananapeel.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class IRCMessage
{
	static public class Source
	{
		public String text;

		public Source(String text)
		{
			this.text = text;
		}

		public String getNick()
		{
			int excl = text.indexOf('!');
			if(excl == -1)
				return text;
			return text.substring(0, excl);
		}

		public String getUser()
		{
			int excl = text.indexOf('!');
			if(excl == -1)
				return null;
			int at = text.indexOf('@');
			if(at == -1)
				return text.substring(excl);
			return text.substring(excl + 1, at);
		}

		public String getHost()
		{
			int at = text.indexOf('@');
			if(at == -1)
				return null;
			return text.substring(at + 1);
		}

		public String getUserHost()
		{
			int at = text.indexOf('!');
			if(at == -1)
				return null;
			return text.substring(at + 1);
		}
	}

	public Map<String, String> tags;
	public Source source;
	public String command;
	public String[] args;
	public String rawString;

	public String getNick()
	{
		return source == null ? null : source.getNick();
	}

	public String getUser()
	{
		return source == null ? null : source.getUser();
	}

	public String getHost()
	{
		return source == null ? null : source.getHost();
	}

	public String getUserHost()
	{
		return source == null ? null : source.getUserHost();
	}

	public IRCMessage()
	{
	}

	public IRCMessage(Map<String, String> tags, Source source, String command, String... args)
	{
		this.tags = tags;
		this.source = source;
		this.command = command;
		this.args = args;
	}

	public IRCMessage(Map<String, String> tags, String command, String... args)
	{
		this(tags, null, command, args);
	}

	public IRCMessage(Source source, String command, String... args)
	{
		this(null, source, command, args);
	}

	public IRCMessage(String command, String... args)
	{
		this(null, null, command, args);
	}

	private static String tagEscape(String value)
	{
		return value.replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r").replaceAll(" ", "\\\\s").replaceAll(";", "\\\\:");
	}

	private static String tagUnescape(String value)
	{
		return value.replaceAll("\\\\:", ";").replaceAll("\\\\s", " ").replaceAll("\\\\r", "\r").replaceAll("\\\\n", "\n").replaceAll("\\\\\\\\", "\\\\\\");
	}

	public String toIRC()
	{
		if(rawString != null)
			return rawString;

		StringBuilder b = new StringBuilder();

		if(tags != null)
		{
			b.append('@');
			boolean first = true;
			for(Map.Entry<String, String> tag : tags.entrySet())
			{
				if(!first)
					b.append(';');
				first = false;
				b.append(tag.getKey());
				if(tag.getValue() != null)
					b.append('=').append(tagEscape(tag.getValue()));
			}
			b.append(' ');
		}

		if(source != null)
			b.append(':').append(source.text).append(' ');
		b.append(command);

		for(int i = 0; i < args.length; i++)
			if(i == args.length - 1 && args[i].indexOf(' ') != -1)
				b.append(" :").append(args[i]);
			else
				b.append(' ').append(args[i]);

		return b.toString();
	}

	public static IRCMessage fromIRC(String origStr)
	{
		origStr = origStr.replace("\n", "").replace("\r", "");
		String str = origStr;

		Map<String, String> tags = null;
		if(str.length() > 0 && str.charAt(0) == '@')
		{
			tags = new TreeMap<>();
			int idx = str.indexOf(' ');
			String tagData;
			if(idx == -1)
			{
				tagData = str.substring(1);
				str = "";
			}
			else
			{
				tagData = str.substring(1, idx);
				str = str.substring(idx + 1);
			}
			for(String tag : tagData.split(";"))
			{
				int at = tag.indexOf('=');
				if(at == -1)
					tags.put(tag, null);
				else
					tags.put(tag.substring(0, at), tagUnescape(tag.substring(at + 1)));
			}
		}

		Source src = null;
		if(str.length() > 0 && str.charAt(0) == ':')
		{
			int idx = str.indexOf(' ');
			if(idx == -1)
			{
				src = new Source(str.substring(1));
				str = "";
			}
			else
			{
				src = new Source(str.substring(1, idx));
				str = str.substring(idx + 1);
			}
		}

		int idx = str.indexOf(' ');
		String cmd;
		if(idx == -1)
		{
			cmd = str;
			str = "";
		}
		else
		{
			cmd = str.substring(0, idx);
			str = str.substring(idx + 1);
		}

		ArrayList<String> args = new ArrayList<>();
		while(str.length() != 0)
		{
			if(str.length() > 0 && str.charAt(0) == ':')
			{
				args.add(str.substring(1));
				break;
			}
			idx = str.indexOf(' ');
			if(idx == -1)
			{
				args.add(str);
				str = "";
			}
			else
			{
				args.add(str.substring(0, idx));
				str = str.substring(idx + 1);
			}
		}

		IRCMessage msg = new IRCMessage();
		msg.tags = tags;
		msg.source = src;
		msg.command = cmd;
		msg.args = args.toArray(new String[args.size()]);
		msg.rawString = origStr;
		return msg;
	}
}
