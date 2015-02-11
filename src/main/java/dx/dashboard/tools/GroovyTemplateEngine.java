package dx.dashboard.tools;

import spark.ModelAndView;
import spark.TemplateEngine;
import groovy.text.SimpleTemplateEngine;

import java.io.File;
import java.util.Map;

public class GroovyTemplateEngine extends TemplateEngine {

	public String render(ModelAndView modelAndView) {
		SimpleTemplateEngine engine = new SimpleTemplateEngine();
		File templateFile = new File("src/main/resources/views/" + modelAndView.getViewName());
		if (!templateFile.exists()) {
			throw new RuntimeException(templateFile.getAbsolutePath() + " was not found");
		}
		try {
			return engine
				.createTemplate(IO.readContentAsString(templateFile))
				.make((Map) modelAndView.getModel())
				.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
