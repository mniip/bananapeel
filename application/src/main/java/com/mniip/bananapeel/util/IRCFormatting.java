package com.mniip.bananapeel.util;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.text.style.UpdateAppearance;

public class IRCFormatting
{
	private static final int[] colors =
		{
			0xFFFFFFFF, 0xFF000000, 0xFF0000CC, 0xFF00CC00, 0xFFCC0000, 0xFFCCCC00, 0xFFCC00CC, 0xFFCC6600,
			0xFFFFFF00, 0xFF00FF00, 0xFF00CCCC, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFF888888, 0xFFCCCCCC
		};

	private static int getColor(int color)
	{
		if(color >= 0 && color < colors.length)
			return colors[color];
		return colors[15];
	}

	public static class Color extends CharacterStyle implements UpdateAppearance
	{
		public Integer fgColor, bgColor;

		public Color(Integer fgColor, Integer bgColor)
		{
			this.fgColor = fgColor;
			this.bgColor = bgColor;
		}

		@Override
		public void updateDrawState(TextPaint ds)
		{
			if(fgColor != null)
				ds.setColor(getColor(fgColor));
			if(bgColor != null)
				ds.bgColor = getColor(bgColor);
		}
	}

	public static class Inverse extends CharacterStyle implements UpdateAppearance
	{
		@Override
		public void updateDrawState(TextPaint ds)
		{
			int tmp = ds.getColor();
			ds.setColor(ds.bgColor);
			ds.bgColor = tmp;
		}
	}

	public static class Bold extends StyleSpan
	{
		public Bold()
		{
			super(Typeface.BOLD);
		}
	}

	public static class Italics extends StyleSpan
	{
		public Italics()
		{
			super(Typeface.ITALIC);
		}
	}

	public static class Underline extends UnderlineSpan
	{
	}

	public static Spannable parse(String text)
	{
		SpannableStringBuilder builder = new SpannableStringBuilder();
		Integer colorBegin = null, inverseBegin = null, boldBegin = null, italicsBegin = null, underlineBegin = null;
		int fgColor = 0;
		Integer bgColor = null;
		for(int i = 0; i < text.length(); i++)
		{
			char ch = text.charAt(i);
			if(ch == 0x02)
			{
				if(boldBegin == null)
					boldBegin = builder.length();
				else
				{
					builder.setSpan(new Bold(), boldBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					boldBegin = null;
				}
			}
			else if(ch == 0x03)
			{
				if(colorBegin != null)
				{
					builder.setSpan(new Color(fgColor, bgColor), colorBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					colorBegin = null;
				}
				if(inverseBegin != null)
				{
					builder.setSpan(new Inverse(), inverseBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					inverseBegin = null;
				}
				if(i + 1 < text.length() && Character.isDigit(text.charAt(i + 1)))
				{
					i++;
					fgColor = Character.digit(text.charAt(i), 10);
					if(i + 1 < text.length() && Character.isDigit(text.charAt(i + 1)))
					{
						i++;
						fgColor = fgColor * 10 + Character.digit(text.charAt(i), 10);
					}
					if(i + 2 < text.length() && text.charAt(i + 1) == ',' && Character.isDigit(text.charAt(i + 2)))
					{
						i += 2;
						bgColor = Character.digit(text.charAt(i), 10);
						if(i + 1 < text.length() && Character.isDigit(text.charAt(i + 1)))
						{
							i++;
							bgColor = bgColor * 10 + Character.digit(text.charAt(i), 10);
						}
					}
					colorBegin = builder.length();
				}
				else
				{
					if(boldBegin != null)
					{
						builder.setSpan(new Bold(), boldBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						inverseBegin = null;
					}
					if(italicsBegin != null)
					{
						builder.setSpan(new Italics(), italicsBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						inverseBegin = null;
					}
					if(underlineBegin != null)
					{
						builder.setSpan(new Underline(), underlineBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						inverseBegin = null;
					}
				}
			}
			else if(ch == 0x0F)
			{
				if(colorBegin != null)
				{
					builder.setSpan(new Color(fgColor, bgColor), colorBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					colorBegin = null;
				}
				if(inverseBegin != null)
				{
					builder.setSpan(new Inverse(), inverseBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					inverseBegin = null;
				}
				if(boldBegin != null)
				{
					builder.setSpan(new Bold(), boldBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					boldBegin = null;
				}
				if(italicsBegin != null)
				{
					builder.setSpan(new Italics(), italicsBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					italicsBegin = null;
				}
				if(underlineBegin != null)
				{
					builder.setSpan(new Underline(), underlineBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					underlineBegin = null;
				}
			}
			else if(ch == 0x16)
			{
				if(inverseBegin == null)
					inverseBegin = builder.length();
				else
				{
					builder.setSpan(new Inverse(), inverseBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					inverseBegin = null;
				}
			}
			else if(ch == 0x1D)
			{
				if(italicsBegin == null)
					italicsBegin = builder.length();
				else
				{
					builder.setSpan(new Italics(), italicsBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					italicsBegin = null;
				}
			}
			else if(ch == 0x1F)
			{
				if(underlineBegin == null)
					underlineBegin = builder.length();
				else
				{
					builder.setSpan(new Underline(), underlineBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					underlineBegin = null;
				}
			}
			else
				builder.append(ch);
		}
		if(colorBegin != null)
			builder.setSpan(new Color(fgColor, bgColor), colorBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		if(inverseBegin != null)
			builder.setSpan(new Inverse(), inverseBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		if(boldBegin != null)
			builder.setSpan(new Bold(), boldBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		if(italicsBegin != null)
			builder.setSpan(new Italics(), italicsBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		if(underlineBegin != null)
			builder.setSpan(new Underline(), underlineBegin, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		for(LinkFinder.Link link : LinkFinder.links(builder))
			builder.setSpan(link, link.begin, link.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		return builder;
	}
}
