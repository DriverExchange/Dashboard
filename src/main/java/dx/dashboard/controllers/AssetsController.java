package dx.dashboard.controllers;

import dx.dashboard.tools.CoffeeScriptCompiler;

import static spark.Spark.*;

public class AssetsController {

	public static void init() {
		get("/assets/scripts/:fileName", (req, res) -> {
			String scriptName = req.params("fileName");
			String coffeeScriptName;
			coffeeScriptName = scriptName.substring(0, scriptName.length() - 3);
			return CoffeeScriptCompiler.compileCoffee(coffeeScriptName);
		});
	}

}
