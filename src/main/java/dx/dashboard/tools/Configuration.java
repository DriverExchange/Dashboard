package dx.dashboard.tools;

import dx.dashboard.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {

	public static final Properties properties = new Properties();
	static {
		InputStream is = ClassLoader.getSystemResourceAsStream("application.conf");
		if (is == null) {
			Logger.error("application.conf file cannot be found");
		}
		try {
			properties.load(is);
		} catch (IOException e) {
			throw new RuntimeException("Error while loading application.conf", e);
		}
	}

	public static boolean isDevMode() {
		return Configuration.properties.getProperty("mode", "prod").equals("dev");
	}

}
