/*
 * Copyright (c) 2018-2021 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.alloc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.slf4j.LoggerFactory.getLogger;
import static org.sqlite.SQLiteConfig.SynchronousMode.NORMAL;
import static org.sqlite.SQLiteConfig.TransactionMode.IMMEDIATE;
import static uk.ac.manchester.spinnaker.alloc.OneThread.threadBound;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.sqlite.SQLiteConfig;

/**
 * The database engine interface. Based on SQLite.
 *
 * @author Donal Fellows
 */
@Component
public class DatabaseEngine {
	private static final Logger log = getLogger(DatabaseEngine.class);

	private static final Map<Resource, String> QUERY_CACHE = new HashMap<>();

	/** Busy timeout for SQLite, in milliseconds. */
	private static final int BUSY_TIMEOUT = 500;

	private boolean initialised;

	private String dbConnectionUrl;

	private SQLiteConfig config = new SQLiteConfig();

	@Value("classpath:/spalloc.sql")
	private Resource sqlDDLFile;

	private String sqlDDL;

	private File dbFile;

	@PostConstruct
	private void loadDDL() throws SQLException {
		sqlDDL = readSQL(sqlDDLFile);
	}

	/**
	 * Create an engine interface for a particular database.
	 *
	 * @param dbFile
	 *            The file containing the database.
	 */
	public DatabaseEngine(@Value("${databasePath:db.sqlite3}") File dbFile) {
		this.dbFile = dbFile.getAbsoluteFile();
		this.dbConnectionUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
		log.info("will manage database at {}", dbFile.getAbsolutePath());
		config.enforceForeignKeys(true);
		config.setSynchronous(NORMAL);
		config.setBusyTimeout(BUSY_TIMEOUT);
		config.setTransactionMode(IMMEDIATE);
		config.setDateClass("INTEGER");
	}

	public Connection getConnection() throws SQLException {
		log.debug("opening database connection {}", dbConnectionUrl);
		synchronized (this) {
			boolean doInit = !initialised || !dbFile.exists();
			Connection conn = config.createConnection(dbConnectionUrl);
			if (doInit) {
				try (Statement statement = conn.createStatement()) {
					log.info("initalising DB from {}", sqlDDLFile);
					statement.executeUpdate(sqlDDL);
				}
				initialised = true;
			}
			return threadBound(conn);
		}
	}

	public static void setParams(PreparedStatement s, Object... arguments)
			throws SQLException {
		int idx = 0;
		for (Object arg : arguments) {
			s.setObject(++idx, arg);
		}
	}

	/**
	 * Set the arguments and run an SQL "update" (DML) statement.
	 *
	 * @param s
	 *            The statement to run
	 * @param arguments
	 *            The arguments to supply to the statement
	 * @return The number of affected rows
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public static int runUpdate(PreparedStatement s, Object... arguments)
			throws SQLException {
		int idx = 0;
		for (Object arg : arguments) {
			s.setObject(++idx, arg);
		}
		return s.executeUpdate();
	}

	/**
	 * Set the arguments and run an SQL "query" (DQL) statement.
	 *
	 * @param s
	 *            The statement to run
	 * @param arguments
	 *            The arguments to supply to the statement
	 * @return The result set of the query
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public static ResultSet runQuery(PreparedStatement s, Object... arguments)
			throws SQLException {
		int idx = 0;
		for (Object arg : arguments) {
			s.setObject(++idx, arg);
		}
		return s.executeQuery();
	}

	/**
	 * A nestable transaction runner. If the {@code operation} completes
	 * normally (and this isn't a nested use), the transaction commits. If an
	 * exception is thrown, the transaction is rolled back.
	 *
	 * @param conn
	 *            The database connection
	 * @param operation
	 *            The operation to run
	 * @throws SQLException
	 *             If something goes wrong with database access.
	 * @throws RuntimeException
	 *             If something other than database access goes wrong with the
	 *             contained code.
	 */
	public static void transaction(Connection conn, Transacted operation)
			throws SQLException {
		if (!conn.getAutoCommit()) {
			// Already in a transaction; just run the operation
			operation.act();
			return;
		}
		conn.setAutoCommit(false);
		boolean done = false;
		try {
			operation.act();
			conn.commit();
			done = true;
		} catch (SQLException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
		} finally {
			if (!done) {
				conn.rollback();
			}
		}
	}

	/**
	 * A nestable transaction runner. If the {@code operation} completes
	 * normally (and this isn't a nested use), the transaction commits. If an
	 * exception is thrown, the transaction is rolled back.
	 *
	 * @param <T>
	 *            The type of the result
	 * @param conn
	 *            The database connection
	 * @param operation
	 *            The operation to run
	 * @return the value returned by {@code operation}
	 * @throws SQLException
	 *             If something goes wrong with database access.
	 * @throws RuntimeException
	 *             If something other than database access goes wrong with the
	 *             contained code.
	 */
	public static <T> T transaction(Connection conn,
			TransactedWithResult<T> operation) throws SQLException {
		if (!conn.getAutoCommit()) {
			// Already in a transaction; just run the operation
			return operation.act();
		}
		conn.setAutoCommit(false);
		boolean done = false;
		try {
			T result = operation.act();
			conn.commit();
			done = true;
			return result;
		} catch (SQLException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
		} finally {
			if (!done) {
				conn.rollback();
			}
		}
	}

	/**
	 * Some code that may be run within a transaction.
	 */
	@FunctionalInterface
	public interface Transacted {
		/**
		 * The operation to run.
		 *
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		void act() throws SQLException;
	}

	/**
	 * Some code that may be run within a transaction that returns a result.
	 *
	 * @param <T>
	 *            The type of the result of the code.
	 */
	@FunctionalInterface
	public interface TransactedWithResult<T> {
		/**
		 * The operation to run.
		 *
		 * @return The result of the operation.
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		T act() throws SQLException;
	}

	/**
	 * Simple reader that loads a complex SQL query from a file.
	 *
	 * @param resource
	 *            The resource to load from
	 * @return The content of the resource
	 * @throws SQLException
	 *             If anything goes wrong
	 */
	public static String readSQL(Resource resource) throws SQLException {
		synchronized (QUERY_CACHE) {
			if (QUERY_CACHE.containsKey(resource)) {
				return QUERY_CACHE.get(resource);
			}
		}
		try {
			String s = readFileToString(resource.getFile(), UTF_8);
			synchronized (QUERY_CACHE) {
				// Not really a problem if it is put in twice
				QUERY_CACHE.put(resource, s);
			}
			return s;
		} catch (IOException e) {
			throw new SQLException("could not load SQL file from " + resource,
					e);
		}
	}
}
