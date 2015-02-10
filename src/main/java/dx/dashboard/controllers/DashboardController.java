package dx.dashboard.controllers;

import dx.dashboard.App;
import dx.dashboard.tools.FreemarkerTemplateEngine;
import dx.dashboard.tools.RenderArgs;
import spark.ModelAndView;

import static spark.Spark.*;

public class DashboardController {

	public static void init() {

		before((req, res) -> {
			RenderArgs.init();
			RenderArgs.put("startId", App.startId);
		});

		get("/", (req, res) -> {
			return new ModelAndView(RenderArgs.renderArgs.get(), "dashboard.ftl");
		}, new FreemarkerTemplateEngine());

	}

}
