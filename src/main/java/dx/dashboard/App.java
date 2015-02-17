package dx.dashboard;

import dx.dashboard.controllers.AssetsController;
import dx.dashboard.controllers.DashboardController;
import dx.dashboard.tools.Codec;
import dx.dashboard.tools.Database;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static spark.Spark.*;

public class App {

	protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(App.class);

	public static final String startId;
	static {
		startId = Codec.hexMD5(Codec.UUID()).substring(0, 7);
		Logger.info("startId: %s", startId);
	}

	public static final Properties configuration = new Properties();
	static {
		try {
			InputStream configurationIs;
			File localConfigurationFile = new File("application.properties");
			if (localConfigurationFile.exists()) {
				Logger.info("Using local application.properties (in the project root) instead of the one in src/main/resource");
				configurationIs = new FileInputStream(localConfigurationFile);
			}
			else {
				Logger.info("Using default application.properties in src/main/resource");
				configurationIs = ClassLoader.getSystemResourceAsStream("application.properties");
			}
			if (configurationIs == null) {
				Logger.error("application.conf file cannot be found");
			}
			configuration.load(configurationIs);
		} catch (IOException e) {
			throw new RuntimeException("Error while loading application.conf", e);
		}
	}

	public static class Databases {
		public Database dx = new Database("dx");
//		public Database dashboard = new Database("dashboard");
	}

	public static final Databases db = new Databases();

	public static boolean isDevMode() {
		return configuration.getProperty("mode", "prod").equals("dev");
	}

	public static void main(String[] args) {
		if (isDevMode()) {
			AssetsController.initDevStaticFile();
		}
		else {
			staticFileLocation("static");
		}

		before((req, res) -> {
			String login = configuration.getProperty("global.login");
			String password = configuration.getProperty("global.password");
			if (login != null && password != null) {
				String authorization = req.headers("authorization");
				if (authorization == null || !authorization.equals("Basic " + Codec.encodeBASE64(login + ":" + password))) {
					res.header("WWW-Authenticate", "Basic realm=Unauthorized");
					res.status(401);
				}
			}
		});

		AssetsController.init();
		DashboardController.init();

		exception(RuntimeException.class, (e, req, res) -> {
			res.status(500);
			res.body("<pre style=\"color: #c00; white-space: pre-wrap\">" + ExceptionUtils.getStackTrace(e) + "</pre>");
		});
	}

}
