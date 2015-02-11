package dx.dashboard.controllers;

import dx.dashboard.App;
import dx.dashboard.Logger;
import dx.dashboard.tools.CoffeeScriptCompiler;
import dx.dashboard.tools.IO;
import dx.dashboard.tools.StylusCompiler;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

import static spark.Spark.*;

public class AssetsController {

	public static void init() {

		get("/assets/:startId/scripts/:fileName", (req, res) -> {
			String scriptName = req.params("fileName");
			String coffeeScriptName = scriptName.substring(0, scriptName.length() - 3);
			res.type("text/javascript");
			if (!App.isDevMode()) {
				res.header("Cache-Control", "max-age=3600");
			}
			return CoffeeScriptCompiler.compileCoffee(coffeeScriptName);
		});

		get("/assets/:startId/styles/:fileName", (req, res) -> {
			String stylusName = req.params("fileName");
			String stylusNameWithoutExt = stylusName.substring(0, stylusName.length() - 4);
			res.type("text/css");
			if (!App.isDevMode()) {
				res.header("Cache-Control", "max-age=3600");
			}
			return StylusCompiler.compile(stylusNameWithoutExt);
		});

	}

	public static final MimetypesFileTypeMap mimeTypes = new MimetypesFileTypeMap(ClassLoader.getSystemResourceAsStream("mime.types"));
	public static void initDevStaticFile() {
		get("/public/*", (req, res) -> {
			String path = req.splat()[0];
			File file = new File(new File("src/main/resources/static/public"), path);
			if (!file.exists()) {
				res.status(404);
				Logger.error("File not found (" + file.getAbsolutePath() + ")");
				return "File not found";
			}
			else {
				HttpServletResponse raw = res.raw();
				String mimeType = mimeTypes.getContentType(path);
				res.type(mimeType);
				raw.getOutputStream().write(IO.readContent(file));
				return "";
			}
		});
	}

}
