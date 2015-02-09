package dx.dashboard.controllers;

import dx.dashboard.tools.Codec;
import dx.dashboard.tools.FreemarkerTemplateEngine;
import dx.dashboard.tools.RenderArgs;
import spark.ModelAndView;

import static spark.Spark.*;

public class DashboardController {

	public static void init() {

		before((req, res) -> {
			RenderArgs.init();
			RenderArgs.put("test", Codec.UUID());
		});

		get("/", (req, res) -> {
			RenderArgs.put("hello", "Hello World!");
			return new ModelAndView(RenderArgs.renderArgs.get(), "dashboard.ftl");
		}, new FreemarkerTemplateEngine());

	}

}
