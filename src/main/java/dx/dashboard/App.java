package dx.dashboard;

import dx.dashboard.controllers.AssetsController;
import dx.dashboard.controllers.DashboardController;
import dx.dashboard.tools.Codec;
import dx.dashboard.tools.Tools;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

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

	public static void main(String[] args) {
		AssetsController.init();
		DashboardController.init();
	}

}
