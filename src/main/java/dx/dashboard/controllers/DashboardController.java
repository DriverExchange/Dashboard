package dx.dashboard.controllers;

import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import static spark.Spark.*;

import java.util.HashMap;
import java.util.Map;

public class DashboardController {

	public static void init() {

		get("/", (req, res) -> {
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("hello", "Hello World!");
			return new ModelAndView(attributes, "dashboard.ftl");
		}, new FreeMarkerEngine());

	}

}
