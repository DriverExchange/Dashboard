package dx.dashboard;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import dx.dashboard.controllers.AssetsController;
import dx.dashboard.controllers.DashboardController;
import dx.dashboard.tools.Codec;
import dx.dashboard.tools.Database;
import dx.dashboard.tools.GroovyTemplateEngine;
import dx.dashboard.tools.RenderArgs;
import fr.zenexity.dbhelper.Sql;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import static spark.Spark.*;

public class App {

	protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(App.class);

	public static final String startId;
	static {
		startId = Codec.hexMD5(Codec.UUID()).substring(0, 7);
		Logger.info("startId: %s", startId);
	}

	public static final Properties configuration = new Properties();
	static {
		try {
			InputStream configurationIs;
			File localConfigurationFile = new File("application.properties");
			if (localConfigurationFile.exists()) {
				Logger.info("Using local application.properties (in the project root) instead of the one in src/main/resource");
				configurationIs = new FileInputStream(localConfigurationFile);
			}
			else {
				Logger.info("Using default application.properties in src/main/resource");
				configurationIs = ClassLoader.getSystemResourceAsStream("application.properties");
			}
			if (configurationIs == null) {
				Logger.error("application.conf file cannot be found");
			}
			configuration.load(configurationIs);
		} catch (IOException e) {
			throw new RuntimeException("Error while loading application.conf", e);
		}
	}

	public static class Databases {
		public Database source = new Database("source");
	}

	public static final Databases db = new Databases();

	public static boolean isDevMode() {
		return configuration.getProperty("mode", "prod").equals("dev");
	}

	public static void main(String[] args) {
		if (isDevMode()) {
			AssetsController.initDevStaticFile();
		}
		else {
			staticFileLocation("static");
		}

		before((req, res) -> {
			req.session(true);
			if (!req.uri().equals("/login")) {
				Long userId = req.session().attribute("userId");
				if (userId == null) {
					String xRequestedWith = req.headers("X-Requested-With");
					boolean isAjax = xRequestedWith != null && xRequestedWith.equals("XMLHttpRequest");
					boolean isHtml = req.headers("Accept").contains("text/html");
					if (isHtml) {
						res.redirect("/login");
					}
					if (isAjax) {
						res.status(403);
					}
				}
			}
		});

		get("/login", (req, res) -> {
			Long userId = req.session().attribute("userId");
			if (userId != null) {
				res.redirect("/");
			}
			return new ModelAndView(RenderArgs.map(), "login.html");
		}, new GroovyTemplateEngine());

		post("/login", (req, res) -> {
			String email = req.queryParams("email");
			String password = req.queryParams("password");

			String loginApiUrl = App.configuration.getProperty("login_api.url");
			String loginApiUsername = App.configuration.getProperty("login_api.username");
			String loginApiPassword = App.configuration.getProperty("login_api.password");

			Map<String, Object> result = new HashMap<>();

			if (loginApiUrl == null || loginApiUrl.isEmpty()) {
				Logger.error("'login_api.url' isn't defined in application.properties");
				result.put("error", "The login API isn't configured");
			}

			else if (loginApiUsername == null || loginApiUsername.isEmpty()) {
				Logger.error("'login_api.username' isn't defined in application.properties");
				result.put("error", "The login API isn't configured");
			}

			else if (loginApiPassword == null || loginApiPassword.isEmpty()) {
				Logger.error("'login_api.password' isn't defined in application.properties");
				result.put("error", "The login API isn't configured");
			}

			else if (email == null || email.isEmpty()) {
				result.put("error", "Email required");
			}

			else if (password == null || password.isEmpty()) {
				result.put("error", "Password required");
			}

			else {

				AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
				Future<Response> futureResponse = asyncHttpClient
					.preparePost(loginApiUrl)
					.addHeader("Authorization", "Basic " + Codec.encodeBASE64(loginApiUsername + ":" + loginApiPassword))
					.addFormParam("email", email)
					.addFormParam("password", password)
					.execute();

				Response response = futureResponse.get();

				if (response.getStatusCode() == 200) {
					String body = response.getResponseBody();
					JsonObject obj = (JsonObject) new JsonParser().parse(body);
					Long userId = obj.getAsJsonPrimitive("userId").getAsNumber().longValue();
					req.session().attribute("userId", userId);
				}
				else if (response.getStatusCode() == 403) {
					result.put("password", "Wrong password");
				}
				else {
					result.put("error", "Login API error (" + response.getStatusCode() + ")");
				}
			}

			if (!result.isEmpty()) {
				res.status(400);
				return new Gson().toJson(result);
			}
			else {
				req.session().attribute("login", email);
				return "{}";
			}
		});

		AssetsController.init();
		DashboardController.init();

		exception(RuntimeException.class, (e, req, res) -> {
			res.status(500);
			res.body("<pre style=\"color: #c00; white-space: pre-wrap\">" + ExceptionUtils.getStackTrace(e) + "</pre>");
		});
	}

}
