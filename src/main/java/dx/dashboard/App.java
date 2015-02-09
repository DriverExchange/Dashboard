package dx.dashboard;

import dx.dashboard.controllers.AssetsController;
import dx.dashboard.controllers.DashboardController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

	public static final Logger logger = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {
		AssetsController.init();
		DashboardController.init();
	}

}
