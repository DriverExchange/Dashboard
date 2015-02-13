package dx.dashboard.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static spark.Spark.*;

public class DashboardController {

	public static final String widgetsPath = App.configuration.getProperty("dashboard.widgets.path");
	static {
		if (widgetsPath == null) {
			throw new RuntimeException("dashboard.widgets.path is undefined");
		}
	}
	public static final File widgetsDir = new File(widgetsPath, "src");
	static {
		if (!widgetsDir.exists()) {
			throw new RuntimeException(widgetsDir.getAbsolutePath() + " (defined in dashboard.widgets.path) was not found");
		}
	}

	public static final Pattern validNameRegex = Pattern.compile("[a-zA-Z0-9]+");

	public static JsonArray readDashboardsConfiguration() {
		List<String> errors = RenderArgs.get("errors");
		File dashboardConfFile = new File(widgetsDir, "dashboards.json");
		if (!dashboardConfFile.exists()) {
			errors.add(dashboardConfFile.getAbsolutePath() + " not found");
		}
		else {
			try {
				return (JsonArray) new JsonParser().parse(IO.readContentAsString(dashboardConfFile));
			} catch (JsonSyntaxException e) {
				errors.add(String.format("Malformed JSON in dashboards.json"));
			}
		}
		return null;
	}

	public static JsonObject getDashboardConfiguration(JsonArray dashboardsConf, String dashboardName) {
		List<String> errors = RenderArgs.get("errors");
		if (dashboardsConf != null) {
			Iterator<JsonElement> dashboardsIt = dashboardsConf.iterator();
			while (dashboardsIt.hasNext()) {
				JsonObject dashboardConf = (JsonObject) dashboardsIt.next();
				String name = dashboardConf.get("name").getAsString();
				if (name != null && name.equals(dashboardName)) {
					return dashboardConf;
				}
			}
		}
		errors.add(String.format(dashboardName + " not found in dashboards.json"));
		return null;
	}

	public static JsonObject readWidgetConfiguration(String widgetName) {
		File configurationFile = new File(widgetsDir, widgetName + "/configuration.json");
		if (configurationFile.exists()) {
			try {
				return (JsonObject) new JsonParser().parse(IO.readContentAsString(configurationFile));
			} catch (JsonSyntaxException e) {
				List<String> errors = RenderArgs.get("errors");
				errors.add(String.format("<strong>%s</strong>: malformed JSON in configuration.json", widgetName));
			}
		}
		return null;
	}

	public static void init() {

		before((req, res) -> {
			RenderArgs.init();
			RenderArgs.put("startId", App.startId);
		});

		get("/", (req, res) -> {
			JsonArray dashboardsConf = readDashboardsConfiguration();
			JsonObject dashboardObject = (JsonObject) dashboardsConf.iterator().next();
			String dashboardName = dashboardObject.get("name").getAsString();
			res.redirect("/dashboards/" + dashboardName);
			return "";
		});

		get("/dashboards/:dashboardName", (req, res) -> {
			String dashboardName = req.params("dashboardName");
			List<String> errors = new ArrayList<>();
			RenderArgs.put("errors", errors);
			Map<String, String> widgetTitles = new HashMap<>();
			JsonArray dashboardsConf = readDashboardsConfiguration();
			JsonObject dashboardConf = getDashboardConfiguration(dashboardsConf, dashboardName);
			if (dashboardConf != null) {
				JsonArray widgetsConf = dashboardConf.getAsJsonArray("widgets");
				if (widgetsConf == null) {
					errors.add(String.format("'widgets' is not defined for " + dashboardName + " in dashboards.json"));
				} else {
					Iterator<JsonElement> colsIt = widgetsConf.iterator();
					while (colsIt.hasNext()) {
						JsonArray widgetNames = colsIt.next().getAsJsonArray();
						Iterator<JsonElement> widgetNamesIt = widgetNames.iterator();
						while (widgetNamesIt.hasNext()) {
							String widgetName = widgetNamesIt.next().getAsString();
							JsonObject configuration = readWidgetConfiguration(widgetName);
							String widgetTitle = configuration.get("title").getAsString();
							widgetTitles.put(widgetName, widgetTitle);
						}
					}
				}
			}
			RenderArgs.put("errors", errors);
			RenderArgs.addJsData("dashboardsConf", dashboardsConf);
			RenderArgs.addJsData("dashboardConf", dashboardConf);
			RenderArgs.addJsData("widgetTitles", widgetTitles);
			return new ModelAndView(RenderArgs.renderArgs.get(), "dashboard.html");
		}, new GroovyTemplateEngine());

		get("/widgets/:widgetName", (req, res) -> {
			res.type("application/json");
			JsonObject result = new JsonObject();
			List<String> errors = new ArrayList<>();
			String widgetName = req.params("widgetName");
			JsonObject objConfiguration = readWidgetConfiguration(widgetName);
			File widgetDir = new File(widgetsDir, widgetName);
			if (!widgetDir.exists()) {
				throw new RuntimeException(widgetsDir.getAbsolutePath() + " is not a directory");
			}
			Map<String, List<Map<String, Object>>> data = new HashMap<>();
			for (File widgetFile : widgetDir.listFiles()) {
				String fileName = widgetFile.getName();
				if (fileName.endsWith(".sql")) {
					String queryName = fileName.substring(0, fileName.length() - 4);
					if (!validNameRegex.matcher(queryName).matches()) {
						errors.add(String.format("<strong>%s</strong> is not a valid query name", queryName));
					}
					String strQuery = IO.readContentAsString(widgetFile);
					if (strQuery == null) {
						errors.add(String.format("<strong>%s</strong> query could not be read", queryName));
					} else if (strQuery.trim().startsWith("SELECT")) {
						errors.add(String.format("<strong>%s</strong> query must start with SELECT", queryName));
					}
					List<Map<String, Object>> queryResults = App.db.dx.run(new FinalQuery(strQuery), JdbcResult.mapFactory()).limit(501).list();
					if (queryResults.size() == 501) {
						errors.add(String.format("<strong>%s</strong>: there where more than 500 rows returned", queryName));
					} else {
						data.put(queryName, queryResults);
					}
				}
			}
			if (!errors.isEmpty()) {
				res.status(400);
				return new Gson().toJson(errors);
			} else {
				result.add("configuration", objConfiguration);
				result.addProperty("name", widgetName);
				result.add("data", new JsonParser().parse(new Gson().toJson(data)));
				return new Gson().toJson(result);
			}
		});

	}

}
