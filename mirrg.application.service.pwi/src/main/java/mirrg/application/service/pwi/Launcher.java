package mirrg.application.service.pwi;

import java.io.BufferedReader;
import java.io.File;
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
import mirrg.application.service.pwi.core.LogWriter;
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
			config.command = properties.getAsString("command");
			config.currentDirectory = properties.getAsString("currentDirectory");
			config.encoding = properties.getAsString("encoding");
			config.logFileName = properties.getAsString("logFileName");

			config.restart = properties.getAsBoolean("restart");
			config.logCount = properties.getAsInt("logCount");

			config.useWeb = properties.getAsBoolean("useWeb");
			config.web.homeDirectory = properties.getAsString("web.homeDirectory");
			config.web.hostname = properties.getAsString("web.hostname");
			config.web.port = properties.getAsInt("web.port");
			config.web.backlog = properties.getAsInt("web.backlog");
			config.web.needAuthentication = properties.getAsBoolean("web.needAuthentication");
			config.web.user = properties.getAsString("web.user");
			config.web.password = properties.getAsString("web.password");
		}

		in = new LineBuffer();
		out = new LineBuffer();
		lineStorage = new LineStorage(config.logCount);
		logger = new Logger(out, new LineSource("SERVICE", "magenta"));

	}

	public Optional<Runner> oRunner = Optional.empty();
	private volatile boolean restartable = true;

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
			Thread thread = Thread.currentThread();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				logger.log("Stopping...");
				restartable = false;
				Optional<Runner> oRunner2 = oRunner;
				if (oRunner2.isPresent()) {
					Optional<Process> oProcess = oRunner2.get().oProcess;
					if (oProcess.isPresent()) {
						oProcess.get().destroy();
					}
				}
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}));
		}

		// loop
		loop();

		// end
		new Thread(() -> System.exit(0)).start();

	}

	private void loop()
	{
		try (LogWriter logWriter = new LogWriter()) {

			ILineDispatcher b;
			try {
				b = new LineDispatcherBuffer(logger, new ILineReceiver() {

					@Override
					public void onLine(Line line) throws Exception
					{
						lineStorage.push(line);

						String text = String.format("[%s] [%s] %s", line.time.format(FORMATTER_LOG), line.source.name, line.text);
						System.out.println(text);
						logWriter.println(text);
					}

					@Override
					public void onClosed(LineSource source) throws Exception
					{

					}

				}, out).start();
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

			while (true) {

				try {

					String sessionId = createSessionId();
					String currentDirectory = config.currentDirectory.replace("%s", sessionId);
					String[] command = Stream.of(config.command.split(" +"))
						.map(s -> s.replace("%s", currentDirectory))
						.toArray(String[]::new);

					new File(currentDirectory).mkdirs();
					logWriter.setFile(new File(currentDirectory, config.logFileName.replace("%s", sessionId)));

					logger.log(String.format("Session Id: %s, Command: %s, Current Directory: %s",
						sessionId,
						String.join(" ", command),
						currentDirectory));

					Runner runner = new Runner(
						sessionId,
						command,
						currentDirectory,
						config.encoding,
						logWriter);

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
				}

				if (!(restartable && config.restart)) break;
			}

			try {
				b.stop();
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

		}
	}

	public static final DateTimeFormatter FORMATTER_LOG = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");

	public class Runner
	{

		public String sessionId;
		public String[] command;
		public String currentDirectory;
		public String encoding;
		private LogWriter logWriter;

		public Optional<Process> oProcess = Optional.empty();

		public Runner(String sessionId, String[] command, String currentDirectory, String encoding, LogWriter logWriter)
		{
			this.sessionId = sessionId;
			this.command = command;
			this.currentDirectory = currentDirectory;
			this.encoding = encoding;
			this.logWriter = logWriter;
		}

		public void run() throws Exception
		{

			// open
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			processBuilder.directory(new File(currentDirectory));
			Process process = processBuilder.start();
			oProcess = Optional.of(process);
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

			// start internal thread
			ILineDispatcher c = new LineDispatcherInputStream(logger, new LineReceiverBuffor(out), new LineSource("STDOUT", "black"), stdout).start();
			ILineDispatcher d = new LineDispatcherInputStream(logger, new LineReceiverBuffor(out), new LineSource("STDERR", "red"), stderr).start();

			// wait
			process.waitFor();
			oProcess = Optional.empty();

			// dispose
			a.stop();
			c.stop();
			d.stop();

		}

	}

	//

	public static final DateTimeFormatter FORMATTER_SESSION_ID = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

	public static String createSessionId()
	{
		return LocalDateTime.now().format(FORMATTER_SESSION_ID);
	}

}
