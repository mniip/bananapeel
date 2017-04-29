package com.mniip.bananapeel.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public interface Hook<C, D>
{
	boolean invoke(C context, D data);

	class Sequence<C, D> extends ArrayList<Hook<C, D>> implements Hook<C, D>
	{
		public Sequence()
		{
			super();
		}

		public Sequence(Collection<? extends Hook<C, D>> list)
		{
			super(list);
		}

		public boolean invoke(C context, D data)
		{
			for(Hook<C, D> hook : this)
				if(hook.invoke(context, data))
					return true;
			return false;
		}
	}

	abstract class Binned<B, C, D> implements Hook<C, D>
	{
		private Map<B, Sequence<C, D>> map = new TreeMap<>();

		abstract public B resolve(D data);

		public void add(B bin, Hook<C, D> hook)
		{
			Sequence<C, D> list = map.get(bin);
			if(list == null)
			{
				list = new Sequence<>();
				list.add(hook);
				map.put(bin, list);
			}
			else
				list.add(hook);
		}

		public boolean invoke(C context, D data)
		{
			B bin = resolve(data);
			Sequence<C, D> hook = map.get(bin);
			if(hook == null)
				return false;
			return hook.invoke(context, data);
		}
	}

	abstract class Filtered<C, D> implements Hook<C, D>
	{
		abstract public boolean filter(D data);

		private Hook<C, D> target;

		public Filtered(Hook<C, D> target)
		{
			this.target = target;
		}

		public boolean invoke(C context, D data)
		{
			return filter(data) && target.invoke(context, data);
		}
	}
}
