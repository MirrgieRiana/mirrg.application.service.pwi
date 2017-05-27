package mirrg.application.service.pwi.core;

public interface ILineReceiver
{

	public void onLine(Line line) throws Exception;

	public default void onLine(Logger logger, Line line)
	{
		try {
			onLine(line);
		} catch (Exception e) {
			logger.log(e);
		}
	}

	public void onClosed(LineSource source) throws Exception;

	public default void onClosed(Logger logger, LineSource source)
	{
		try {
			onClosed(source);
		} catch (Exception e) {
			logger.log(e);
		}
	}

}
