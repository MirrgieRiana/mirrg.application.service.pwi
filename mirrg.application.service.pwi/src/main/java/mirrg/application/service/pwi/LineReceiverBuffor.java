package mirrg.application.service.pwi;

import mirrg.application.service.pwi.core.ILineReceiver;
import mirrg.application.service.pwi.core.Line;
import mirrg.application.service.pwi.core.LineBuffer;
import mirrg.application.service.pwi.core.LineSource;

public class LineReceiverBuffor implements ILineReceiver
{

	private LineBuffer lineBuffer;

	public LineReceiverBuffor(LineBuffer lineBuffer)
	{
		this.lineBuffer = lineBuffer;
	}

	@Override
	public void onLine(Line line) throws Exception
	{
		lineBuffer.push(line);
	}

	@Override
	public void onClosed(LineSource source) throws Exception
	{

	}

}
