package dx.dashboard.tools;

import com.google.gson.GsonBuilder;
import dx.dashboard.App;
import dx.dashboard.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AssetsCompiler {

	public static String getEjsViews(String directoryName) {
		File viewDir = App.isDevMode() ? new File("src/main/resources/ejs", directoryName) : Tools.getResourceAsFile("ejs/" + directoryName);
		if (!viewDir.exists()) {
			throw new RuntimeException(viewDir.getAbsolutePath() + " was not found");
		}
		File[] viewFiles = viewDir.listFiles();
		if (viewFiles == null) {
			throw new RuntimeException(viewDir.getAbsolutePath() + " isn't a directory not found");
		}
		Map<String, String> loadedViews = new HashMap<>();
		for (File viewFile : viewFiles) {
			String fileName = viewFile.getName();
			if (fileName.endsWith(".ejs")) {
				String viewName = fileName.substring(0, fileName.indexOf("."));
				loadedViews.put(viewName, IO.readContentAsString(viewFile).replaceAll("\\t+", ""));
			}
			else {
				Logger.warn("Invalid extension for " + viewFile.getName() + ". It should be 'ejs'.");
			}
		}
		GsonBuilder gsonBuilder = new GsonBuilder().disableHtmlEscaping();
		return "window.fwk.views.addInline(" + gsonBuilder.create().toJson(loadedViews) + ");\n";
	}

}
