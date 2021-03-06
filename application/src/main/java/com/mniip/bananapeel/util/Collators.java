package com.mniip.bananapeel.util;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;

public class Collators
{
	private static final String RFC1459_RULES =
		"& 0 < 1 < 2 < 3 < 4 < 5 < 6 < 7 < 8 < 9" +
		"< A,a < B,b < C,c < D,d < E,e < F,f < G,g < H,h " +
		"< I,i < J,j < K,k < L,l < M,m < N,n < O,o < P,p " +
		"< Q,q < R,r < S,s < T,t < U,u < V,v < W,w < X,x " +
		"< Y,y < Z,z < '[','{' < '\\','|' < ']','}' < '^','~' < '_' < '-'";

	private static final String ASCII_RULES =
		"& 0 < 1 < 2 < 3 < 4 < 5 < 6 < 7 < 8 < 9" +
		"< A,a < B,b < C,c < D,d < E,e < F,f < G,g < H,h " +
		"< I,i < J,j < K,k < L,l < M,m < N,n < O,o < P,p " +
		"< Q,q < R,r < S,s < T,t < U,u < V,v < W,w < X,x " +
		"< Y,y < Z,z < '_' < '-'";

	public static Collator rfc1459()
	{
		try
		{
			RuleBasedCollator collator = new RuleBasedCollator(RFC1459_RULES);
			collator.setStrength(Collator.SECONDARY);
			return collator;
		}
		catch(ParseException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static Collator ascii()
	{
		try
		{
			RuleBasedCollator collator = new RuleBasedCollator(ASCII_RULES);
			collator.setStrength(Collator.SECONDARY);
			return collator;
		}
		catch(ParseException e)
		{
			throw new RuntimeException(e);
		}
	}
}