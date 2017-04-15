package com.mniip.bananapeel.util;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class IRCServerConfig
{
	public List<Character> statusChars;
	public List<Character> statusModes;
	public Set<Character> listModes;
	public Set<Character> alwaysArgumentModes;
	public Set<Character> setArgumentModes;
	public Set<Character> noArgumentModes;
	public Set<Character> channelNames;
	public Collator nickCollator;
	public String networkName;
	public int modesPerLine;


	public static IRCServerConfig rfc1459()
	{
		IRCServerConfig config = new IRCServerConfig();
		config.statusChars = charList("@+");
		config.statusModes = charList("ov");
		config.listModes = new TreeSet<>(charList("beI"));
		config.alwaysArgumentModes = new TreeSet<>(charList("k"));
		config.setArgumentModes = new TreeSet<>(charList("flj"));
		config.noArgumentModes = new TreeSet<>(charList("imnst"));
		config.channelNames = new TreeSet<>(charList("#&"));
		config.nickCollator = Collators.rfc1459();
		config.modesPerLine = 3;
		return config;
	}

	public boolean isChannel(String str)
	{
		return !str.isEmpty() && channelNames.contains(str.charAt(0));
	}

	public static class Mode
	{
		private boolean set;
		private char mode;
		private Character status;
		private String argument;

		public boolean isSet()
		{
			return set;
		}

		public char getMode()
		{
			return mode;
		}

		public boolean isStatus()
		{
			return status != null;
		}

		public Character getStatus()
		{
			return status;
		}

		public String getArgument()
		{
			return argument;
		}
	}

	public List<Mode> parseModes(String mode, List<String> args)
	{
		List<Mode> modes = new ArrayList<>();

		int arg = 0;
		boolean set = true;
		for(int i = 0; i < mode.length(); i++)
		{
			char ch = mode.charAt(i);
			if(ch == '+')
				set = true;
			else if(ch == '-')
				set = false;
			else
			{
				Mode m = new Mode();
				m.set = set;
				m.mode = ch;
				if(statusModes.contains(ch) && arg < args.size())
				{
					m.status = statusChars.get(statusModes.indexOf(ch));
					m.argument = args.get(arg++);
				}
				else if((alwaysArgumentModes.contains(ch) || listModes.contains(ch) || (setArgumentModes.contains(ch) && set)) && arg < args.size())
					m.argument = args.get(arg++);
				modes.add(m);
			}
		}
		return modes;
	}

	private static List<Character> charList(String str)
	{
		List<Character> list = new ArrayList<>(str.length());
		for(int i = 0; i < str.length(); i++)
			list.add(str.charAt(i));
		return list;
	}

	public void parseISupport(List<String> data)
	{
		for(String entry : data)
		{
			int at = entry.indexOf('=');
			if(at == -1)
			{
			}
			else
			{
				String key = entry.substring(0, at);
				String value = entry.substring(at + 1);

				if(key.equalsIgnoreCase("CASEMAPPING"))
				{
					if(value.equalsIgnoreCase("ascii"))
						nickCollator = Collators.ascii();
					else if(value.equalsIgnoreCase("rfc1459"))
						nickCollator = Collators.rfc1459();
				}
				else if(key.equalsIgnoreCase("CHANMODES"))
				{
					String[] modes = key.split(",");
					if(modes.length >= 4)
					{
						listModes = new TreeSet<>(charList(modes[0]));
						alwaysArgumentModes = new TreeSet<>(charList(modes[1]));
						setArgumentModes = new TreeSet<>(charList(modes[2]));
						noArgumentModes = new TreeSet<>(charList(modes[3]));
					}
				}
				else if(key.equalsIgnoreCase("CHANTYPES"))
					channelNames = new TreeSet<>(charList(value));
				else if(key.equalsIgnoreCase("MODES"))
					try
					{
						modesPerLine = Integer.parseInt(value);
					}
					catch(NumberFormatException e) {}
				else if(key.equalsIgnoreCase("NETWORK"))
					networkName = value;
				else if(key.equals("PREFIX"))
				{
					if(value.length() > 0 && value.charAt(0) == '(')
					{
						int end = value.indexOf(')');
						if(end != -1 && end == value.length() - end)
						{
							statusModes = charList(value.substring(1, end));
							statusChars = charList(value.substring(end + 1));
						}
					}
				}
			}
		}
	}
}