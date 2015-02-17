package dx.dashboard.tools;

import com.google.gson.GsonBuilder;
import dx.dashboard.App;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AssetsCompiler {

	public static String getEjsViews(String directoryName) {
		Map<String, String> loadedViews = new HashMap<>();
		String resourcePath = "ejs/" + directoryName;
		if (!App.isDevMode()) {
			String[] resources = Tools.getResourceListing(resourcePath);
			for (String resourceName : resources) {
				if (resourceName.endsWith(".ejs")) {
					String viewName = resourceName.substring(0, resourceName.indexOf("."));
					InputStream resourceIs = ClassLoader.getSystemResourceAsStream(resourcePath + "/" + resourceName);
					String viewContent = IO.readContentAsString(resourceIs).replaceAll("\\t", "");
					loadedViews.put(viewName, viewContent);
				}
			}
		}
		else {
			File viewDir = new File("src/main/resources/ejs", directoryName);
			if (!viewDir.exists()) {
				throw new RuntimeException(viewDir.getAbsolutePath() + " was not found");
			}
			File[] viewFiles = viewDir.listFiles();
			if (viewFiles == null) {
				throw new RuntimeException(viewDir.getAbsolutePath() + " isn't a directory not found");
			}
			for (File viewFile : viewFiles) {
				String fileName = viewFile.getName();
				if (fileName.endsWith(".ejs")) {
					String viewName = fileName.substring(0, fileName.indexOf("."));
					loadedViews.put(viewName, IO.readContentAsString(viewFile).replaceAll("\\t+", ""));
				}
			}
		}

		GsonBuilder gsonBuilder = new GsonBuilder().disableHtmlEscaping();
		return "window.fwk.views.addInline(" + gsonBuilder.create().toJson(loadedViews) + ");\n";
	}

}
