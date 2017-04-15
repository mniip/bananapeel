package com.mniip.bananapeel.ui;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class EditIntPreference extends EditTextPreference
{
	private int minLimit = Integer.MIN_VALUE;
	private int maxLimit = Integer.MAX_VALUE;

	public EditIntPreference(Context ctx, AttributeSet set, int defStyleAttrs)
	{
		super(ctx, set, defStyleAttrs);
		minLimit = set.getAttributeIntValue(null, "min", Integer.MIN_VALUE);
		maxLimit = set.getAttributeIntValue(null, "max", Integer.MAX_VALUE);
	}

	public EditIntPreference(Context ctx, AttributeSet set)
	{
		super(ctx, set);
		minLimit = set.getAttributeIntValue(null, "min", Integer.MIN_VALUE);
		maxLimit = set.getAttributeIntValue(null, "max", Integer.MAX_VALUE);
	}

	public EditIntPreference(Context ctx)
	{
		super(ctx);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		if(positiveResult)
		{
			try
			{
				int i = Integer.parseInt(getEditText().getText().toString());
				if(i < minLimit || i > maxLimit)
					positiveResult = false;
			}
			catch(NumberFormatException e)
			{
				positiveResult = false;
			}
		}
		super.onDialogClosed(positiveResult);
	}

	@Override
	public boolean persistString(String text)
	{
		return persistInt(Integer.parseInt(text));
	}

	@Override
	public String getPersistedString(String def)
	{
		return String.valueOf(getPersistedInt(Integer.parseInt(def)));
	}

	public void setInt(int i)
	{
		setText(String.valueOf(i));
	}
}
