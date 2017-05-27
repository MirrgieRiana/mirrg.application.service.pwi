package mirrg.application.service.pwi.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class LineStorage
{

	private int size;
	private LinkedList<ImmutablePair<Integer, Line>> lines = new LinkedList<>();
	private int count = 0;

	public LineStorage(int size)
	{
		this.size = size;
	}

	public synchronized void push(Line line)
	{
		lines.addLast(new ImmutablePair<>(count, line));
		if (size != -1 && lines.size() > size) lines.removeFirst();
		count++;
		notifyAll();
	}

	public synchronized int getCount()
	{
		return count;
	}

	public synchronized Stream<ImmutablePair<Integer, Line>> stream()
	{
		ArrayList<ImmutablePair<Integer, Line>> lines2 = lines.stream().collect(Collectors.toCollection(ArrayList::new));
		return lines2.stream();
	}

}
