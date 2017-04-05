package com.mniip.bananapeel;

import java.util.Set;
import java.util.TreeSet;

public class NickListEntry
{
	public String nick;
	public Set<Character> status = new TreeSet<>();

	public NickListEntry()
	{
	}

	public NickListEntry(String n)
	{
		nick = n;
	}
}
