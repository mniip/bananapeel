package com.mniip.bananapeel;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;

public class Collators
{
	private static final String RFC1459_RULES =
		"& A,a < B,b < C,c < D,d < E,e < F,f < G,g < H,h " +
		"< I,i < J,j < K,k < L,l < M,m < N,n < O,o < P,p " +
		"< Q,q < R,r < S,s < T,t < U,u < V,v < W,w < X,x " +
		"< Y,y < Z,z < '[','{' < '\\','|' < ']','}' < '^','~' < '_' < '-'";

	public static Collator rfc1459()
	{
		try
		{
			return new RuleBasedCollator(RFC1459_RULES);
		}
		catch(ParseException e)
		{
			throw new RuntimeException(e);
		}
	}
}