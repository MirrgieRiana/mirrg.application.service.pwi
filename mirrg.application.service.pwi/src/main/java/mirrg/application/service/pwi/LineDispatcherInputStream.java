package mirrg.application.service.pwi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InterruptedIOException;

import mirrg.application.service.pwi.core.ILineReceiver;
import mirrg.application.service.pwi.core.Line;
import mirrg.application.service.pwi.core.LineSource;
import mirrg.application.service.pwi.core.Logger;

public class LineDispatcherInputStream extends LineDispatcherThreadBase
{

	private Logger logger;
	private ILineReceiver receiver;
	private LineSource source;
	private BufferedReader in;

	public LineDispatcherInputStream(Logger logger, ILineReceiver receiver, LineSource source, BufferedReader in)
	{
		this.logger = logger;
		this.receiver = receiver;
		this.source = source;
		this.in = in;
	}

	@Override
	protected Thread createThread() throws Exception
	{
		return new Thread(() -> {

			while (true) {

				String text;
				try {
					text = in.readLine();
				} catch (InterruptedIOException e) {
					break;
				} catch (IOException e) {
					logger.log(e);
					break;
				}

				if (text == null) break;

				receiver.onLine(logger, new Line(source, text));

			}

			receiver.onClosed(logger, source);

		}, source.name);
	}

}
