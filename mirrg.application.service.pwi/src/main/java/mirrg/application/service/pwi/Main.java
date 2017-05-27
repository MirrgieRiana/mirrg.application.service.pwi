package mirrg.application.service.pwi;

import mirrg.application.service.pwi.util.PropertiesWrapper;

public class Main
{

	public static void main(String[] args) throws Exception
	{
		new Launcher(PropertiesWrapper.create("default", ".pwi.properties", args)).start();
	}

}
