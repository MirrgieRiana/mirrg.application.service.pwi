package mirrg.application.service.pwi;

import mirrg.application.service.pwi.core.ILineReceiver;
import mirrg.application.service.pwi.core.LineBuffer;
import mirrg.application.service.pwi.core.LineSource;
import mirrg.application.service.pwi.core.Logger;

public class LineDispatcherBuffer extends LineDispatcherThreadBase
{

	private Logger logger;
	private ILineReceiver receiver;
	private LineBuffer in;

	public LineDispatcherBuffer(Logger logger, ILineReceiver receiver, LineBuffer in)
	{
		this.logger = logger;
		this.receiver = receiver;
		this.in = in;
	}

	@Override
	protected Thread createThread() throws Exception
	{
		return new Thread(() -> {

			while (true) {

				synchronized (in) {

					in.flush().forEach(l -> receiver.onLine(logger, l));

					try {
						in.wait();
					} catch (InterruptedException e) {
						break;
					}
				}

			}

			receiver.onClosed(logger, new LineSource("BUFFER", "orange"));

		}, "BUFFER");
	}

}
