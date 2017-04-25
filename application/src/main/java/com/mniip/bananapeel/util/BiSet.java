package com.mniip.bananapeel.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class BiSet<E> extends TreeSet<E> implements Set<E>
{
	private TreeSet<TreeSet<E>> other;

	private static class SetComparator<E> implements Comparator<Set<E>>
	{
		private final Comparator<E> base;

		private SetComparator(Comparator<E> base)
		{
			this.base = base;
		}

		public int compare(Set<E> a, Set<E> b)
		{
			if(a.isEmpty() || b.isEmpty())
				throw new IllegalStateException("Empty set in secondary tree");
			return base.compare(a.iterator().next(), b.iterator().next());
		}
	}

	public BiSet(Comparator<E> primary, Comparator<E> secondary)
	{
		super(primary);
		other = new TreeSet<>(new SetComparator<>(secondary));
	}

	public boolean add(E e)
	{
		TreeSet<E> item = new TreeSet<E>(comparator());
		item.add(e);
		TreeSet<E> target = other.floor(item);
		if(target != null && other.comparator().compare(item, target) == 0)
			target.add(e);
		else
			other.add(item);
		return super.add(e);
	}

	public boolean remove(Object e)
	{
		TreeSet<E> item = new TreeSet<E>(comparator());
		item.add((E)e);
		TreeSet<E> target = other.floor(item);
		if(target != null && other.comparator().compare(item, target) == 0)
		{
			if(target.size() == 1)
				other.remove(target);
			else
				target.remove(e);
		}
		return super.remove(e);
	}

	public boolean retainAll(Collection<?> c)
	{
		for(TreeSet<E> target : other)
		{
			target.retainAll(c);
		}
		return super.retainAll(c);
	}

	public boolean removeAll(Collection<?> c)
	{
		for(TreeSet<E> target : other)
			target.removeAll(c);
		return super.removeAll(c);
	}

	public void clear()
	{
		other.clear();
		super.clear();
	}

	public void setSecondaryComparator(Comparator<E> secondary)
	{
		other = new TreeSet<>(new SetComparator<>(secondary));
		addAll(this);
	}

	public E findPrimary(E e)
	{
		E value = floor(e);
		if(value == null || comparator().compare(value, e) != 0)
			return null;
		return value;
	}

	public E getSecondary(int i)
	{
		for(TreeSet<E> target : other)
		{
			int size = target.size();
			if(size < i)
				i -= size;
			else
				for(E e : target)
					if(i-- == 0)
						return e;
		}
		throw new IndexOutOfBoundsException("Get " + i + " past end of set");
	}
}
