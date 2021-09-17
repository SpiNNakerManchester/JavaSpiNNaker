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
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.sqlite.Function.FLAG_DETERMINISTIC;
import static org.sqlite.SQLiteConfig.SynchronousMode.NORMAL;
import static org.sqlite.SQLiteConfig.TransactionMode.IMMEDIATE;
import static org.sqlite.SQLiteErrorCode.SQLITE_BUSY;
import static uk.ac.manchester.spinnaker.alloc.UncheckedConnection.mapException;
import static uk.ac.manchester.spinnaker.storage.threading.OneThread.uncloseableThreadBound;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.jdbc.datasource.init.UncategorizedScriptException;
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
 *
 * @author Donal Fellows
 */
@Component
public final class DatabaseEngine extends DatabaseCache<SQLiteConnection> {
	private static final Logger log = getLogger(DatabaseEngine.class);

	/**
	 * The name of the mounted database. Always {@code main} by SQLite
	 * convention.
	 */
	private static final String MAIN_DB_NAME = "main";

	@ResultColumn("c")
	@SingleRowResult
	private static final String COUNT_MOVEMENTS =
			"SELECT count(*) AS c FROM movement_directions";

	private static final Map<Resource, String> QUERY_CACHE = new HashMap<>();

	// From https://sqlite.org/lang_analyze.html
	// These are special operations
	private static final String OPTIMIZE_DB =
			"PRAGMA analysis_limit=%d; PRAGMA optimize;";

	/**
	 * Used to validate the database contents. Number of items in
	 * {@code movement_directions} table.
	 */
	private static final int EXPECTED_NUM_MOVEMENTS = 18;

	private final Path dbPath;

	private String tombstoneFile;

	private final String dbConnectionUrl;

	private final SQLiteConfig config = new SQLiteConfig();

	private boolean initialised;

	/**
	 * Control the amount of resources used for auto-optimisation.
	 *
	 * @see <a href="https://sqlite.org/lang_analyze.html">SQLite docs</a>
	 */
	private int analysisLimit;

	/** Busy timeout for SQLite. */
	private Duration busyTimeout = Duration.ofSeconds(1);

	@Value("classpath:/spalloc.sql")
	private Resource sqlDDLFile;

	@Value("classpath:/spalloc-tombstone.sql")
	private Resource tombstoneDDLFile;

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
		 */
		public Set<String> getColumnNames() {
			try {
				return columnNames(rs.getMetaData());
			} catch (SQLException e) {
				throw mapException(e, null);
			}
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return A string, or {@code null} on {@code NULL}.
		 */
		public String getString(String columnLabel) {
			try {
				return rs.getString(columnLabel);
			} catch (SQLException e) {
				throw mapException(e, null);
			}
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return A boolean, or {@code false} on {@code NULL}.
		 */
		public boolean getBoolean(String columnLabel) {
			try {
				return rs.getBoolean(columnLabel);
			} catch (SQLException e) {
				throw mapException(e, null);
			}
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return An integer, or {@code 0} on {@code NULL}.
		 */
		public int getInt(String columnLabel) {
			try {
				return rs.getInt(columnLabel);
			} catch (SQLException e) {
				throw mapException(e, null);
			}
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return An integer or {@code null}.
		 */
		public Integer getInteger(String columnLabel) {
			try {
				return (Integer) rs.getObject(columnLabel);
			} catch (SQLException e) {
				throw mapException(e, null);
			}
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return A byte array, or {@code null} on {@code NULL}.
		 */
		public byte[] getBytes(String columnLabel) {
			try {
				return rs.getBytes(columnLabel);
			} catch (SQLException e) {
				throw mapException(e, null);
			}
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return An instant, or {@code null} on {@code NULL}.
		 */
		public Instant getInstant(String columnLabel) {
			try {
				long moment = rs.getLong(columnLabel);
				if (rs.wasNull()) {
					return null;
				}
				return Instant.ofEpochSecond(moment);
			} catch (SQLException e) {
				throw mapException(e, null);
			}
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return A duration, or {@code null} on {@code NULL}.
		 */
		public Duration getDuration(String columnLabel) {
			try {
				long span = rs.getLong(columnLabel);
				if (rs.wasNull()) {
					return null;
				}
				return Duration.ofSeconds(span);
			} catch (SQLException e) {
				throw mapException(e, null);
			}
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return An automatically-decoded object, or {@code null} on
		 *         {@code NULL}. (Only returns basic types.)
		 */
		public Object getObject(String columnLabel) {
			try {
				return rs.getObject(columnLabel);
			} catch (SQLException e) {
				throw mapException(e, null);
			}
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
		 */
		public <T extends Enum<T>> T getEnum(String columnLabel,
				Class<T> type) {
			try {
				int value = rs.getInt(columnLabel);
				if (rs.wasNull()) {
					return null;
				}
				return type.getEnumConstants()[value];
			} catch (SQLException e) {
				throw mapException(e, null);
			}
		}

		/**
		 * Get the contents of the named column.
		 *
		 * @param columnLabel
		 *            The name of the column.
		 * @return A long value, or {@code null} on {@code NULL}.
		 */
		public Long getLong(String columnLabel) {
			try {
				Number value = (Number) rs.getObject(columnLabel);
				if (rs.wasNull()) {
					return null;
				}
				return value.longValue();
			} catch (SQLException e) {
				throw mapException(e, null);
			}
		}
	}

	@PostConstruct
	private void ensureDBsetup() {
		threadFactory.setTerminationCallback(this::optimiseDB);
		setupConfig();
		// Check that the connection is correct
		try (Connection conn = getConnection();
				Query countMovements = query(conn, COUNT_MOVEMENTS)) {
			int numMovements = countMovements.call1().get().getInt("c");
			if (numMovements != EXPECTED_NUM_MOVEMENTS) {
				log.warn("database {} seems incomplete ({} != {})",
						dbConnectionUrl, numMovements, EXPECTED_NUM_MOVEMENTS);
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
	 * @param properties
	 *            The application configuration.
	 */
	@Autowired
	public DatabaseEngine(SpallocProperties properties) {
		dbPath = requireNonNull(properties.getDatabasePath(),
				"a database file must be given").getAbsoluteFile().toPath();
		tombstoneFile = requireNonNull(properties.getHistoricalData().getPath(),
				"an historical database file must be given").getAbsolutePath();
		dbConnectionUrl = "jdbc:sqlite:" + dbPath;
		analysisLimit = properties.getSqlite().getAnalysisLimit();
		busyTimeout = properties.getSqlite().getTimeout();
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
	private DatabaseEngine(DatabaseEngine prototype) {
		dbPath = null;
		tombstoneFile = ":memory:";
		dbConnectionUrl = "jdbc:sqlite::memory:";
		log.info("will manage pure in-memory database");
		busyTimeout = prototype.busyTimeout;
		analysisLimit = prototype.analysisLimit;
		sqlDDLFile = prototype.sqlDDLFile;
		tombstoneDDLFile = prototype.tombstoneDDLFile;
		sqlInitDataFile = prototype.sqlInitDataFile;
		functions = prototype.functions;
		setupConfig();
	}

	/**
	 * Create an engine interface for an in-memory database. This is intended
	 * mainly for testing purposes. Note that various coupled automatic services
	 * are disabled, in particular connections are not closed automatically.
	 *
	 * @return The in-memory database interface.
	 */
	public DatabaseEngine getInMemoryDB() {
		return new DatabaseEngine(this);
	}

	/**
	 * If placed on a bean of type {@link Function}, specifies how many
	 * arguments that function takes.
	 *
	 * @author Donal Fellows
	 */
	@Documented
	@Retention(RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ArgumentCount {
		/**
		 * The number of arguments taken by the function. If not specified, any
		 * number are taken.
		 *
		 * @return The allowed number of arguments.
		 */
		int value() default -1;
	}

	/**
	 * If placed on a bean of type {@link Function}, specifies that the function
	 * will always give the same answer with the same inputs.
	 *
	 * @author Donal Fellows
	 */
	@Documented
	@Retention(RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Deterministic {
	}

	/**
	 * If placed on a bean of type {@link Function}, specifies that the function
	 * may be used in schema structures.
	 *
	 * @see DatabaseEngine#SQLITE_DIRECTONLY SQLITE_DIRECTONLY
	 * @see DatabaseEngine#SQLITE_INNOCUOUS SQLITE_INNOCUOUS
	 * @author Donal Fellows
	 * @deprecated Consult the SQLite documentation on innocuous functions very
	 *             carefully before enabling this. Only disable the deprecation
	 *             warning if you're sure the security concerns it talks about
	 *             have been accounted for.
	 */
	@Deprecated
	@Documented
	@Retention(RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Innocuous {
	}

	/**
	 * The prefix of bean names that may be removed to generate the SQLite
	 * function name.
	 */
	private static final String FUN_NAME_PREFIX = "function.";

	/**
	 * Flag direct from SQLite.
	 * <p>
	 * <blockquote>The {@code SQLITE_DIRECTONLY} flag means that the function
	 * may only be invoked from top-level SQL, and cannot be used in
	 * <em>VIEW</em>s or <em>TRIGGER</em>s nor in schema structures such as
	 * <em>CHECK</em> constraints, <em>DEFAULT</em> clauses, expression indexes,
	 * partial indexes, or generated columns. The {@code SQLITE_DIRECTONLY}
	 * flags is a security feature which is recommended for all
	 * application-defined SQL functions, and especially for functions that have
	 * side-effects or that could potentially leak sensitive
	 * information.</blockquote>
	 * <p>
	 * Note that the password-related functions we install are definitely
	 * examples of functions that are only usable directly.
	 *
	 * @see <a href=
	 *      "https://www.sqlite.org/c3ref/c_deterministic.html">SQLite</a>
	 */
	public static final int SQLITE_DIRECTONLY = 0x000080000;

	/**
	 * Flag direct from SQLite.
	 * <p>
	 * <blockquote>The {@code SQLITE_INNOCUOUS} flag means that the function is
	 * unlikely to cause problems even if misused. An innocuous function should
	 * have no side effects and should not depend on any values other than its
	 * input parameters. The {@code abs()} function is an example of an
	 * innocuous function. The {@code load_extension()} SQL function is not
	 * innocuous because of its side effects.
	 * <p>
	 * {@code SQLITE_INNOCUOUS} is similar to {@code SQLITE_DETERMINISTIC}, but
	 * is not exactly the same. The {@code random()} function is an example of a
	 * function that is innocuous but not deterministic.
	 * <p>
	 * Some heightened security settings ({@code SQLITE_DBCONFIG_TRUSTED_SCHEMA}
	 * and {@code PRAGMA trusted_schema=OFF}) disable the use of SQL functions
	 * inside views and triggers and in schema structures such as <em>CHECK</em>
	 * constraints, <em>DEFAULT</em> clauses, expression indexes, partial
	 * indexes, and generated columns unless the function is tagged with
	 * {@code SQLITE_INNOCUOUS}. Most built-in functions are innocuous.
	 * Developers are advised to avoid using the {@code SQLITE_INNOCUOUS} flag
	 * for application-defined functions unless the function has been carefully
	 * audited and found to be free of potentially security-adverse side-effects
	 * and information-leaks. </blockquote>
	 * <p>
	 * Note that this engine marks non-innocuous functions as
	 * {@link #SQLITE_DIRECTONLY}; this is slightly over-eager, but likely
	 * correct.
	 *
	 * @see <a href=
	 *      "https://www.sqlite.org/c3ref/c_deterministic.html">SQLite</a>
	 */
	public static final int SQLITE_INNOCUOUS = 0x000200000;

	/**
	 * Install a function into SQLite.
	 *
	 * @param conn
	 *            The database connection to install the function in.
	 * @param name
	 *            The name of the function. Usually the bean name. Note that the
	 *            prefix "{@code function.}" will be removed.
	 * @param func
	 *            The implementation of the function.
	 * @throws SQLException
	 *             If installation fails. Caller handles mapping.
	 */
	private static void installFunction(SQLiteConnection conn, String name,
			Function func) throws SQLException {
		if (requireNonNull(name, "function name must not be null")
				.startsWith(FUN_NAME_PREFIX)) {
			name = name.substring(FUN_NAME_PREFIX.length());
		}
		if (name.isEmpty()) {
			throw new UncategorizedScriptException("crazy function name");
		}

		int nArgs = -1;
		ArgumentCount c = findAnnotation(func.getClass(), ArgumentCount.class);
		if (nonNull(c)) {
			nArgs = c.value();
		}

		int flags;
		if (nonNull(findAnnotation(func.getClass(), Innocuous.class))) {
			flags = SQLITE_INNOCUOUS;
		} else {
			flags = SQLITE_DIRECTONLY;
		}
		if (nonNull(findAnnotation(func.getClass(), Deterministic.class))) {
			flags |= FLAG_DETERMINISTIC;
		}

		// Call into the driver to actually bind the function
		Function.create(conn, name, func, nArgs, flags);

		if (nArgs >= 0) {
			log.debug("installed function {} ({} argument(s)) in connection {}",
					name, nArgs, conn);
		} else {
			log.debug("installed function {} in connection {}", name, conn);
		}
	}

	/**
	 * How to initialise a connection opened on a database that didn't
	 * previously exist.
	 *
	 * @param conn
	 *            The connection to the database.
	 */
	private void initDBConn(SQLiteConnection conn) {
		try {
			log.info("initalising DB ({}) schema from {}", conn.libversion(),
					sqlDDLFile);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
		// Note that we don't close the wrapper
		Connection wrapper = new Connection(conn);
		exec(wrapper, sqlDDLFile);
		log.info("attaching historical job DB ({}) schema from {}",
				tombstoneFile, tombstoneDDLFile);
		update(wrapper, "ATTACH DATABASE ? AS tombstone").call(tombstoneFile);
		exec(wrapper, tombstoneDDLFile);
		transaction(wrapper, () -> {
			log.info("initalising DB static data from {}", sqlInitDataFile);
			exec(wrapper, sqlInitDataFile);
		});
	}

	@Override
	SQLiteConnection openDatabaseConnection() {
		log.debug("opening database connection {}", dbConnectionUrl);
		try {
			SQLiteConnection conn =
					(SQLiteConnection) config.createConnection(dbConnectionUrl);
			// Has to be done for every connection; can't be cached
			for (String s : functions.keySet()) {
				installFunction(conn, s, functions.get(s));
			}
			return conn;
		} catch (SQLException e) {
			throw mapException(e, null);
		}
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
			} catch (DataAccessException e) {
				/*
				 * If we're busy, just don't bother; it's optional to optimise
				 * the DB at this point. If we miss one, eventually another
				 * thread will pick it up.
				 */
				if (!isBusy(e)) {
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
	 * Connections made by the database engine bean. Its methods do not throw
	 * checked exceptions. The connection is thread-bound, and will be cleaned
	 * up correctly when the thread exits (ideal for thread pools).
	 */
	public static final class Connection extends UncheckedConnection {
		private Connection(java.sql.Connection c) {
			super(c);
		}
	}

	/**
	 * Get a connection. This connection is thread-bound and pooled; it <em>must
	 * not</em> be passed to other threads. They should get their own
	 * connections instead.
	 * <p>
	 * Note that if an in-memory database is used (see
	 * {@link #getInMemoryDB()}), that DB can <em>only</em> be accessed from the
	 * connection returned from this method; the next call to this method
	 * (whether from the current thread or another one) will get an independent
	 * database. Such in-memory databases are not subject to thread-bound
	 * cleanup actions; they're simply deleted from memory when no longer used
	 * (but the connection should be {@code close()}d after use for efficiency
	 * nonetheless).
	 *
	 * @return A configured initialised connection to the database.
	 */
	public Connection getConnection() {
		if (isNull(dbPath)) {
			// In-memory DB (dbPath null) always must be initialised
			SQLiteConnection conn = openDatabaseConnection();
			initDBConn(conn);
			return new Connection(conn);
		}
		synchronized (this) {
			boolean doInit = !initialised || !exists(dbPath);
			SQLiteConnection conn = getCachedDatabaseConnection();
			if (doInit) {
				initDBConn(conn);
				initialised = true;
			}
			return new Connection(uncloseableThreadBound(conn));
		}
	}

	/**
	 * Creates a backup of the database. <em>This operation should only be
	 * called by administrators.</em>
	 *
	 * @param backupFilename
	 *            The backup file to create.
	 */
	public void createBackup(File backupFilename) {
		try (SQLiteConnection conn = getCachedDatabaseConnection()) {
			conn.getDatabase().backup(MAIN_DB_NAME,
					backupFilename.getAbsolutePath(),
					(remaining, pageCount) -> log.info(
							"BACKUP TO {} (remaining:{}, page count:{})",
							backupFilename, remaining, pageCount));
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	/**
	 * Restores the database from backup. <em>This operation should only be
	 * called by administrators.</em>
	 *
	 * @param backupFilename
	 *            The backup file to restore from.
	 * @throws PermissionDeniedDataAccessException
	 *             If the backup cannot be read.
	 */
	public void restoreFromBackup(File backupFilename) {
		if (!backupFilename.isFile() || !backupFilename.canRead()) {
			throw new PermissionDeniedDataAccessException(
					"backup file \"" + backupFilename
							+ "\" doesn't exist or isn't a readable file",
					new FileNotFoundException(backupFilename.toString()));
		}
		try (SQLiteConnection conn = getCachedDatabaseConnection()) {
			conn.getDatabase().restore(MAIN_DB_NAME,
					backupFilename.getAbsolutePath(),
					(remaining, pageCount) -> log.info(
							"RESTORE FROM {} (remaining:{}, page count:{})",
							backupFilename, remaining, pageCount));
		} catch (SQLException e) {
			throw mapException(e, null);
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
	 */
	public static void setParams(PreparedStatement s, Object... arguments) {
		try {
			int paramCount = s.getParameterMetaData().getParameterCount();
			if (paramCount == 0 && arguments.length > 0) {
				throw new InvalidDataAccessResourceUsageException(
						"prepared statement takes no arguments");
			} else if (paramCount != arguments.length) {
				throw new InvalidDataAccessResourceUsageException(
						"prepared statement takes " + paramCount
								+ " arguments, not " + arguments.length);
			}
			s.clearParameters();

			int idx = 0;
			for (Object arg : arguments) {
				// The classes we augment the DB driver with
				if (arg instanceof Instant) {
					arg = ((Instant) arg).getEpochSecond();
				} else if (arg instanceof Duration) {
					arg = ((Duration) arg).getSeconds();
				} else if (arg instanceof Enum) {
					arg = ((Enum<?>) arg).ordinal();
				}
				s.setObject(++idx, arg);
			}
		} catch (SQLException e) {
			throw mapException(e, s.toString());
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
	 */
	public static int runUpdate(PreparedStatement s, Object... arguments) {
		try {
			setParams(s, arguments);
			return s.executeUpdate();
		} catch (SQLException e) {
			throw mapException(e, s.toString());
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
	 */
	public static ResultSet runQuery(PreparedStatement s, Object... arguments) {
		try {
			setParams(s, arguments);
			return s.executeQuery();
		} catch (SQLException e) {
			throw mapException(e, s.toString());
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
	 * @throws DataAccessException
	 *             If something goes wrong with the database access.
	 * @throws RuntimeException
	 *             If something unexpected goes wrong with the contained code.
	 */
	public static void transaction(Connection conn, Transacted operation) {
		if (!conn.getAutoCommit()) {
			// Already in a transaction; just run the operation
			operation.act();
			return;
		}
		if (log.isDebugEnabled()) {
			log.debug("start transaction: {}", getCaller());
		}
		conn.setAutoCommit(false);
		boolean done = false;
		try {
			operation.act();
			conn.commit();
			done = true;
			return;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
		} finally {
			if (!done) {
				conn.rollback();
			}
			conn.setAutoCommit(true);
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
	 * @throws DataAccessException
	 *             If something goes wrong with the database access.
	 * @throws RuntimeException
	 *             If something unexpected goes wrong with the contained code.
	 */
	public static <T> T transaction(Connection conn,
			TransactedWithResult<T> operation) {
		if (!conn.getAutoCommit()) {
			// Already in a transaction; just run the operation
			return operation.act();
		}
		if (log.isDebugEnabled()) {
			log.debug("start transaction:\n{}", getCaller());
		}
		conn.setAutoCommit(false);
		boolean done = false;
		try {
			T result = operation.act();
			conn.commit();
			done = true;
			return result;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
		} finally {
			if (!done) {
				conn.rollback();
			}
			conn.setAutoCommit(true);
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
	 * @throws RuntimeException
	 *             If something goes wrong with the database access or the
	 *             contained code.
	 */
	public void executeVoid(Connected operation) {
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
	 * @throws RuntimeException
	 *             If something other than database access goes wrong with the
	 *             contained code.
	 */
	public <T> T execute(ConnectedWithResult<T> operation) {
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
		 */
		void act();
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
		 */
		T act();
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
		 */
		void act(Connection connection);
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
		 */
		T act(Connection connection);
	}

	/**
	 * Simple reader that loads a complex SQL query from a file.
	 *
	 * @param resource
	 *            The resource to load from
	 * @return The content of the resource
	 * @throws UncategorizedScriptException
	 *             If the resource can't be loaded.
	 */
	public static String readSQL(Resource resource) {
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
			throw new UncategorizedScriptException(
					"could not load SQL file from " + resource, e);
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
	 */
	public static void exec(Connection conn, String sql) {
		try (Statement s = conn.createStatement()) {
			// MUST be executeUpdate() to run multiple statements at once!
			s.executeUpdate(sql);
		} catch (SQLException e) {
			throw mapException(e, sql);
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
	 */
	public static void exec(Connection conn, Resource sqlResource) {
		exec(conn, readSQL(sqlResource));
	}

	/**
	 * Run some SQL where the result is of no interest.
	 *
	 * @param sql
	 *            The SQL to run. Probably DDL. This may contain multiple
	 *            statements.
	 */
	public void exec(String sql) {
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
	 */
	public void exec(Resource sqlResource) {
		try (Connection conn = getConnection()) {
			exec(conn, sqlResource);
		}
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

		StatementWrapper(Connection conn, String sql) {
			s = conn.prepareStatement(sql);
			rs = null;
		}

		final void closeResults() {
			if (nonNull(rs)) {
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
		 */
		public final int getNumArguments() {
			try {
				return s.getParameterMetaData().getParameterCount();
			} catch (SQLException e) {
				throw mapException(e, s.toString());
			}
		}

		/**
		 * Get the set of names of columns produced when calling this statement.
		 *
		 * @return A set of names. The order is the order in the SQL producing
		 *         the result set, but this should normally be insignificant.
		 */
		public final Set<String> getRowColumnNames() {
			try {
				return columnNames(s.getMetaData());
			} catch (SQLException e) {
				throw mapException(e, s.toString());
			}
		}

		@Override
		public final void close() {
			try {
				closeResults();
				s.close();
			} catch (SQLException e) {
				throw mapException(e, s.toString());
			}
		}

		private static final int TRIM_LENGTH = 80;

		@Override
		public String toString() {
			// Exclude comments and compress whitespace
			String sql = s.toString().replaceAll("--[^\n]*\n", " ")
					.replaceAll("\\s+", " ").trim();
			// Trim long queries to no more than TRIM_LENGTH...
			String sql2 =
					sql.replaceAll("^(.{0," + TRIM_LENGTH + "})\\b.*$", "$1");
			if (sql2 != sql) {
				// and add an ellipsis if we do the trimming
				sql = sql2 + "...";
			}
			return getClass().getSimpleName() + " : " + sql;
		}
	}

	/**
	 * Extends iterable with the ability to be mapped to different values.
	 *
	 * @param <T> The type of elements returned by the iterator
	 * @author Donal Fellows
	 */
	public interface MappableIterable<T> extends Iterable<T> {
		default <TT> MappableIterable<TT>
				map(java.util.function.Function<T, TT> mapper) {
			MappableIterable<T> src = this;
			return new MappableIterable<TT>() {
				@Override
				public Iterator<TT> iterator() {
					Iterator<T> srcit = src.iterator();
					return new Iterator<TT>() {
						@Override
						public boolean hasNext() {
							return srcit.hasNext();
						}

						@Override
						public TT next() {
							return mapper.apply(srcit.next());
						}
					};
				}
			};
		}
	}

	/**
	 * Wrapping a prepared query to be more suitable for Java 8 onwards.
	 *
	 * @author Donal Fellows
	 */
	public static final class Query extends StatementWrapper {
		private Query(Connection conn, String sql) {
			super(conn, sql);
		}

		/**
		 * Run the query on the given arguments.
		 *
		 * @param arguments
		 *            Positional argument to the query
		 * @return The results, wrapped as a one-shot iterable. The
		 *         {@linkplain Row rows} in the iterable <em>must not</em> be
		 *         retained by callers; they may share state and might not
		 *         outlive the iteration.
		 */
		public MappableIterable<Row> call(Object... arguments) {
			closeResults();
			rs = runQuery(s, arguments);
			return () -> new Iterator<Row>() {
				private boolean finished = false;
				private boolean consumed = true;
				// Share this row wrapper; underlying row changes
				private final Row row = new Row(rs);

				@Override
				public boolean hasNext() {
					if (finished) {
						return false;
					}
					if (!consumed) {
						return true;
					}
					boolean result = false;
					try {
						result = rs.next();
					} catch (SQLException e) {
					} finally {
						if (result) {
							consumed = false;
						} else {
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
					consumed = true;
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
		 * @return The single row with the results, or empty if there is no such
		 *         row.
		 */
		public Optional<Row> call1(Object... arguments) {
			try {
				setParams(s, arguments);
				closeResults();
				rs = s.executeQuery();
				if (rs.next()) {
					return Optional.of(new Row(rs));
				} else {
					return Optional.empty();
				}
			} catch (SQLException e) {
				throw mapException(e, s.toString());
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
	 */
	// @formatter:on
	public static Query query(Connection conn, String sql) {
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
	 */
	// @formatter:on
	public static Query query(Connection conn, Resource sqlResource) {
		return new Query(conn, readSQL(sqlResource));
	}

	/**
	 * Wrapping a prepared update to be more suitable for Java 8 onwards.
	 *
	 * @author Donal Fellows
	 */
	public static final class Update extends StatementWrapper {
		private Update(Connection conn, String sql) {
			super(conn, sql);
		}

		/**
		 * Run the update on the given arguments.
		 *
		 * @param arguments
		 *            Positional argument to the query
		 * @return The number of rows updated
		 */
		public int call(Object... arguments) {
			closeResults();
			return runUpdate(s, arguments);
		}

		/**
		 * Run the update on the given arguments.
		 *
		 * @param arguments
		 *            Positional arguments to the query
		 * @return The integer primary keys generated by the update.
		 */
		public MappableIterable<Integer> keys(Object... arguments) {
			/*
			 * In theory, the statement should have been prepared with the
			 * GET_GENERATED_KEYS flag set. In practice, the SQLite driver
			 * ignores that flag.
			 */
			closeResults();
			int numRows = runUpdate(s, arguments);
			try {
				rs = s.getGeneratedKeys();
			} catch (SQLException e) {
				throw mapException(e, s.toString());
			}
			return () -> new Iterator<Integer>() {
				private boolean finished = false;
				private int rowCount = 0;
				private boolean consumed = true;

				@Override
				public boolean hasNext() {
					if (finished || rowCount + 1 > numRows) {
						return false;
					}
					if (!consumed) {
						return true;
					}
					boolean result = false;
					try {
						result = rs.next();
						rowCount++;
					} catch (SQLException e) {
					} finally {
						if (!result) {
							finished = true;
							closeResults();
						}
					}
					if (result) {
						consumed = false;
					}
					return result;
				}

				@Override
				public Integer next() {
					if (finished) {
						return null;
					}
					try {
						// Assume that keys fit in 31 bits
						return (Integer) rs.getObject(1);
					} catch (SQLException e) {
						closeResults();
						finished = true;
						return null;
					} finally {
						consumed = true;
					}
				}
			};
		}

		/**
		 * Run the update on the given arguments.
		 *
		 * @param arguments
		 *            Positional arguments to the query
		 * @return The integer primary key generated by the update.
		 */
		public Optional<Integer> key(Object... arguments) {
			closeResults();
			runUpdate(s, arguments);
			try {
				rs = s.getGeneratedKeys();
				if (rs.next()) {
					return Optional.ofNullable((Integer) rs.getObject(1));
				} else {
					return Optional.empty();
				}
			} catch (SQLException e) {
				throw mapException(e, s.toString());
			} finally {
				closeResults();
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
	 */
	// @formatter:on
	public static Update update(Connection conn, String sql) {
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
	 */
	// @formatter:on
	public static Update update(Connection conn, Resource sqlResource) {
		return new Update(conn, readSQL(sqlResource));
	}

	/**
	 * Utility for testing whether an exception was thrown because the database
	 * was busy.
	 *
	 * @param e
	 *            The outer wrapping exception.
	 * @return Whether it was caused by the database being busy.
	 */
	public static boolean isBusy(DataAccessException e) {
		return e.getMostSpecificCause() instanceof SQLiteException
				&& ((SQLiteException) e.getMostSpecificCause()).getResultCode()
						.equals(SQLITE_BUSY);
	}

	/**
	 * Convert an iterable of rows (see {@link DatabaseEngine.Query Query}) into
	 * a list of objects, one per row.
	 *
	 * @param <T>
	 *            The type of elements in the list
	 * @param rows
	 *            The rows to convert.
	 * @param mapper
	 *            The conversion function.
	 * @return List of objects mapped from rows. (This list happens to be
	 *         modifiable, but with no effect on the database.)
	 */
	public static <T> List<T> rowsAsList(Iterable<Row> rows,
			java.util.function.Function<Row, T> mapper) {
		List<T> result = new ArrayList<>();
		for (Row row : rows) {
			result.add(mapper.apply(row));
		}
		return result;
	}

	/**
	 * Convert an iterable of rows (see {@link DatabaseEngine.Query Query}) into
	 * a set of objects, one per row.
	 *
	 * @param <T>
	 *            The type of elements in the set
	 * @param rows
	 *            The rows to convert.
	 * @param mapper
	 *            The conversion function.
	 * @return Set of objects mapped from rows. (This set happens to be
	 *         modifiable, but with no effect on the database. The set's natural
	 *         ordering is the order of the elements in the database.)
	 */
	public static <T> Set<T> rowsAsSet(Iterable<Row> rows,
			java.util.function.Function<Row, T> mapper) {
		Set<T> result = new LinkedHashSet<>();
		for (Row row : rows) {
			result.add(mapper.apply(row));
		}
		return result;
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
		@Autowired
		private SpallocProperties properties;

		@Override
		public Response toResponse(SQLException exception) {
			log.warn("uncaught SQL exception", exception);
			if (properties.getSqlite().isDebugFailures()) {
				return status(INTERNAL_SERVER_ERROR)
						.entity("failed: " + exception.getMessage()).build();
			}
			return status(INTERNAL_SERVER_ERROR).entity("failed").build();
		}
	}

	/**
	 * Handler for {@link DataAccessException}.
	 *
	 * @author Donal Fellows
	 */
	@Component
	@Provider
	public static class DataAccessExceptionMapper
			implements ExceptionMapper<DataAccessException> {
		@Autowired
		private SpallocProperties properties;

		@Override
		public Response toResponse(DataAccessException exception) {
			log.warn("uncaught SQL exception", exception);
			if (properties.getSqlite().isDebugFailures()) {
				return status(INTERNAL_SERVER_ERROR)
						.entity("failed: " + exception.getMessage()).build();
			}
			return status(INTERNAL_SERVER_ERROR).entity("failed").build();
		}
	}
}
