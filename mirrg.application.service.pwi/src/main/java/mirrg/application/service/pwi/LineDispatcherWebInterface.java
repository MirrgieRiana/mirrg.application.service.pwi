package mirrg.application.service.pwi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import mirrg.application.service.pwi.Config.ConfigWeb;
import mirrg.application.service.pwi.core.ILineReceiver;
import mirrg.application.service.pwi.core.Line;
import mirrg.application.service.pwi.core.LineSource;
import mirrg.application.service.pwi.core.LineStorage;
import mirrg.application.service.pwi.core.Logger;

public class LineDispatcherWebInterface extends LineDispatcherThreadBase
{

	private Logger logger;
	private ILineReceiver receiver;
	private LineSource source;
	private ConfigWeb config;
	private LineStorage lineStorage;

	public LineDispatcherWebInterface(Logger logger, ILineReceiver receiver, LineSource source, ConfigWeb config, LineStorage lineStorage)
	{
		this.logger = logger;
		this.receiver = receiver;
		this.source = source;
		this.config = config;
		this.lineStorage = lineStorage;
	}

	@Override
	protected Thread createThread() throws Exception
	{

		// create server
		HttpServer httpServer = HttpServer.create(new InetSocketAddress(config.hostname, config.port), config.backlog);
		{
			HttpContext context = httpServer.createContext("/", e -> {

				String path = e.getRequestURI().getPath();
				if (path.toString().matches("/api(|/.*)")) {

					if (path.toString().matches("/api/log")) {
						send(e, String.format(
							"<table style='font-family: monospace; white-space: nowrap;'>%s</table>",
							lineStorage.stream()
								.map(t -> String.format(
									"<tr style=\"color: %s;\"><td>%s</td><td><b>%s</b></td><td>%s</td></tr>",
									t.right.source.color,
									t.left,
									t.right.source.name,
									t.right.text))
								.collect(Collectors.joining())));
					} else if (path.toString().matches("/api/log/count")) {
						send(e, "" + lineStorage.getCount());
					} else if (path.toString().matches("/api/send")) {
						String query = e.getRequestURI().getQuery();
						if (query == null) {
							send(e, 400, "400");
						} else {

							logger.log("Access: " + e.getRequestURI() + " " + e.getRemoteAddress());
							receiver.onLine(logger, new Line(source, query));

							send(e, "Success[" + query + "]");
						}
					} else {
						send(e, 404, "404");
					}

				} else if (path.toString().matches("/")) {
					redirect(e, "/index.html");
				} else if (!path.contains("/..")) {
					sendFile(e, new File(config.homeDirectory, path).toURI().toURL());
				} else {
					send(e, 404, "404");
				}

			});
			if (config.needAuthentication) {
				context.setAuthenticator(new BasicAuthenticator("Controller") {

					@Override
					public boolean checkCredentials(String arg0, String arg1)
					{
						if (!config.user.equals(arg0)) return false;
						if (!config.password.equals(arg1)) return false;
						return true;
					}
				});
			}
		}

		return new Thread(() -> {

			httpServer.start();
			logger.log("Web server started on http://" + config.hostname + ":" + config.port);

			while (true) {
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					break;
				}
			}

			receiver.onClosed(logger, source);

		}, source.name);
	}

	private static void redirect(HttpExchange e, String string) throws IOException
	{
		e.getResponseHeaders().add("Location", string);
		e.sendResponseHeaders(301, 0);
		e.getResponseBody().close();
	}

	private static void send(HttpExchange e, String text) throws IOException
	{
		send(e, 200, "text/html", text, "utf-8");
	}

	private static void send(HttpExchange e, int code, String text) throws IOException
	{
		send(e, code, "text/html", text, "utf-8");
	}

	private static void send(HttpExchange e, int code, String contentType, String text, String charset) throws IOException
	{
		e.getResponseHeaders().add("Content-Type", contentType + "; charset= " + charset);
		byte[] bytes = text.getBytes(charset);
		e.sendResponseHeaders(code, bytes.length);
		e.getResponseBody().write(bytes);
		e.getResponseBody().close();
	}

	private static void sendFile(HttpExchange e, URL url) throws IOException
	{
		try {
			InputStream in = url.openStream();

			ArrayList<ImmutablePair<byte[], Integer>> buffers = new ArrayList<>();
			while (true) {
				byte[] buffer = new byte[4000];
				int len = in.read(buffer);
				if (len == -1) break;
				buffers.add(new ImmutablePair<>(buffer, len));
			}
			in.close();

			e.sendResponseHeaders(200, buffers.stream()
				.mapToInt(t -> t.right)
				.sum());
			for (ImmutablePair<byte[], Integer> buffer : buffers) {
				e.getResponseBody().write(buffer.left, 0, buffer.right);
			}
			e.getResponseBody().close();
		} catch (IOException e2) {
			send(e, 404, "404");
		}
	}

}
