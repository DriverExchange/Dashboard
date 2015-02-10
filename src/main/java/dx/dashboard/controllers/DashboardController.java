package dx.dashboard.controllers;

import dx.dashboard.App;
import dx.dashboard.tools.FreemarkerTemplateEngine;
import dx.dashboard.tools.RenderArgs;
import fr.zenexity.dbhelper.Sql;
import spark.ModelAndView;

import java.util.List;

import static spark.Spark.*;

public class DashboardController {

	public static class Result {
		public Long id;
		public String name;
		public String toString() {
			return name + "(id: " + name + ")";
		}
	}

	public static void init() {

		before((req, res) -> {
			RenderArgs.init();
			RenderArgs.put("startId", App.startId);
		});

		get("/", (req, res) -> {
			Sql.Select select = Sql.select("id", "name").from("sites").orderBy("name");
			List<Result> sites = App.db.execute(select, Result.class).list();
			RenderArgs.put("sites", sites);
			return new ModelAndView(RenderArgs.renderArgs.get(), "dashboard.ftl");
		}, new FreemarkerTemplateEngine());

	}

}
