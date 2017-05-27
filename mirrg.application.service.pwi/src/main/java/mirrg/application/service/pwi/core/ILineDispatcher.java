package mirrg.application.service.pwi.core;

public interface ILineDispatcher
{

	public ILineDispatcher start(boolean isDaemon) throws Exception;

	public default ILineDispatcher start() throws Exception
	{
		return start(true);
	}

	public void stop() throws Exception;

}
