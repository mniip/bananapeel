package com.mniip.bananapeel.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkFinder
{
	private static final String rDomain = "[_\\p{L}\\p{N}\\p{S}][-_\\p{L}\\p{N}\\p{S}]*(?:\\.[-_\\p{L}\\p{N}\\p{S}]+)*";
	private static final String rTLD = "\\.[\\p{L}][-\\p{L}\\p{N}}]*[\\p{L}]";
	private static final String rIPv4 = "[0-9]{1,3}(?:\\.[0-9]{1,3}){3}";
	private static final String rIPv6Group = "(?:[0-9a-f]{1,4})";
	private static final String rIPv6 = "(?:" + rIPv6Group + "?(?:(?::" + rIPv6Group + "){6}|(?::" + rIPv6Group + "){0,6}:(?::" + rIPv6Group + "){0,6}):" + rIPv6Group + "?)";
	private static final String rHostURL = "(?:" + rDomain + rTLD + "|" + rIPv4 + "|\\[" + rIPv6 + "\\])";
	private static final String rHostURLOptTLD = "(?:" + rDomain + "|" + rIPv4 + "|\\[" + rIPv6 + "\\])";
	private static final String rPort = "(?::[1-9][0-9]{0,4})";
	private static final String rOptPort = "(?:" + rPort + ")?";
	private static final String rScheme = "(?:http://|https://|irc://|ircs://|ftp://|sftp://|ftps://)";
	private static final String rPath = "(?:(?:\\([^()\\s]*\\)|[^()\\s]*)*(?<![.,?!:]))";
	private static final String rUserinfo = "(?:[-\\p{L}\\p{N}._~%]+(?::[-\\p{L}\\p{N}._~%]*)?)";
	private static final String rURL = "(?:" + rScheme + rUserinfo + "?" + rHostURLOptTLD + rOptPort + rPath + "?|(" + rHostURL + rOptPort + rPath + "?))";

	private static final Pattern pURL = Pattern.compile(rURL, Pattern.CASE_INSENSITIVE);

	public static class Link
	{
		public final int begin, end;
		public final boolean hasSchema;

		private Link(int begin, int end, boolean hasSchema)
		{
			this.begin = begin;
			this.end = end;
			this.hasSchema = hasSchema;
		}
	}

	static Iterable<Link> links(CharSequence sequence)
	{
		ArrayList<Link> list = new ArrayList<>();
		Matcher matcher = pURL.matcher(sequence);
		while(matcher.find())
			list.add(new Link(matcher.start(), matcher.end(), matcher.groupCount() == 0));
		return list;
	}
}
