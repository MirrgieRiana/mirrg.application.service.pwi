package mirrg.application.service.pwi;

import mirrg.application.service.pwi.core.ILineReceiver;
import mirrg.application.service.pwi.core.Line;
import mirrg.application.service.pwi.core.LineBuffer;
import mirrg.application.service.pwi.core.LineSource;
import mirrg.application.service.pwi.core.Logger;

public class LineReceiverService implements ILineReceiver
{

	private Logger logger;
	private Launcher launcher;
	private LineBuffer in;
	private LineBuffer out;

	public LineReceiverService(Logger logger, Launcher launcher, LineBuffer in, LineBuffer out)
	{
		this.logger = logger;
		this.launcher = launcher;
		this.in = in;
		this.out = out;
	}

	@Override
	public void onLine(Line line) throws Exception
	{
		out.push(line);

		if (line.text.startsWith("/")) {
			if (line.text.equals("/set restart false")) {
				launcher.config.restart = false;
				logger.log("Changed");
			} else if (line.text.equals("/set restart true")) {
				launcher.config.restart = true;
				logger.log("Changed");
			} else if (line.text.equals("/get sessionId")) {
				logger.log(launcher.oRunner.map(r -> r.sessionId).orElse(""));
			} else if (line.text.equals("/help")) {
				logger.log("/set restart true");
				logger.log("/set restart false");
				logger.log("/get sessionId");
				logger.log("/help");
			} else if (line.text.startsWith("//")) {
				in.push(new Line(line.source, line.text.substring(1), line.time));
			} else {
				logger.log("Unknown Command: " + line.text);
			}
		} else {
			in.push(line);
		}
	}

	@Override
	public void onClosed(LineSource source) throws Exception
	{

	}

}
