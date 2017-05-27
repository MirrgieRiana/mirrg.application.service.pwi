package mirrg.application.service.pwi.core;

import java.util.LinkedList;

public class LineBuffer
{

	private LinkedList<Line> lines = new LinkedList<>();

	public synchronized void push(Line line)
	{
		lines.addLast(line);
		notifyAll();
	}

	public synchronized LinkedList<Line> flush()
	{
		LinkedList<Line> lines2 = lines;
		lines = new LinkedList<>();
		return lines2;
	}

}
