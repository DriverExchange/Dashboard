package dx.dashboard;

import dx.dashboard.controllers.AssetsController;
import dx.dashboard.controllers.DashboardController;
import dx.dashboard.tools.Codec;
import dx.dashboard.tools.Database;
import dx.dashboard.tools.Tools;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import static spark.Spark.*;

public class App {

	protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(App.class);

	public static final String startId;
	static {
		URL scriptUrl = ClassLoader.getSystemResource("shellscripts/current_commit_hash.sh");
		try {
			File scriptFile = new File(scriptUrl.toURI());
			String hash = Tools.runProcess("current_commit_hash.sh", scriptFile.getAbsolutePath());
			if (hash == null || hash.length() < 8) {
				hash = Codec.hexMD5(Codec.UUID()).substring(0, 8);
			}
			startId = hash.substring(0, 8);
			Logger.info("startId: %s", startId);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public static final Properties configuration = new Properties();
	static {
		InputStream is = ClassLoader.getSystemResourceAsStream("application.properties");
		if (is == null) {
			Logger.error("application.conf file cannot be found");
		}
		try {
			configuration.load(is);
		} catch (IOException e) {
			throw new RuntimeException("Error while loading application.conf", e);
		}
	}

	public static class Databases {
		public Database dx = new Database("dx");
		public Database dashboard = new Database("dashboard");
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
			Logger.info("staticFileLocation");
			staticFileLocation("static");
		}

		AssetsController.init();
		DashboardController.init();
	}

}
