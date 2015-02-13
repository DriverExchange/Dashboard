package dx.dashboard.tools;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class RenderArgs {

	public final static ThreadLocal<Map<String, Object>> renderArgs = new ThreadLocal<>();

	public static void init() {
		Map<String, Object> args = new HashMap<>();
		renderArgs.set(args);
	}

	public static void put(String key, Object value) {
		map().put(key, value);
	}

	public static <T> T get(String key) {
		return (T) map().get(key);
	}

	public static Map<String, Object> map() {
		return renderArgs.get();
	}

	public static final Gson gson = new Gson();
	public static void addJsData(String name, Object value) {
		Map<String, Object> jsData = RenderArgs.get("jsData");
		if (jsData == null) {
			jsData = new HashMap<>();
			RenderArgs.put("jsData", jsData);
		}
		jsData.put(name, gson.toJson(value));
	}

}
