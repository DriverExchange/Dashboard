package dx.dashboard.controllers;

import dx.dashboard.tools.CoffeeScriptCompiler;
import dx.dashboard.tools.Configuration;
import dx.dashboard.tools.StylusCompiler;

import static spark.Spark.*;

public class AssetsController {

	public static void init() {

		get("/assets/:startId/scripts/:fileName", (req, res) -> {
			String scriptName = req.params("fileName");
			String coffeeScriptName = scriptName.substring(0, scriptName.length() - 3);
			res.type("text/javascript");
			if (!Configuration.isDevMode()) {
				res.header("Cache-Control", "max-age=3600");
			}
			return CoffeeScriptCompiler.compileCoffee(coffeeScriptName);
		});

		get("/assets/:startId/styles/:fileName", (req, res) -> {
			String stylusName = req.params("fileName");
			String stylusNameWithoutExt = stylusName.substring(0, stylusName.length() - 4);
			res.type("text/css");
			if (!Configuration.isDevMode()) {
				res.header("Cache-Control", "max-age=3600");
			}
			return StylusCompiler.compile(stylusNameWithoutExt);
		});
	}

}
