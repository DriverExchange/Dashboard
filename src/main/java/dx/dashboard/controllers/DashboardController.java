package dx.dashboard.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dx.dashboard.App;
import dx.dashboard.tools.GroovyTemplateEngine;
import dx.dashboard.tools.IO;
import dx.dashboard.tools.RenderArgs;
import fr.zenexity.dbhelper.JdbcResult;
import fr.zenexity.dbhelper.Sql.FinalQuery;
import spark.ModelAndView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class DashboardController {

	public static void init() {

		final String widgetsPath = App.configuration.getProperty("dashboard.widgets.path");
		if (widgetsPath == null) {
			throw new RuntimeException("dashboard.widgets.path is undefined");
		}

		final File widgetsDir = new File(widgetsPath, "src");
		if (!widgetsDir.exists()) {
			throw new RuntimeException(widgetsDir.getAbsolutePath() + " (defined in dashboard.widgets.path) was not found");
		}

		final String validNameRegex = "[a-zA-Z0-9]+";

		before((req, res) -> {
			RenderArgs.init();
			RenderArgs.put("startId", App.startId);
		});

		get("/", (req, res) -> {
			JsonArray widgets = new JsonArray();
			List<String> errors = new ArrayList<>();
			File[] widgetDirs = widgetsDir.listFiles();
			if (widgetDirs == null) {
				throw new RuntimeException(widgetsDir.getAbsolutePath() + " is not a directory");
			}
			for (File widgetDir : widgetDirs) {
				if (widgetDir.isDirectory()) {
					String widgetName = widgetDir.getName();
					if (!widgetDir.getName().matches(validNameRegex)) {
						errors.add(String.format("<strong>%s</strong> is not a correct widget name. It must match %s", widgetName, validNameRegex));
					}
					else {
						File configurationFile = new File(widgetDir, "configuration.json");
						if (!configurationFile.exists()) {
							errors.add(String.format("<strong>%s</strong> does not have a 'configuration.json' file", widgetName));
						}
						else {
							JsonObject configuration = (JsonObject) new JsonParser().parse(IO.readContentAsString(configurationFile));
							JsonObject widgetObject = new JsonObject();
							widgetObject.add("configuration", configuration);
							widgetObject.addProperty("name", widgetName);
							widgets.add(widgetObject);
						}
					}
				}
			}
			RenderArgs.put("errors", errors);
			RenderArgs.put("widgetsJson", new Gson().toJson(widgets));
			return new ModelAndView(RenderArgs.renderArgs.get(), "dashboard.html");
		}, new GroovyTemplateEngine());

		get("/widgets/:widgetName", (req, res) -> {
			res.type("application/json");
			JsonObject result = new JsonObject();
			List<String> errors = new ArrayList<>();
			String widgetName = req.params("widgetName");
			File widgetDir = new File(widgetsDir, widgetName);
			if (!widgetDir.exists()) {
				throw new RuntimeException(widgetsDir.getAbsolutePath() + " is not a directory");
			}
			for (File widgetFile : widgetDir.listFiles()) {
				String fileName = widgetFile.getName();
				if (fileName.equals("configuration.json")) {
					String strConfiguration = IO.readContentAsString(widgetFile);
					JsonObject objConfiguration = (JsonObject) new JsonParser().parse(strConfiguration);
					result.add("configuration", objConfiguration);
				}
				else if (fileName.endsWith(".sql")) {
					String queryName = fileName.substring(0, fileName.length() - 4);
					if (!validNameRegex.matches(validNameRegex)) {
						errors.add(String.format("<strong>%s</strong> is not a valid query name", queryName));
					}
					String strQuery = IO.readContentAsString(widgetFile);
					if (strQuery == null) {
						errors.add(String.format("<strong>%s</strong> query could not be read", queryName));
					}
					else if (strQuery.trim().startsWith("SELECT")) {
						errors.add(String.format("<strong>%s</strong> query must start with SELECT", queryName));
					}
					List<Map<String, Object>> queryResults = App.db.dx.run(new FinalQuery(strQuery), JdbcResult.mapFactory()).limit(501).list();
					if (queryResults.size() == 501) {
						errors.add(String.format("<strong>%s</strong>: there where more than 500 rows returned", queryName));
					}
					else {
						result.add(queryName, new JsonParser().parse(new Gson().toJson(queryResults)));
					}
				}
			}
			if (!errors.isEmpty()) {
				res.status(400);
				return new Gson().toJson(errors);
			}
			else {
				return new Gson().toJson(result);
			}
		});

	}

}
