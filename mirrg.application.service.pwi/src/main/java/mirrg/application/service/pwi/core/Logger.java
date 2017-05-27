package mirrg.application.service.pwi.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Stream;

public class Logger
{

	private LineBuffer out;
	private LineSource source;

	public Logger(LineBuffer out, LineSource source)
	{
		this.out = out;
		this.source = source;
	}

	public void log(String text)
	{
		out.push(new Line(source, text));
	}

	public void log(Exception e)
	{
		StringWriter out = new StringWriter();
		e.printStackTrace(new PrintWriter(out));
		Stream.of(out.toString().split("\r\n|[\r\n]")).forEach(l -> log(l));
	}

}
