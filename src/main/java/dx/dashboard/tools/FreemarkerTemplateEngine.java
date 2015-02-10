package dx.dashboard.tools;

import dx.dashboard.App;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import spark.ModelAndView;
import spark.TemplateEngine;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

public class FreemarkerTemplateEngine extends TemplateEngine {

	public final freemarker.template.Configuration configuration;
	public FreemarkerTemplateEngine() {
		try {
			configuration = new freemarker.template.Configuration();
			if (App.isDevMode()) {
				configuration.setDirectoryForTemplateLoading(new File("src/main/resources/views"));
			} else {
				File viewDirectory = Tools.getResourceAsFile("views");
				configuration.setDirectoryForTemplateLoading(viewDirectory);
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String render(ModelAndView modelAndView) {
		try {
			StringWriter stringWriter = new StringWriter();
			Template template = configuration.getTemplate(modelAndView.getViewName());
			template.process(modelAndView.getModel(), stringWriter);
			return stringWriter.toString();
		} catch (IOException | TemplateException e) {
			throw new RuntimeException(e);
		}
	}

}
