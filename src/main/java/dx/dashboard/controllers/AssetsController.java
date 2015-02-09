package dx.dashboard.controllers;

import dx.dashboard.tools.CoffeeScriptCompiler;
import dx.dashboard.tools.Configuration;

import static spark.Spark.*;

public class AssetsController {

	public static void init() {
		get("/assets/:startId/scripts/:fileName", (req, res) -> {
			String scriptName = req.params("fileName");
			String coffeeScriptName;
			coffeeScriptName = scriptName.substring(0, scriptName.length() - 3);
			res.type("text/javascript");
			if (!Configuration.isDevMode()) {
				res.header("Cache-Control", "max-age=3600");
			}
			return CoffeeScriptCompiler.compileCoffee(coffeeScriptName);
		});
	}

}
