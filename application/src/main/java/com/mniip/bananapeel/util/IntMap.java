package com.mniip.bananapeel.util;

import android.util.SparseArray;

public class IntMap<E> extends SparseArray<E> implements Iterable<E>
{
	public static class KV<E>
	{
		private IntMap<E> parent;
		private int index;

		private KV(IntMap<E> parent, int index)
		{
			this.parent = parent;
			this.index = index;
		}

		public int getKey()
		{
			return parent.keyAt(index);
		}

		public E getValue()
		{
			return parent.valueAt(index);
		}

		public void setValue(E value)
		{
			parent.setValueAt(index, value);
		}

		private static class Iterator<E> implements java.util.Iterator<KV<E>>
		{
			private int index = 0;
			private IntMap<E> parent;

			private Iterator(IntMap<E> parent)
			{
				this.parent = parent;
			}

			@Override
			public boolean hasNext()
			{
				return index < parent.size();
			}

			@Override
			public KV<E> next()
			{
				return new KV<>(parent, index++);
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		}
	}

	private static class Iterator<E> implements java.util.Iterator<E>
	{
		private int index = 0;
		private IntMap<E> parent;

		private Iterator(IntMap<E> parent)
		{
			this.parent = parent;
		}

		@Override
		public boolean hasNext()
		{
			return index < parent.size();
		}

		@Override
		public E next()
		{
			return parent.valueAt(index++);
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public java.util.Iterator<E> iterator()
	{
		return new Iterator<>(this);
	}

	public static class Pairs<E> implements Iterable<KV<E>>
	{
		private IntMap<E> parent;

		private Pairs(IntMap<E> parent)
		{
			this.parent = parent;
		}

		@Override
		public java.util.Iterator<KV<E>> iterator()
		{
			return new KV.Iterator<>(parent);
		}
	}

	public Pairs<E> pairs()
	{
		return new Pairs<>(this);
	}
}
