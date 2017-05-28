package mirrg.application.service.pwi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Stream;

import mirrg.application.service.pwi.core.ILineDispatcher;
import mirrg.application.service.pwi.core.ILineReceiver;
import mirrg.application.service.pwi.core.Line;
import mirrg.application.service.pwi.core.LineBuffer;
import mirrg.application.service.pwi.core.LineSource;
import mirrg.application.service.pwi.core.LineStorage;
import mirrg.application.service.pwi.core.Logger;
import mirrg.application.service.pwi.util.PropertiesWrapper;

public class Launcher
{

	public Config config = new Config();

	private LineBuffer in;
	private LineBuffer out;
	private LineStorage lineStorage;
	private Logger logger;

	public Launcher(PropertiesWrapper properties)
	{

		// read config
		{
			config.command = properties.get("command");
			config.currentDirectory = properties.get("currentDirectory");
			config.encoding = properties.get("encoding");
			config.logFileName = properties.get("logFileName");

			config.restart = properties.getAsBoolean("restart");
			config.logCount = properties.getAsInt("logCount");

			config.useWeb = properties.getAsBoolean("useWeb");
			config.web.homeDirectory = properties.get("web.homeDirectory");
			config.web.hostname = properties.get("web.hostname");
			config.web.port = properties.getAsInt("web.port");
			config.web.backlog = properties.getAsInt("web.backlog");
			config.web.needAuthentication = properties.getAsBoolean("web.needAuthentication");
			config.web.user = properties.get("web.user");
			config.web.password = properties.get("web.password");
		}

		in = new LineBuffer();
		out = new LineBuffer();
		lineStorage = new LineStorage(config.logCount);
		logger = new Logger(out, new LineSource("SERVICE", "magenta"));

	}

	public Optional<Runner> oRunner = Optional.empty();

	public void start() throws Exception
	{

		// prepare stdin receiver
		{
			new LineDispatcherInputStream(
				logger,
				new LineReceiverService(logger, this, in, out),
				new LineSource("STDIN", "blue"),
				new BufferedReader(new InputStreamReader(System.in))).start();
			if (config.useWeb) {
				new LineDispatcherWebInterface(
					logger,
					new LineReceiverService(logger, this, in, out),
					new LineSource("WEB", "green"),
					config.web,
					lineStorage).start();
			}
		}

		// loop
		loop();

		// end
		System.exit(0);

	}

	private void loop()
	{
		while (true) {

			logger.log("Starting...");
			try {

				String sessionId = createSessionId();
				String currentDirectory = config.currentDirectory.replace("%s", sessionId);
				String[] command = Stream.of(config.command.split(" +"))
					.map(s -> s.replace("%s", currentDirectory))
					.toArray(String[]::new);
				new File(currentDirectory).mkdirs();
				String logFileName = config.logFileName.replace("%s", sessionId);
				PrintStream outLog = new PrintStream(new FileOutputStream(new File(currentDirectory, logFileName)), true);
				logger.log(String.format("Session Id: %s, Command: %s, Current Directory: %s",
					sessionId,
					String.join(" ", command),
					currentDirectory));

				Runner runner = new Runner(
					sessionId,
					command,
					currentDirectory,
					config.encoding,
					outLog);

				oRunner = Optional.of(runner);
				try {

					runner.run();

				} catch (Exception e) {
					logger.log(e);
				} finally {
					oRunner = Optional.empty();
				}

			} catch (Exception e) {
				logger.log(e);
			} finally {
				logger.log("Stopped");
			}

			if (!config.restart) break;
		}
	}

	public static final DateTimeFormatter FORMATTER_LOG = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");

	public class Runner
	{

		public String sessionId;
		public String[] command;
		public String currentDirectory;
		public String encoding;

		private PrintStream outLog;

		public Runner(String sessionId, String[] command, String currentDirectory, String encoding, PrintStream outLog)
		{
			this.sessionId = sessionId;
			this.command = command;
			this.currentDirectory = currentDirectory;
			this.encoding = encoding;
			this.outLog = outLog;
		}

		public void run() throws Exception
		{

			// open
			Process process = new ProcessBuilder(command).start();
			PrintStream stdin = new PrintStream(process.getOutputStream(), true, encoding);
			BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), encoding));
			BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream(), encoding));

			// start external thread
			ILineDispatcher a = new LineDispatcherBuffer(logger, new ILineReceiver() {

				@Override
				public void onLine(Line line) throws Exception
				{
					stdin.println(line.text);
				}

				@Override
				public void onClosed(LineSource source) throws Exception
				{

				}

			}, in).start();
			ILineDispatcher b = new LineDispatcherBuffer(logger, new ILineReceiver() {

				@Override
				public void onLine(Line line) throws Exception
				{
					lineStorage.push(line);

					String text = String.format("[%s] [%s] %s", line.time.format(FORMATTER_LOG), line.source.name, line.text);
					System.out.println(text);
					outLog.println(text);
				}

				@Override
				public void onClosed(LineSource source) throws Exception
				{

				}

			}, out).start();

			// start internal thread
			ILineDispatcher c = new LineDispatcherInputStream(logger, new LineReceiverBuffor(out), new LineSource("STDOUT", "black"), stdout).start();
			ILineDispatcher d = new LineDispatcherInputStream(logger, new LineReceiverBuffor(out), new LineSource("STDERR", "red"), stderr).start();

			// wait
			process.waitFor();

			// dispose
			a.stop();
			b.stop();
			c.stop();
			d.stop();
			outLog.close();

		}

	}

	//

	public static final DateTimeFormatter FORMATTER_SESSION_ID = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

	public static String createSessionId()
	{
		return LocalDateTime.now().format(FORMATTER_SESSION_ID);
	}

}
