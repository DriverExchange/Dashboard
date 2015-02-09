package dx.dashboard;

public class Logger {

	public static void debug(String str, Object... obj) {
		App.logger.debug(String.format(str, obj));
	}

	public static void info(String str, Object... obj) {
		App.logger.info(String.format(str, obj));
	}

	public static void warn(String str, Object... obj) {
		App.logger.warn(String.format(str, obj));
	}

	public static void error(String str, Object... obj) {
		App.logger.error(String.format(str, obj));
	}

}

