package mirrg.application.service.pwi.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PropertiesWrapper
{

	public Properties properties;

	public PropertiesWrapper(Properties properties)
	{
		this.properties = properties;
	}

	public String get(String key)
	{
		return properties.getProperty(key);
	}

	public int getAsInt(String key)
	{
		return Integer.parseInt(properties.getProperty(key), 10);
	}

	public double getAsDouble(String key)
	{
		return Double.parseDouble(properties.getProperty(key));
	}

	public boolean getAsBoolean(String key)
	{
		return Boolean.parseBoolean(properties.getProperty(key));
	}

	public static PropertiesWrapper create(String defaultPropertyFileName, String propertyFileNameSuffix, String... args) throws FileNotFoundException, IOException
	{
		ArrayList<String> args2 = Stream.of(args).collect(Collectors.toCollection(ArrayList::new));

		//

		String propertyFileName = defaultPropertyFileName;
		Properties properties2 = new Properties();

		for (String arg : args2) {

			int index = arg.indexOf("=");
			if (index >= 0) {
				properties2.setProperty(arg.substring(0, index), arg.substring(index + 1));
				continue;
			}

			if (!arg.isEmpty()) {
				propertyFileName = arg;
				continue;
			}

		}

		//

		Properties properties = new Properties();
		if (!new File(propertyFileName).exists()) propertyFileName += propertyFileNameSuffix;
		properties.load(new FileInputStream(new File(propertyFileName)));
		properties.putAll(properties2);
		return new PropertiesWrapper(properties);
	}

}
