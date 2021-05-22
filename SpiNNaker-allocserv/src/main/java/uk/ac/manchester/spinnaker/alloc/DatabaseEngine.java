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
import static uk.ac.manchester.spinnaker.storage.threading.OneThread.threadBound;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
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

	/**
	 * Run some SQL where the result is of no interest.
	 *
	 * @param conn
	 *            The connection.
	 * @param sql
	 *            The SQL to run. Probably DDL.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public static void exec(Connection conn, String sql) throws SQLException {
		try (Statement s = conn.createStatement()) {
			s.execute(sql);
		}
	}

	/**
	 * Run some SQL where the result is of no interest.
	 *
	 * @param conn
	 *            The connection.
	 * @param sql
	 *            Reference to the SQL to run. Probably DDL.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public static void exec(Connection conn, Resource sqlResource)
			throws SQLException {
		exec(conn, readSQL(sqlResource));
	}

	/**
	 * Wrapping a prepared query to be more suitable for Java 8 onwards.
	 *
	 * @author Donal Fellows
	 */
	public static final class Query implements AutoCloseable {
		private final PreparedStatement s;

		private ResultSet rs;

		private Query(Connection conn, String sql) throws SQLException {
			s = conn.prepareStatement(sql);
			rs = null;
		}

		private void closeResults() {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException ignored) {
				}
				rs = null;
			}
		}

		/**
		 * Run the query on the given arguments.
		 *
		 * @param arguments
		 *            Positional argument to the query
		 * @return The results, wrapped as a one-shot iterable
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		public Iterable<ResultSet> call(Object... arguments)
				throws SQLException {
			int idx = 0;
			for (Object arg : arguments) {
				s.setObject(++idx, arg);
			}
			closeResults();
			rs = s.executeQuery();
			return () -> new Iterator<ResultSet>() {
				private boolean finished = false;

				@Override
				public boolean hasNext() {
					boolean result = false;
					if (finished) {
						return false;
					}
					try {
						result = rs.next();
						if (!result) {
							closeResults();
						}
					} catch (SQLException e) {
						result = false;
					} finally {
						if (!result) {
							finished = true;
							closeResults();
						}
					}
					return result;
				}

				@Override
				public ResultSet next() {
					if (finished) {
						return null;
					}
					return rs;
				}
			};
		}

		@Override
		public void close() throws SQLException {
			closeResults();
			s.close();
		}
	}

	// @formatter:off
	/**
	 * Create a new query. Usage pattern:
	 * <pre>
	 * try (Query q = query(conn, SQL_SELECT)) {
	 *     for (ResultSet row : u.call(argument1, argument2)) {
	 *         // Do something with the row
	 *     }
	 * }
	 * </pre>
	 *
	 * @param conn
	 *            The connection.
	 * @param sql
	 *            The SQL of the query.
	 * @return The query object.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	// @formatter:on
	public static Query query(Connection conn, String sql) throws SQLException {
		return new Query(conn, sql);
	}

	// @formatter:off
	/**
	 * Create a new query.
	 * <pre>
	 * try (Query q = query(conn, sqlSelectResource)) {
	 *     for (ResultSet row : u.call(argument1, argument2)) {
	 *         // Do something with the row
	 *     }
	 * }
	 * </pre>
	 *
	 * @param conn
	 *            The connection.
	 * @param sqlResource
	 *            Reference to the SQL of the query.
	 * @return The query object.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	// @formatter:on
	public static Query query(Connection conn, Resource sqlResource)
			throws SQLException {
		return new Query(conn, readSQL(sqlResource));
	}

	/**
	 * Wrapping a prepared update to be more suitable for Java 8 onwards.
	 *
	 * @author Donal Fellows
	 */
	public static final class Update implements AutoCloseable {
		private final PreparedStatement s;

		private ResultSet rs;

		private Update(Connection conn, String sql) throws SQLException {
			s = conn.prepareStatement(sql);
			rs = null;
		}

		/**
		 * Run the update on the given arguments.
		 *
		 * @param arguments
		 *            Positional argument to the query
		 * @return The number of rows updated
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		public int call(Object... arguments) throws SQLException {
			int idx = 0;
			for (Object arg : arguments) {
				s.setObject(++idx, arg);
			}
			closeResults();
			return s.executeUpdate();
		}

		@Override
		public void close() throws SQLException {
			closeResults();
			s.close();
		}

		/**
		 * Run the update on the given arguments.
		 *
		 * @param arguments
		 *            Positional arguments to the query
		 * @return The keys generated by the update; the keys will be the first
		 *         item in each row.
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		public Iterable<Integer> keys(Object... arguments)
				throws SQLException {
			/*
			 * In theory, the statement should have been prepared with the
			 * GET_GENERATED_KEYS flag set. In practice, the SQLite driver
			 * ignores that flag.
			 */
			int idx = 0;
			for (Object arg : arguments) {
				s.setObject(++idx, arg);
			}
			closeResults();
			s.executeUpdate();
			rs = s.getGeneratedKeys();
			return () -> new Iterator<Integer>() {
				private boolean finished = false;

				@Override
				public boolean hasNext() {
					boolean result = false;
					if (finished) {
						return false;
					}
					try {
						result = rs.next();
						if (!result) {
							closeResults();
						}
					} catch (SQLException e) {
						result = false;
					} finally {
						if (!result) {
							finished = true;
							closeResults();
						}
					}
					return result;
				}

				@Override
				public Integer next() {
					if (finished) {
						return null;
					}
					try {
						return (Integer) rs.getObject(1);
					} catch (SQLException e) {
						closeResults();
						finished = true;
						return null;
					}
				}
			};
		}

		private void closeResults() {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException ignored) {
				}
				rs = null;
			}
		}
	}

	// @formatter:off
	/**
	 * Create a new update. Usage pattern:
	 * <pre>
	 * try (Update u = update(conn, SQL_UPDATE)) {
	 *     int numRows = u.call(argument1, argument2);
	 * }
	 * </pre>
	 * or:
	 * <pre>
	 * try (Update u = update(conn, SQL_INSERT)) {
	 *     for (int key : u.keys(argument1, argument2)) {
	 *         // Do something with the key
	 *     }
	 * }
	 * </pre>
	 *
	 * @param conn
	 *            The connection.
	 * @param sql
	 *            The SQL of the update.
	 * @return The update object.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	// @formatter:on
	public static Update update(Connection conn, String sql)
			throws SQLException {
		return new Update(conn, sql);
	}

	// @formatter:off
	/**
	 * Create a new update.
	 * <pre>
	 * try (Update u = update(conn, sqlUpdateResource)) {
	 *     int numRows = u.call(argument1, argument2);
	 * }
	 * </pre>
	 * or:
	 * <pre>
	 * try (Update u = update(conn, sqlInsertResource)) {
	 *     for (int key : u.keys(argument1, argument2)) {
	 *         // Do something with the key
	 *     }
	 * }
	 * </pre>
	 *
	 * @param conn
	 *            The connection.
	 * @param sqlResource
	 *            Reference to the SQL of the update.
	 * @return The update object.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	// @formatter:on
	public static Update update(Connection conn, Resource sqlResource)
			throws SQLException {
		return new Update(conn, readSQL(sqlResource));
	}
}
