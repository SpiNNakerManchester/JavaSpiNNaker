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

import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.util.Objects.requireNonNull;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.slf4j.LoggerFactory.getLogger;
import static org.sqlite.SQLiteConfig.SynchronousMode.NORMAL;
import static org.sqlite.SQLiteConfig.TransactionMode.IMMEDIATE;
import static org.sqlite.SQLiteErrorCode.SQLITE_BUSY;
import static uk.ac.manchester.spinnaker.storage.threading.OneThread.uncloseableThreadBound;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.sqlite.Function;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteException;

import uk.ac.manchester.spinnaker.storage.ResultColumn;
import uk.ac.manchester.spinnaker.storage.SingleRowResult;

/**
 * The database engine interface. Based on SQLite. Manages a pool of database
 * connections, each of which is bound to a specific thread. The connections are
 * closed when the thread in question terminates. <em>Assumes</em> that you are
 * using a thread pool.
 * <p>
 * Key configuration properties:
 * <dl>
 * <dt>sqlite.analysis_limit
 * <dd>The <a href="https://sqlite.org/lang_analyze.html">analysis limit</a>.
 * Defaults to {@code 400}.
 * <dt>sqlite.timeout
 * <dd>The timeout for waiting for a transaction. Defaults to {@code PT1S} (a
 * {@link Duration}).
 * </dl>
 *
 * @author Donal Fellows
 */
@Component
public final class DatabaseEngine extends DatabaseCache<SQLiteConnection> {
	private static final Logger log = getLogger(DatabaseEngine.class);

	@ResultColumn("c")
	@SingleRowResult
	private static final String COUNT_MOVEMENTS =
			"SELECT count(*) AS c FROM movement_directions";

	private static final Map<Resource, String> QUERY_CACHE = new HashMap<>();

	// From https://sqlite.org/lang_analyze.html
	// These are special operations
	private static final String OPTIMIZE_DB =
			"PRAGMA analysis_limit=%d; PRAGMA optimize;";

	private static final int DEFAULT_ANALYSIS_LIMIT = 400;

	/** Used to validate the database contents. */
	private static final int EXPECTED_NUM_MOVEMENTS = 6;

	private final Path dbPath;

	private final String dbConnectionUrl;

	private final SQLiteConfig config = new SQLiteConfig();

	private boolean initialised;

	/**
	 * Control the amount of resources used for auto-optimisation.
	 *
	 * @see <a href="https://sqlite.org/lang_analyze.html">SQLite docs</a>
	 */
	@Value("${sqlite.analysis_limit:" + DEFAULT_ANALYSIS_LIMIT + "}")
	private int analysisLimit = DEFAULT_ANALYSIS_LIMIT;

	/** Busy timeout for SQLite. */
	@Value("${sqlite.timeout:PT1S}")
	private Duration busyTimeout = Duration.ofSeconds(1);

	@Value("classpath:/spalloc.sql")
	private Resource sqlDDLFile;

	@Value("classpath:/spalloc-static-data.sql")
	private Resource sqlInitDataFile;

	@Autowired
	private TerminationNotifyingThreadFactory threadFactory;

	@Autowired(required = false)
	private Map<String, Function> functions = new HashMap<>();

	private static Set<String> columnNames(ResultSetMetaData md)
			throws SQLException {
		Set<String> names = new LinkedHashSet<>();
		for (int i = 1; i <= md.getColumnCount(); i++) {
			names.add(md.getColumnName(i));
		}
		return names;
	}

	/**
	 * A restricted form of result set. Note that this object <em>must not</em>
	 * be saved outside the context of iteration over its' query's results.
	 *
	 * @author Donal Fellows
	 */
	public static final class Row {
		private final ResultSet rs;

		private Row(ResultSet rs) {
			this.rs = rs;
		}

		/**
		 * Get the column names from this row.
		 *
		 * @return The set of column names; all lookup of columns is by name, so
		 *         the order is unimportant. (The set returned will iterate over
		 *         the names in the order they are in the underlying result set,
		 *         but this is considered "unimportant".)
		 * @throws SQLException
		 */
		public Set<String> getColumnNames() throws SQLException {
			return columnNames(rs.getMetaData());
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return A string, or {@code null} on {@code NULL}.
		 * @throws SQLException
		 *             If a problem occurs
		 */
		public String getString(String columnLabel) throws SQLException {
			return rs.getString(columnLabel);
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return A boolean, or {@code false} on {@code NULL}.
		 * @throws SQLException
		 *             If a problem occurs
		 */
		public boolean getBoolean(String columnLabel) throws SQLException {
			return rs.getBoolean(columnLabel);
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return An integer, or {@code 0} on {@code NULL}.
		 * @throws SQLException
		 *             If a problem occurs
		 */
		public int getInt(String columnLabel) throws SQLException {
			return rs.getInt(columnLabel);
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return A byte array, or {@code null} on {@code NULL}.
		 * @throws SQLException
		 *             If a problem occurs
		 */
		public byte[] getBytes(String columnLabel) throws SQLException {
			return rs.getBytes(columnLabel);
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return An instant, or {@code null} on {@code NULL}.
		 * @throws SQLException
		 *             If a problem occurs
		 */
		public Instant getInstant(String columnLabel) throws SQLException {
			long moment = rs.getLong(columnLabel);
			if (rs.wasNull()) {
				return null;
			}
			return Instant.ofEpochSecond(moment);
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return A duration, or {@code null} on {@code NULL}.
		 * @throws SQLException
		 *             If a problem occurs
		 */
		public Duration getDuration(String columnLabel) throws SQLException {
			long span = rs.getLong(columnLabel);
			if (rs.wasNull()) {
				return null;
			}
			return Duration.ofSeconds(span);
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return An automatically-decoded object, or {@code false} on
		 *         {@code NULL}. (Only returns basic types.)
		 * @throws SQLException
		 *             If a problem occurs
		 */
		public Object getObject(String columnLabel) throws SQLException {
			return rs.getObject(columnLabel);
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param <T>
		 *            The enumeration type.
		 * @param columnLabel
		 *            The name of the column.
		 * @param type
		 *            The enumeration type class.
		 * @return An enum value, or {@code null} on {@code NULL}.
		 * @throws SQLException
		 *             If a problem occurs
		 */
		public <T extends Enum<T>> T getEnum(String columnLabel, Class<T> type)
				throws SQLException {
			int value = rs.getInt(columnLabel);
			if (rs.wasNull()) {
				return null;
			}
			return type.getEnumConstants()[value];
		}
	}

	@PostConstruct
	private void ensureDBsetup() throws SQLException {
		threadFactory.setTerminationCallback(this::optimiseDB);
		setupConfig();
		try (Connection conn = getConnection();
				Query countMovements = query(conn, COUNT_MOVEMENTS)) {
			Row row = countMovements.call1().get();
			if (row.getInt("c") != EXPECTED_NUM_MOVEMENTS) {
				log.warn("database {} seems incomplete", dbConnectionUrl);
			} else {
				log.debug("database {} ready", dbConnectionUrl);
			}
		}
	}

	private void setupConfig() {
		config.enforceForeignKeys(true);
		config.setSynchronous(NORMAL);
		config.setBusyTimeout((int) busyTimeout.toMillis());
		config.setTransactionMode(IMMEDIATE);
		config.setDateClass("INTEGER");
	}

	/**
	 * Create an engine interface for a particular database. This constructor
	 * assumes that it is being called by Spring.
	 *
	 * @param dbFile
	 *            The file containing the database.
	 */
	@Autowired
	public DatabaseEngine(
			@Value("${databasePath:spalloc.sqlite3}") File dbFile) {
		dbPath = requireNonNull(dbFile, "a database file must be given")
				.getAbsoluteFile().toPath();
		dbConnectionUrl = "jdbc:sqlite:" + dbPath;
		log.info("will manage database at {}", dbPath);
	}

	/**
	 * Create an engine interface for an in-memory database. This is intended
	 * mainly for testing purposes. Note that various coupled automatic services
	 * are disabled, in particular connections are not closed automatically.
	 *
	 * @param prototype
	 *            Used to initialise fields normally set by injection. Must not
	 *            be {@code null}.
	 */
	public DatabaseEngine(DatabaseEngine prototype) {
		dbPath = null;
		dbConnectionUrl = "jdbc:sqlite::memory:";
		log.info("will manage pure in-memory database");
		busyTimeout = prototype.busyTimeout;
		sqlDDLFile = prototype.sqlDDLFile;
		sqlInitDataFile = prototype.sqlInitDataFile;
		setupConfig();
	}

	/**
	 * How to initialise a connection opened on a database that didn't
	 * previously exist.
	 *
	 * @param conn
	 *            The connection to the database.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	private void initDBConn(SQLiteConnection conn) throws SQLException {
		log.info("initalising DB ({}) schema from {}", conn.libversion(),
				sqlDDLFile);
		exec(conn, sqlDDLFile);
		for (String s : functions.keySet()) {
			Function.create(conn, s, functions.get(s));
		}
		transaction(conn, () -> {
			log.info("initalising DB static data from {}", sqlInitDataFile);
			exec(conn, sqlInitDataFile);
		});
	}

	@Override
	SQLiteConnection openDatabaseConnection() throws SQLException {
		log.debug("opening database connection {}", dbConnectionUrl);
		return (SQLiteConnection) config.createConnection(dbConnectionUrl);
	}

	/**
	 * Used to ensure that only one database connection is actually writing
	 * serialisation stuff at a time. We can wait for each other.
	 * <p>
	 * This is necessary because there is otherwise extremely high contention on
	 * the database during application shutdown.
	 */
	private Lock optimiseSerialisationLock = new ReentrantLock();

	/**
	 * Optimises the database. Called when the database connection is about to
	 * be closed due to the thread (in the thread pool) that owns it going away.
	 * <em>Called from the thread that owns the database connection in
	 * question.</em>
	 */
	private void optimiseDB() {
		optimiseSerialisationLock.lock();
		try {
			long start = System.currentTimeMillis();
			// NB: Not a standard query! Safe, because we know we have an int
			try (Connection conn = getConnection()) {
				conn.unwrap(SQLiteConnection.class).setBusyTimeout(0);
				exec(conn, String.format(OPTIMIZE_DB, analysisLimit));
			} catch (SQLiteException e) {
				/*
				 * If we're busy, just don't bother; it's optional to optimise
				 * the DB at this point. If we miss one, eventually another
				 * thread will pick it up.
				 */
				if (e.getResultCode() != SQLITE_BUSY) {
					throw e;
				}
			}
			long end = System.currentTimeMillis();
			log.debug("optimised the database in {}ms", end - start);
		} catch (SQLException e) {
			log.warn("failed to optimise DB pre-close", e);
		} finally {
			optimiseSerialisationLock.unlock();
		}
	}

	/**
	 * Get a connection. This connection is thread-bound; it <em>must not</em>
	 * be passed to other threads. They should get their own connections
	 * instead.
	 * <p>
	 * Note that if an in-memory database is used (see the
	 * {@linkplain #DatabaseEngine(DatabaseEngine) alternate constructor}, that
	 * DB can <em>only</em> be accessed from the connection returned from this
	 * method; the next call to this method (whether from the current thread or
	 * another one) will get an independent database.
	 *
	 * @return A configured initialised connection to the database.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public Connection getConnection() throws SQLException {
		try {
			if (dbPath == null) {
				// In-memory DB (dbPath null) always must be initialised
				SQLiteConnection conn = openDatabaseConnection();
				initDBConn(conn);
				return conn;
			}
			synchronized (this) {
				boolean doInit = !initialised || !exists(dbPath);
				SQLiteConnection conn = getCachedDatabaseConnection();
				if (doInit) {
					initDBConn(conn);
					initialised = true;
				}
				return uncloseableThreadBound(conn);
			}
		} catch (SQLiteException e) {
			throw rewriteException(e);
		}
	}

	/**
	 * Get the location of the database.
	 *
	 * @return The path to the database.
	 */
	public Path getDatabasePath() {
		return dbPath;
	}

	/**
	 * Set the parameters for a prepared statement.
	 *
	 * @param s
	 *            The statement to set the parameters for.
	 * @param arguments
	 *            The values to set the parameters to.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public static void setParams(PreparedStatement s, Object... arguments)
			throws SQLException {
		int idx = 0;
		int paramCount = s.getParameterMetaData().getParameterCount();
		if (paramCount == 0 && arguments.length > 0) {
			throw new SQLException("prepared statement takes no arguments");
		} else if (paramCount != arguments.length) {
			throw new SQLException("prepared statement takes " + paramCount
					+ " arguments, not " + arguments.length);
		}
		s.clearParameters();
		for (Object arg : arguments) {
			if (arg instanceof Instant) {
				arg = ((Instant) arg).getEpochSecond();
			} else if (arg instanceof Duration) {
				arg = ((Duration) arg).getSeconds();
			} else if (arg instanceof Enum) {
				arg = ((Enum<?>) arg).ordinal();
			}
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
		try {
			setParams(s, arguments);
			return s.executeUpdate();
		} catch (SQLiteException e) {
			throw rewriteException(e);
		}
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
		try {
			setParams(s, arguments);
			return s.executeQuery();
		} catch (SQLiteException e) {
			throw rewriteException(e);
		}
	}

	/**
	 * Get the stack frame description of the caller of the of the transaction.
	 *
	 * @return The (believed) caller of the transaction. {@code null} if this
	 *         can't be determined.
	 */
	private static StackTraceElement getCaller() {
		try {
			boolean found = false;
			for (StackTraceElement frame : currentThread().getStackTrace()) {
				String name = frame.getClassName();
				if (name.startsWith("java.") || name.startsWith("javax.")
						// MAGIC!
						|| name.startsWith("sun.")) {
					continue;
				}
				boolean found1 = name.equals(DatabaseEngine.class.getName())
						// Special case
						&& !frame.getMethodName().contains("initDBConn");
				found |= found1;
				if (found && !found1) {
					return frame;
				}
			}
		} catch (SecurityException ignored) {
			// Security manager says no? OK, we can cope.
		}
		return null;
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
		try {
			if (!conn.getAutoCommit()) {
				// Already in a transaction; just run the operation
				operation.act();
				return;
			}
			if (log.isDebugEnabled()) {
				log.debug("start transaction: {}", getCaller());
			}
			conn.setAutoCommit(false);
		} catch (SQLiteException e) {
			throw rewriteException(e);
		}
		boolean done = false;
		try {
			operation.act();
			try {
				conn.commit();
			} catch (SQLiteException e) {
				throw rewriteException(e);
			}
			done = true;
		} catch (SQLException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
		} finally {
			try {
				if (!done) {
					conn.rollback();
				}
				conn.setAutoCommit(true);
			} catch (SQLiteException e) {
				throw rewriteException(e);
			}
			log.debug("finish transaction");
		}
	}

	/**
	 * A nestable transaction runner. If the {@code operation} completes
	 * normally (and this isn't a nested use), the transaction commits. If an
	 * exception is thrown, the transaction is rolled back.
	 *
	 * @param <T>
	 *            The type of the result of {@code operation}
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
		try {
			if (!conn.getAutoCommit()) {
				// Already in a transaction; just run the operation
				return operation.act();
			}
			if (log.isDebugEnabled()) {
				log.debug("start transaction:\n{}", getCaller());
			}
			conn.setAutoCommit(false);
		} catch (SQLiteException e) {
			throw rewriteException(e);
		}
		boolean done = false;
		try {
			T result = operation.act();
			try {
				conn.commit();
			} catch (SQLiteException e) {
				throw rewriteException(e);
			}
			done = true;
			return result;
		} catch (SQLException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
		} finally {
			try {
				if (!done) {
					conn.rollback();
				}
				conn.setAutoCommit(true);
			} catch (SQLiteException e) {
				throw rewriteException(e);
			}
			log.debug("finish transaction");
		}
	}

	/**
	 * A connection manager and transaction runner. If the {@code operation}
	 * completes normally (and this isn't a nested use), the transaction
	 * commits. If an exception is thrown, the transaction is rolled back. The
	 * connection is closed up in any case.
	 *
	 * @param operation
	 *            The operation to run
	 * @throws SQLException
	 *             If something goes wrong with database access.
	 * @throws RuntimeException
	 *             If something other than database access goes wrong with the
	 *             contained code.
	 */
	public void executeVoid(Connected operation) throws SQLException {
		try (Connection conn = getConnection()) {
			transaction(conn, () -> operation.act(conn));
		}
	}

	/**
	 * A connection manager and transaction runner. If the {@code operation}
	 * completes normally (and this isn't a nested use), the transaction
	 * commits. If an exception is thrown, the transaction is rolled back. The
	 * connection is closed up in any case.
	 *
	 * @param <T>
	 *            The type of the result of {@code operation}
	 * @param operation
	 *            The operation to run
	 * @return the value returned by {@code operation}
	 * @throws SQLException
	 *             If something goes wrong with database access.
	 * @throws RuntimeException
	 *             If something other than database access goes wrong with the
	 *             contained code.
	 */
	public <T> T execute(ConnectedWithResult<T> operation)
			throws SQLException {
		try (Connection conn = getConnection()) {
			return transaction(conn, () -> operation.act(conn));
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
	 * Some code that may be run within a transaction and which will be given a
	 * new connection for the duration.
	 */
	@FunctionalInterface
	public interface Connected {
		/**
		 * The operation to run.
		 *
		 * @param connection
		 *            The newly-created connection. Do not save beyond the scope
		 *            of this action.
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		void act(Connection connection) throws SQLException;
	}

	/**
	 * Some code that may be run within a transaction that returns a result and
	 * which will be given a new connection for the duration.
	 *
	 * @param <T>
	 *            The type of the result of the code.
	 */
	@FunctionalInterface
	public interface ConnectedWithResult<T> {
		/**
		 * The operation to run.
		 *
		 * @param connection
		 *            The newly-created connection. Do not save beyond the scope
		 *            of this action.
		 * @return The result of the operation.
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		T act(Connection connection) throws SQLException;
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
			log.debug("{} is {}", resource,
					resource.getFile().getAbsoluteFile());
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
	 *            The SQL to run. Probably DDL. This may contain multiple
	 *            statements.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public static void exec(Connection conn, String sql) throws SQLException {
		try (Statement s = conn.createStatement()) {
			// MUST be executeUpdate() to run multiple statements at once!
			s.executeUpdate(sql);
		} catch (SQLiteException e) {
			throw rewriteException(e);
		}
	}

	/**
	 * Run some SQL where the result is of no interest.
	 *
	 * @param conn
	 *            The connection.
	 * @param sqlResource
	 *            Reference to the SQL to run. Probably DDL. This may contain
	 *            multiple statements.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public static void exec(Connection conn, Resource sqlResource)
			throws SQLException {
		exec(conn, readSQL(sqlResource));
	}

	/**
	 * Run some SQL where the result is of no interest.
	 *
	 * @param sql
	 *            The SQL to run. Probably DDL. This may contain multiple
	 *            statements.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public void exec(String sql) throws SQLException {
		try (Connection conn = getConnection()) {
			exec(conn, sql);
		}
	}

	/**
	 * Run some SQL where the result is of no interest.
	 *
	 * @param sqlResource
	 *            Reference to the SQL to run. Probably DDL. This may contain
	 *            multiple statements.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	public void exec(Resource sqlResource) throws SQLException {
		try (Connection conn = getConnection()) {
			exec(conn, sqlResource);
		}
	}

	private static SQLiteException rewriteException(SQLiteException e) {
		// Sometimes, exception messages obscure rather than illuminate
		String msg = e.getMessage();
		if (msg.contains("SQL error or missing database (")) {
			msg = msg.replaceFirst("SQL error or missing database \\((.*)\\)",
					"$1");
			SQLiteException e2 = new SQLiteException(msg, e.getResultCode());
			e2.setStackTrace(e.getStackTrace());
			return e2;
		}
		return e;
	}

	/**
	 * Common shared code between {@link Query} and {@link Update}.
	 *
	 * @author Donal Fellows
	 */
	public abstract static class StatementWrapper implements AutoCloseable {
		/** The statement being managed. */
		final PreparedStatement s;

		/** The result set from the statement that we will manage. */
		ResultSet rs;

		StatementWrapper(Connection conn, String sql) throws SQLException {
			try {
				s = conn.prepareStatement(sql);
			} catch (SQLiteException e) {
				throw rewriteException(e);
			}
			rs = null;
		}

		final void closeResults() {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException ignored) {
				}
				rs = null;
			}
		}

		/**
		 * Get the number of arguments expected when calling this statement.
		 *
		 * @return The number of arguments. Types are arbitrary (because SQLite)
		 * @throws SQLException
		 *             If anything goes wrong
		 */
		public final int getNumArguments() throws SQLException {
			try {
				return s.getParameterMetaData().getParameterCount();
			} catch (SQLiteException e) {
				throw rewriteException(e);
			}
		}

		/**
		 * Get the set of names of columns produced when calling this statement.
		 *
		 * @return A set of names. The order is the order in the SQL producing
		 *         the result set, but this should normally be insignificant.
		 * @throws SQLException
		 *             If anything goes wrong
		 */
		public final Set<String> getRowColumnNames() throws SQLException {
			try {
				return columnNames(s.getMetaData());
			} catch (SQLiteException e) {
				throw rewriteException(e);
			}
		}

		@Override
		public final void close() throws SQLException {
			try {
				closeResults();
				s.close();
			} catch (SQLiteException e) {
				throw rewriteException(e);
			}
		}
	}

	/**
	 * Wrapping a prepared query to be more suitable for Java 8 onwards.
	 *
	 * @author Donal Fellows
	 */
	public static final class Query extends StatementWrapper {
		private Query(Connection conn, String sql) throws SQLException {
			super(conn, sql);
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
		public Iterable<Row> call(Object... arguments) throws SQLException {
			try {
				setParams(s, arguments);
				closeResults();
				rs = s.executeQuery();
			} catch (SQLiteException e) {
				throw rewriteException(e);
			}
			Row row = new Row(rs);
			return () -> new Iterator<Row>() {
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
				public Row next() {
					if (finished) {
						return null;
					}
					return row;
				}
			};
		}

		/**
		 * Run the query on the given arguments. The query must be one that only
		 * produces a single row result.
		 *
		 * @param arguments
		 *            Positional argument to the query
		 * @return The single row with the results, or empty if there is
		 *         no such row.
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		public Optional<Row> call1(Object... arguments) throws SQLException {
			try {
				setParams(s, arguments);
				closeResults();
				rs = s.executeQuery();
				if (rs.next()) {
					return Optional.of(new Row(rs));
				} else {
					return Optional.empty();
				}
			} catch (SQLiteException e) {
				throw rewriteException(e);
			}
		}
	}

	// @formatter:off
	/**
	 * Create a new query. Usage pattern:
	 * <pre>
	 * try (Query q = query(conn, SQL_SELECT)) {
	 *     for (Row row : u.call(argument1, argument2)) {
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
	 *     for (Row row : u.call(argument1, argument2)) {
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
	public static final class Update extends StatementWrapper {
		private Update(Connection conn, String sql) throws SQLException {
			super(conn, sql);
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
			try {
				setParams(s, arguments);
				closeResults();
				return s.executeUpdate();
			} catch (SQLiteException e) {
				throw rewriteException(e);
			}
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
		public Iterable<Integer> keys(Object... arguments) throws SQLException {
			/*
			 * In theory, the statement should have been prepared with the
			 * GET_GENERATED_KEYS flag set. In practice, the SQLite driver
			 * ignores that flag.
			 */
			try {
				setParams(s, arguments);
				closeResults();
				s.executeUpdate();
				rs = s.getGeneratedKeys();
			} catch (SQLiteException e) {
				throw rewriteException(e);
			}
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

	/**
	 * Handler for {@link SQLException}.
	 *
	 * @author Donal Fellows
	 */
	@Component
	@Provider
	public static class SQLExceptionMapper
			implements ExceptionMapper<SQLException> {
		@Value("${sqlite.throwExceptionDetail:true}") // TODO turn off in prod
		private boolean throwDetail;

		@Override
		public Response toResponse(SQLException exception) {
			log.warn("uncaught SQL exception", exception);
			if (throwDetail) {
				return status(INTERNAL_SERVER_ERROR)
						.entity("failed: " + exception.getMessage()).build();
			}
			return status(INTERNAL_SERVER_ERROR).entity("failed").build();
		}
	}
}
