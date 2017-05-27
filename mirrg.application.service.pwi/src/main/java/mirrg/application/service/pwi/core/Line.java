package mirrg.application.service.pwi.core;

import java.time.LocalDateTime;

public class Line
{

	public final LineSource source;
	public final String text;
	public final LocalDateTime time;

	public Line(LineSource source, String text, LocalDateTime time)
	{
		this.source = source;
		this.text = text;
		this.time = time;
	}

	public Line(LineSource source, String text)
	{
		this(source, text, LocalDateTime.now());
	}

}
