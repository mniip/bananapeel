package com.mniip.bananapeel;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class NickListEntry
{
	public String nick;
	public Set<Character> status = new TreeSet<>();
	public Character highestStatus = null;

	public NickListEntry()
	{
	}

	public NickListEntry(String n)
	{
		nick = n;
	}

	public void updateStatus(IRCServer server)
	{
		highestStatus = null;
		for(Character stat : server.statusChars)
			if(status.contains(stat))
			{
				highestStatus = stat;
				break;
			}
	}

	public static Comparator<NickListEntry> nickComparator(final Comparator<? super String> comparator)
	{
		return new Comparator<NickListEntry>()
		{
			@Override
			public int compare(NickListEntry a, NickListEntry b)
			{
				return comparator.compare(a.nick, b.nick);
			}
		};
	}

	public static Comparator<NickListEntry> statusComparator(final List<Character> statusChars)
	{
		return new Comparator<NickListEntry>()
		{
			@Override
			public int compare(NickListEntry a, NickListEntry b)
			{
				if(a.highestStatus == null)
					if(b.highestStatus == null)
						return 0;
					else
						return 1;
				if(b.highestStatus == null)
					return -1;
				return statusChars.indexOf(a.highestStatus) - statusChars.indexOf(b.highestStatus);
			}
		};
	}
}
