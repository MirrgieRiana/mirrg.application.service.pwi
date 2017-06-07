package mirrg.application.service.pwi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import mirrg.application.service.pwi.util.PropertiesWrapper;

public class Main
{

	public static void main(String[] args) throws Exception
	{
		new Launcher(create("default", ".pwi.properties", args)).start();
	}

	private static PropertiesWrapper create(String defaultPropertyFileName, String propertyFileNameSuffix, String... args) throws FileNotFoundException, IOException
	{
		String propertyFileName = null;
		Properties properties = new Properties();
		{
			for (String arg : args) {

				int index = arg.indexOf("=");
				if (index >= 0) {
					properties.setProperty(arg.substring(0, index), arg.substring(index + 1));
					continue;
				}

				if (!arg.isEmpty()) {
					propertyFileName = arg;
					continue;
				}

			}
		}

		properties.setProperty("parent", propertyFileName == null ? defaultPropertyFileName : propertyFileName);
		return new PropertiesWrapper(PropertiesWrapper.loadPropertiesWithParent(properties, new File("."), propertyFileNameSuffix));
	}

}
