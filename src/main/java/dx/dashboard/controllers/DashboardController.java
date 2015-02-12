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

	public static void init() {

		final String widgetsPath = App.configuration.getProperty("dashboard.widgets.path");
		if (widgetsPath == null) {
			throw new RuntimeException("dashboard.widgets.path is undefined");
		}

		final File widgetsDir = new File(widgetsPath, "src");
		if (!widgetsDir.exists()) {
			throw new RuntimeException(widgetsDir.getAbsolutePath() + " (defined in dashboard.widgets.path) was not found");
		}

		final Pattern validNameRegex = Pattern.compile("[a-zA-Z0-9]+");

		before((req, res) -> {
			RenderArgs.init();
			RenderArgs.put("startId", App.startId);
		});

		get("/", (req, res) -> {
			List<String> errors = new ArrayList<>();
			JsonArray dashboardConf = null;
			Map<String, String> widgetTitles = new HashMap<>();
			File dashboardConfFile = new File(widgetsDir, "dashboard.json");
			if (!dashboardConfFile.exists()) {
				errors.add(dashboardConfFile.getAbsolutePath() + " not found");
			}
			try {
				dashboardConf = (JsonArray) new JsonParser().parse(IO.readContentAsString(dashboardConfFile));
			} catch (JsonSyntaxException e) {
				errors.add(String.format("Malformed JSON in dashboard.json"));
			}
			if (dashboardConf != null) {
				Iterator<JsonElement> colsIt = dashboardConf.iterator();
				while (colsIt.hasNext()) {
					JsonArray widgetNames = colsIt.next().getAsJsonArray();
					Iterator<JsonElement> widgetNamesIt = widgetNames.iterator();
					while (widgetNamesIt.hasNext()) {
						String widgetName = widgetNamesIt.next().getAsString();
						File configurationFile = new File(widgetsDir, widgetName + "/configuration.json");
						if (configurationFile.exists()) {
							try {
								JsonObject configuration = (JsonObject) new JsonParser().parse(IO.readContentAsString(configurationFile));
								widgetTitles.put(widgetName, configuration.get("title").getAsString());
							} catch (JsonSyntaxException e) {
								errors.add(String.format("<strong>%s</strong>: malformed JSON in configuration.json", widgetName));
							}
						}
					}
				}
			}
			Gson gson = new Gson();
			RenderArgs.put("errors", errors);
			RenderArgs.put("dashboardConfJson", gson.toJson(dashboardConf));
			RenderArgs.put("widgetTitlesJson", gson.toJson(widgetTitles));
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
			Map<String, List<Map<String, Object>>> data = new HashMap<>();
			for (File widgetFile : widgetDir.listFiles()) {
				String fileName = widgetFile.getName();
				if (fileName.equals("configuration.json")) {
					String strConfiguration = IO.readContentAsString(widgetFile);
					try {
						JsonObject objConfiguration = (JsonObject) new JsonParser().parse(strConfiguration);
						result.add("configuration", objConfiguration);
					} catch (JsonSyntaxException e) {
						errors.add("Malformed JSON in configuration.json");
					}
				} else if (fileName.endsWith(".sql")) {
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
				result.addProperty("name", widgetName);
				result.add("data", new JsonParser().parse(new Gson().toJson(data)));
				return new Gson().toJson(result);
			}
		});

	}

}
