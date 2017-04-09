package com.mniip.bananapeel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class OrderedList<E> extends ArrayList<E>
{
	private Comparator<E> comparator;

	public Comparator<E> getComparator()
	{
		return comparator;
	}

	public void setComparator(Comparator<E> comparator)
	{
		this.comparator = comparator;
		Collections.sort(this, comparator);
	}

	public OrderedList(Comparator<E> comparator)
	{
		super();
		this.comparator = comparator;
	}

	public OrderedList(Comparator<E> comparator, Collection<? extends E> es)
	{
		super(es);
		this.comparator = comparator;
		Collections.sort(this, comparator);
	}

	@Override
	public boolean add(E e)
	{
		if(comparator.compare(get(size() - 1), e) > 0)
			throw new IllegalStateException("Add " + e + " after " + get(size() - 1));
		return super.add(e);
	}

	@Override
	public void add(int index, E e)
	{
		if(index > 0 && comparator.compare(get(index - 1), e) > 0)
			throw new IllegalStateException("Add " + e + " after " + get(index - 1));
		if(index < size() && comparator.compare(get(index), e) < 0)
			throw new IllegalStateException("Add " + e + " before " + get(index));
		super.add(index, e);
	}

	@Override
	public E set(int index, E e)
	{
		if(index > 0 && comparator.compare(get(index - 1), e) > 0)
			throw new IllegalStateException("Set to " + e + " after " + get(index - 1));
		if(index < size() - 1 && comparator.compare(get(index + 1), e) < 0)
			throw new IllegalStateException("Set to " + e + " before " + get(index + 1));
		return super.set(index, e);
	}

	public void addOrdered(E e)
	{
		int i = 0;
		while(i < size() && comparator.compare(get(i), e) < 0)
			i++;
		super.add(i, e);
	}

	public void setOrdered(int index, E e)
	{
		while(index > 0 && comparator.compare(get(index - 1), e) > 0)
		{
			super.set(index - 1, super.set(index, get(index - 1)));
			index--;
		}
		while(index < size() - 1 && comparator.compare(get(index + 1), e) < 0)
		{
			super.set(index + 1, super.set(index, get(index + 1)));
			index++;
		}
	}
}
