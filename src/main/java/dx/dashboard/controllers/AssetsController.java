package dx.dashboard.controllers;

import dx.dashboard.tools.CoffeeScriptCompiler;
import dx.dashboard.tools.IO;

import java.io.InputStream;

import static spark.Spark.*;

public class AssetsController {

	public static void init() {
		CoffeeScriptCompiler coffeeCompiler = new CoffeeScriptCompiler();
		get("/assets/scripts/:fileName", (req, res) -> {
			String scriptName = req.params("fileName");
			String coffeeScriptName;
			coffeeScriptName = scriptName.substring(0, scriptName.length() - 3) + ".coffee";
			InputStream coffeeIs = ClassLoader.getSystemResourceAsStream("scripts/" + coffeeScriptName);
			return coffeeCompiler.toJs(IO.readContentAsString(coffeeIs));
		});
	}

}
