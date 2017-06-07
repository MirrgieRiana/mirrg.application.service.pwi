package mirrg.application.service.pwi.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesWrapper
{

	private Properties properties;

	public PropertiesWrapper(Properties properties)
	{
		this.properties = properties;
	}

	public String getAsString(String key)
	{
		return properties.getProperty(key);
	}

	public int getAsInt(String key)
	{
		return Integer.parseInt(getAsString(key), 10);
	}

	public double getAsDouble(String key)
	{
		return Double.parseDouble(getAsString(key));
	}

	public boolean getAsBoolean(String key)
	{
		return Boolean.parseBoolean(getAsString(key));
	}

	public static Properties loadPropertiesWithParent(Properties properties, File directory, String propertyFileNameSuffix) throws IOException
	{
		String parent = properties.getProperty("parent");
		if (parent != null && !parent.isEmpty()) {
			if (!new File(directory, parent).isFile()) parent += propertyFileNameSuffix;
			Properties properties2 = new Properties(loadPropertiesWithParent(new File(directory, parent), propertyFileNameSuffix));
			properties2.putAll(properties);
			return properties2;
		}
		return properties;
	}

	public static Properties loadPropertiesWithParent(File file, String propertyFileNameSuffix) throws IOException
	{
		Properties properties = new Properties();
		properties.load(new FileInputStream(file));
		return loadPropertiesWithParent(properties, file.getParentFile(), propertyFileNameSuffix);
	}

}
