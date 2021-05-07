/*
 * Copyright (c) 2018-2019 The University of Manchester
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
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.slf4j.LoggerFactory.getLogger;
import static org.sqlite.SQLiteConfig.SynchronousMode.NORMAL;
import static org.sqlite.SQLiteConfig.TransactionMode.IMMEDIATE;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.sqlite.SQLiteConfig;

/**
 * The database engine interface. Based on SQLite.
 *
 * @author Donal Fellows
 */
public class DatabaseEngine {
	private static final Logger log = getLogger(DatabaseEngine.class);

	private static String sqlDDL;

	static {
		try {
			sqlDDL = resourceToString("/spalloc.sql", UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("failed to read database definition SQL",
					e);
		}
	}

	/** Busy timeout for SQLite, in milliseconds. */
	private static final int BUSY_TIMEOUT = 500;

	private boolean initialised;

	private String dbConnectionUrl;

	private SQLiteConfig config = new SQLiteConfig();

	/**
	 * Create an engine interface for a particular database.
	 *
	 * @param dbFile
	 *            The file containing the database.
	 */
	public DatabaseEngine(File dbFile) {
		this.dbConnectionUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
		log.info("will manage database at " + dbFile.getAbsolutePath());
		config.enforceForeignKeys(true);
		config.setSynchronous(NORMAL);
		config.setBusyTimeout(BUSY_TIMEOUT);
		config.setTransactionMode(IMMEDIATE);
		config.setDateClass("INTEGER");
	}

	public Connection getConnection() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("opening database connection {}", dbConnectionUrl);
		}

		Connection conn = config.createConnection(dbConnectionUrl);
		synchronized (this) {
			if (!initialised) {
				try (Statement statement = conn.createStatement()) {
					statement.executeUpdate(sqlDDL);
				}
				initialised = true;
			}
		}
		return conn;
	}

	public static void setParams(PreparedStatement s, Object... arguments)
			throws SQLException {
		int idx = 0;
		for (Object arg : arguments) {
			s.setObject(++idx, arg);
		}
	}

	public static int runUpdate(PreparedStatement s, Object... arguments)
			throws SQLException {
		int idx = 0;
		for (Object arg : arguments) {
			s.setObject(++idx, arg);
		}
		return s.executeUpdate();
	}

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
		void act() throws SQLException;
	}

	/**
	 * Some code that may be run within a transaction that returns a result.
	 */
	@FunctionalInterface
	public interface TransactedWithResult<T> {
		T act() throws SQLException;
	}
}
