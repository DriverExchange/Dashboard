package dx.dashboard.controllers;

import com.google.gson.*;
import dx.dashboard.App;
import dx.dashboard.tools.GroovyTemplateEngine;
import dx.dashboard.tools.IO;
import dx.dashboard.tools.RenderArgs;
import fr.zenexity.dbhelper.JdbcResult;
import fr.zenexity.dbhelper.Sql.FinalQuery;
import groovy.text.SimpleTemplateEngine;
import spark.ModelAndView;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import static spark.Spark.before;
import static spark.Spark.get;

public class DashboardController {

	public static final Pattern validNameRegex = Pattern.compile("[a-zA-Z0-9]+");

	public static File getWidgetsDir() {
		String widgetsPath = App.configuration.getProperty("dashboard.widgets.path");
		if (widgetsPath == null) {
			throw new RuntimeException("dashboard.widgets.path is undefined");
		}
		File widgetsDir = new File(widgetsPath, "src");
		if (!widgetsDir.exists()) {
			throw new RuntimeException(widgetsDir.getAbsolutePath() + " (defined in dashboard.widgets.path) was not found");
		}
		return widgetsDir;
	}

	public static JsonArray readDashboardsConfiguration() {
		List<String> errors = RenderArgs.get("errors");
		File dashboardConfFile = new File(getWidgetsDir(), "dashboards.json");
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
		File configurationFile = new File(getWidgetsDir(), widgetName + "/configuration.json");
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

		get("/modals/queryNames/:queryNames/sqlParam/:sqlParam", (req, res) -> {
			res.type("application/json");
			String[] queryNames = req.params("queryNames").split(";");
			Map<String, String> sqlParam = new HashMap();
			sqlParam.put("sqlParam", req.params("sqlParam"));

			File widgetsDir = getWidgetsDir();
			File widgetDir = new File(widgetsDir, "modals");

			if (!widgetDir.exists() && widgetDir.isDirectory()) {
				throw new RuntimeException(widgetDir.getAbsolutePath() + " is not a directory");
			}

			List<Map<String, Object>> queryResults = new ArrayList<Map<String, Object>>();
			boolean fileFound = false;
			for (String queryName : queryNames) {
				for (File widgetFile : widgetDir.listFiles()) {
					String fileName = widgetFile.getName();
					if (fileName.equals(queryName + ".ejs.sql")) {
						fileFound = true;
						String queryString = new SimpleTemplateEngine().createTemplate(widgetFile).make(sqlParam).toString();
						queryResults.add(App.db.source.run(new FinalQuery(queryString), JdbcResult.mapFactory()).limit(501).first());
					}
				}
			}
			if (!fileFound) {
				res.status(400);
				return "No modal ajax query found matching file type '.ejs.sql'";
			}
			else {
				return new Gson().toJson(queryResults);
			}

		});

		get("/widgets/:widgetName", (req, res) -> {
			res.type("application/json");
			JsonObject result = new JsonObject();
			List<String> errors = new ArrayList<>();
			String widgetName = req.params("widgetName");
			JsonObject objConfiguration = readWidgetConfiguration(widgetName);
			File widgetsDir = getWidgetsDir();
			File widgetDir = new File(widgetsDir, widgetName);
			if (!widgetDir.exists() && widgetDir.isDirectory()) {
				throw new RuntimeException(widgetDir.getAbsolutePath() + " is not a directory");
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
					} else if (!strQuery.trim().toUpperCase().startsWith("SELECT")) {
						errors.add(String.format("<strong>%s</strong> query must start with SELECT", queryName));
					}
					List<Map<String, Object>> queryResults = App.db.source.run(new FinalQuery(strQuery), JdbcResult.mapFactory()).limit(501).list();
					if (queryResults.size() == 501) {
						errors.add(String.format("<strong>%s</strong>: there where more than 500 rows returned", queryName));
					} else {
						data.put(queryName, queryResults);
					}
				}
				else if (fileName.equals("widget.ejs")) {
					result.add("template", new JsonPrimitive(IO.readContentAsString(widgetFile)));
				}
				else if (fileName.equals("widget.css")) {
					result.add("css", new JsonPrimitive(IO.readContentAsString(widgetFile)));
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
