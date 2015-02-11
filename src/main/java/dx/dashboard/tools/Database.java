package dx.dashboard.tools;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import dx.dashboard.App;
import dx.dashboard.Logger;
import fr.zenexity.dbhelper.Jdbc;
import fr.zenexity.dbhelper.JdbcException;
import fr.zenexity.dbhelper.JdbcIterator;
import fr.zenexity.dbhelper.JdbcResult;
import fr.zenexity.dbhelper.JdbcStatementException;
import fr.zenexity.dbhelper.Sql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class Database {

	final Jdbc jdbc;
	public static DataSource datasource;

	public static void initDataSource(String dbConfName) {
		try {

			String prefix = "db." + dbConfName;
			Properties p = App.configuration;

			// Try the driver
			String driver = p.getProperty(prefix + ".driver");
			try {
				Class.forName(driver).newInstance();
			} catch (Exception e) {
				throw new Exception("Driver not found (" + driver + ")");
			}

			// Try the connection
			Connection fake = null;
			try {
				if (App.configuration.getProperty(prefix + ".user") == null) {
					fake = DriverManager.getConnection(p.getProperty(prefix + ".url"));
				} else {
					fake = DriverManager.getConnection(p.getProperty(prefix + ".url"), p.getProperty(prefix + ".user"), p.getProperty(prefix + ".pass"));
				}
			} finally {
				if (fake != null) {
					fake.close();
				}
			}

			System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
			System.setProperty("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "OFF");
			ComboPooledDataSource ds = new ComboPooledDataSource();
			ds.setDriverClass(p.getProperty(prefix + ".driver"));
			ds.setJdbcUrl(p.getProperty(prefix + ".url"));
			ds.setUser(p.getProperty(prefix + ".user"));
			ds.setPassword(p.getProperty(prefix + ".pass"));
			ds.setAcquireRetryAttempts(10);
			ds.setCheckoutTimeout(Integer.parseInt(p.getProperty(prefix + ".pool.timeout", "5000")));
			ds.setBreakAfterAcquireFailure(false);
			ds.setMaxPoolSize(Integer.parseInt(p.getProperty(prefix + ".pool.maxSize", "30")));
			ds.setMinPoolSize(Integer.parseInt(p.getProperty(prefix + ".pool.minSize", "1")));
			ds.setMaxIdleTimeExcessConnections(Integer.parseInt(p.getProperty(prefix + ".pool.maxIdleTimeExcessConnections", "0")));
			ds.setIdleConnectionTestPeriod(10);
			ds.setTestConnectionOnCheckin(true);

			datasource = ds;
			Connection c = null;
			try {
				c = ds.getConnection();
			} finally {
				if (c != null) {
					c.close();
				}
			}
			Logger.info("Connected to " + ds.getJdbcUrl());

		} catch (Exception e) {
			datasource = null;
			Logger.error("Cannot connected to the database : " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public Database(String dbConfName) {
		initDataSource(dbConfName);
		try {
			jdbc = new Jdbc(datasource.getConnection());
		} catch (SQLException e) {
			Logger.error("Cannot obtain a new connection (" + e.getMessage() + ")");
			throw new RuntimeException(e);
		}
	}

	public void begin() throws SQLException {
		Logger.debug("BEGIN");
		jdbc.connection.setAutoCommit(false);
	}

	public void commit() throws SQLException {
		Logger.debug("COMMIT");
		jdbc.connection.setAutoCommit(true);
	}

	public <T> JdbcIterator<T> run(Sql.Query query, JdbcResult.Factory<T> resultFactory) throws JdbcException {
		Logger.debug(Sql.resolve(query));
		return jdbc.execute(query, resultFactory);
	}

	public <T> JdbcIterator<T> run(Sql.Query query, Class<T> resultClass) throws JdbcException {
		Logger.debug(Sql.resolve(query));
		return jdbc.execute(query, resultClass);
	}

	public <T> JdbcIterator<T> run(Sql.Query query, Class<T> resultClass, String... fields) throws JdbcException {
		Logger.debug(Sql.resolve(query));
		return jdbc.execute(query, resultClass, fields);
	}

	public <T> JdbcIterator<T> run(Sql.Query query, Class<T> resultClass, List<String> fields) throws JdbcException {
		Logger.debug(Sql.resolve(query));
		return jdbc.execute(query, resultClass, fields);
	}

	public <T> JdbcIterator<T> run(Sql.Query query, int offset, int size, JdbcResult.Factory<T> resultFactory) throws JdbcException {
		Logger.debug(Sql.resolve(query));
		return jdbc.execute(query, offset, size, resultFactory);
	}

	public <T> JdbcIterator<T> run(Sql.Query query, int offset, int size, Class<T> resultClass) throws JdbcException {
		Logger.debug(Sql.resolve(query));
		return jdbc.execute(query, offset, size, resultClass);
	}

	public <T> JdbcIterator<T> run(Sql.Query query, int offset, int size, Class<T> resultClass, String... fields) throws JdbcException {
		Logger.debug(Sql.resolve(query));
		return jdbc.execute(query, offset, size, resultClass, fields);
	}

	public <T> JdbcIterator<T> run(Sql.Query query, int offset, int size, Class<T> resultClass, List<String> fields) throws JdbcException {
		Logger.debug(Sql.resolve(query));
		return jdbc.execute(query, offset, size, resultClass, fields);
	}

	public int runUpdate(String query, Object... params) throws JdbcStatementException {
		Logger.debug(query);
		return jdbc.executeUpdate(query, params);
	}

	public int run(Sql.UpdateQuery query) throws JdbcStatementException {
		Logger.debug(Sql.resolve(query));
		return jdbc.execute(query);
	}

}
