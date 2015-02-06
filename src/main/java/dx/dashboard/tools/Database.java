package dx.dashboard.tools;

import fr.zenexity.dbhelper.Jdbc;

import java.sql.Connection;
import java.sql.DriverManager;

public class Database {

	public static Jdbc jdbc;

	public Database(String jdbcDriver, String jdbcURL) {
		String _jdbcDriver = jdbcDriver == null ? System.getProperty("...") : jdbcDriver;
		String _jdbcURL = jdbcURL == null ? System.getProperty("...") : jdbcURL;
		if (_jdbcDriver == null || _jdbcDriver.isEmpty()) {
			throw new RuntimeException("jdbcDriver is undefined");
		}
		if (_jdbcURL == null || _jdbcURL.isEmpty()) {
			throw new RuntimeException("jdbcURL is undefined");
		}
		jdbc = new Jdbc(getConnection(_jdbcDriver, _jdbcURL));
	}

	public static Connection getConnection(String driverClass, String connectionURL) {
		try {
			Class.forName(driverClass);
			return DriverManager.getConnection(connectionURL);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
