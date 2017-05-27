package mirrg.application.service.pwi;

public class Config
{

	public String command;
	public String currentDirectory;
	public String encoding;

	public boolean restart;
	public int logCount;

	public boolean useWeb;
	public Config.ConfigWeb web = new ConfigWeb();

	public static class ConfigWeb
	{

		public String hostname;
		public int port;
		public int backlog;
		public boolean needAuthentication;
		public String user;
		public String password;

	}

}
