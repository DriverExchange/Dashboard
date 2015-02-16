package dx.dashboard.tools;

import dx.dashboard.App;
import dx.dashboard.Logger;
import spark.ModelAndView;
import spark.TemplateEngine;
import groovy.text.SimpleTemplateEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

public class GroovyTemplateEngine extends TemplateEngine {

	public String render(ModelAndView modelAndView) {
		try {
			SimpleTemplateEngine engine = new SimpleTemplateEngine();
			InputStream templateIs;
			if (App.isDevMode()) {
				File templateFile = new File("src/main/resources/views/" + modelAndView.getViewName());
				if (!templateFile.exists()) {
					throw new RuntimeException(templateFile.getAbsolutePath() + " was not found");
				}
				templateIs = new FileInputStream(templateFile);
			}
			else {
				templateIs = ClassLoader.getSystemResourceAsStream("views/" + modelAndView.getViewName());
			}
			return engine
				.createTemplate(IO.readContentAsString(templateIs))
				.make((Map) modelAndView.getModel())
				.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
