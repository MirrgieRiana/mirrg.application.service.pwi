package mirrg.application.service.pwi.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class LogWriter implements AutoCloseable
{

	private PrintStream out;
	public String charset = null;

	public synchronized void setFile(File file) throws FileNotFoundException
	{
		close();
		out = new PrintStream(new FileOutputStream(file));
	}

	public synchronized void println(String line)
	{
		out.println(line);
	}

	@Override
	public synchronized void close()
	{
		if (out != null) out.close();
	}

}
