package dx.dashboard.tools;

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

	public static Object get(String key) {
		return map().get(key);
	}

	public static Map<String, Object> map() {
		return renderArgs.get();
	}

}
