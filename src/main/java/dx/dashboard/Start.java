package dx.dashboard;

import dx.dashboard.controllers.AssetsController;
import dx.dashboard.controllers.DashboardController;

public class Start {

	public static void main(String[] args) {
		AssetsController.init();
		DashboardController.init();
	}

}
