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
package uk.ac.manchester.spinnaker.alloc.db;

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.nanoTime;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.concurrent.ConcurrentUtils.constantFuture;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.sqlite.Function.FLAG_DETERMINISTIC;
import static org.sqlite.SQLiteConfig.JournalMode.WAL;
import static org.sqlite.SQLiteConfig.SynchronousMode.NORMAL;
import static org.sqlite.SQLiteConfig.TransactionMode.DEFERRED;
import static org.sqlite.SQLiteConfig.TransactionMode.IMMEDIATE;
import static uk.ac.manchester.spinnaker.alloc.IOUtils.serialize;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.db.SQLiteFlags.SQLITE_DIRECTONLY;
import static uk.ac.manchester.spinnaker.alloc.db.SQLiteFlags.SQLITE_INNOCUOUS;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.isBusy;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.mapException;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.trimSQL;
import static uk.ac.manchester.spinnaker.storage.threading.OneThread.threadBound;
import static uk.ac.manchester.spinnaker.storage.threading.OneThread.uncloseableThreadBound;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.collectToArray;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.NSEC_PER_MSEC;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.NSEC_PER_USEC;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.datasource.init.UncategorizedScriptException;
import org.springframework.stereotype.Service;
import org.sqlite.Function;
import org.sqlite.SQLiteCommitListener;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteException;

import com.google.errorprone.annotations.CompileTimeConstant;
import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.DBProperties;
import uk.ac.manchester.spinnaker.storage.ResultColumn;
import uk.ac.manchester.spinnaker.storage.SingleRowResult;
import uk.ac.manchester.spinnaker.utils.DefaultMap;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * The database engine interface. Based on SQLite. Manages a pool of database
 * connections, each of which is bound to a specific thread. The connections are
 * closed when the thread in question terminates. <em>Assumes</em> that you are
 * using a thread pool.
 *
 * @author Donal Fellows
 */
@Service
public final class DatabaseEngine extends DatabaseCache<SQLiteConnection>
		implements DatabaseAPI {
	private static final Logger log = getLogger(DatabaseEngine.class);

	/**
	 * The name of the mounted database. Always {@code main} by SQLite
	 * convention.
	 */
	private static final String MAIN_DB_NAME = "main";

	/**
	 * The prefix of bean names that may be removed to generate the SQLite
	 * function name.
	 */
	private static final String FUN_NAME_PREFIX = "function.";

	@ResultColumn("c")
	@SingleRowResult
	private static final String COUNT_MOVEMENTS =
			"SELECT count(*) AS c FROM movement_directions";

	@GuardedBy("itself")
	private final Map<Resource, String> queryCache = new HashMap<>();

	/**
	 * Used to validate the database contents. Number of items in
	 * {@code movement_directions} table.
	 */
	private static final int EXPECTED_NUM_MOVEMENTS = 18;

	private static final int TRIM_PERF_LOG_LENGTH = 120;

	private final Path dbPath;

	private String tombstoneFile;

	private final String dbConnectionUrl;

	private final SQLiteConfig config = new SQLiteConfig();

	private volatile boolean initialised;

	private volatile boolean initMsg;

	private DBProperties props;

	@Value("classpath:/spalloc.sql")
	private Resource sqlDDLFile;

	/** The list of files containing schema updates. */
	@Value("classpath*:spalloc-schema-update-*.sql")
	private Resource[] schemaUpdates;

	@Value("classpath:/spalloc-tombstone.sql")
	private Resource tombstoneDDLFile;

	@Value("classpath:/spalloc-static-data.sql")
	private Resource sqlInitDataFile;

	@Autowired
	private TerminationNotifyingThreadFactory threadFactory;

	@Autowired(required = false)
	private Map<String, Function> functions = new HashMap<>();

	@Lazy
	@Autowired
	private ScheduledExecutorService executor;

	/**
	 * Whether to schedule a warning for long transactions. Normally want this
	 * when this bean is in service, but not when starting up or shutting down.
	 */
	private boolean warnOnLongTransactions;

	// If you add more autowired stuff here, make sure in-memory DBs get a copy!

	/**
	 * Mapping from SQL string to summary statistics about the execution times
	 * for that statement. The statistics are collected in microseconds.
	 */
	@GuardedBy("itself")
	private final Map<String, SummaryStatistics> statementLengths =
			new DefaultMap<>(SummaryStatistics::new);

	@GuardedBy("itself")
	private final Set<Thread> transactionHolders = new HashSet<>();

	/**
	 * Stores our per-connection state. This is kept in a weak hash map so that
	 * it lives as long as the actual connections do (as determined by the
	 * {@link DatabaseCache}), but no longer. Connection state has to be managed
	 * carefully because there are quite a few ephemeral wrappers involved as
	 * well.
	 */
	private final Map<SQLiteConnection, ConnectionState> stateMap =
			new WeakHashMap<>();

	/**
	 * Lock protecting whole-database level operations, especially startup and
	 * shutdown.
	 * <p>
	 * Normal transactions hold this as a shared read lock (even if they are
	 * transactions that write to the DB). Whole DB actions (initialise
	 * connection, backup, restore, optimise and shutdown connection) hold it as
	 * an exclusive write lock. Think of it as a lock protecting the database
	 * metadata/schema, even though that's strictly inaccurate.
	 */
	private final ReadWriteLock wholeDBLock = new ReentrantReadWriteLock();

	private final ThreadLocal<Boolean> alreadyLockedByMe =
			ThreadLocal.withInitial(() -> false);

	private void withDBWriteLock(Runnable action) {
		if (alreadyLockedByMe.get()) {
			log.info("double write lock by {}", Thread.currentThread(),
					new StackTraceCaptureException());
			action.run();
			return;
		}
		var lock = wholeDBLock.writeLock();
		alreadyLockedByMe.set(true);
		lock.lock();
		try {
			action.run();
		} finally {
			lock.unlock();
			alreadyLockedByMe.set(false);
		}
	}

	private <T> T withDBReadLock(Supplier<T> action) {
		if (alreadyLockedByMe.get()) {
			if (log.isDebugEnabled()) {
				log.debug("read inside write lock by {}",
						Thread.currentThread(),
						new StackTraceCaptureException());
			}
			return action.get();
		}
		var lock = wholeDBLock.readLock();
		lock.lock();
		try {
			return action.get();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Schedule a task (expected to be
	 * {@link DatabaseEngine.Connection.Locker#warnLock()}) to be run in the
	 * future, provided we are not initializing.
	 *
	 * @param task
	 *            The task to schedule.
	 * @param delay
	 *            How far in the future this is to happen.
	 * @return The cancellable future. The result value of the future is
	 *         unimportant. Never {@code null}.
	 */
	private Future<?> schedule(Runnable task, Duration delay) {
		try {
			if (warnOnLongTransactions) {
				return executor.schedule(task, delay.toNanos(), NANOSECONDS);
			}
		} catch (RejectedExecutionException ignored) {
			// Can't do anything about this, and it isn't too important.
		}
		return constantFuture(task);
	}

	/**
	 * Records the execution time of a statement, at least to first result set.
	 *
	 * @param s
	 *            The statement in question.
	 * @param pre
	 *            Nano-timestamp before.
	 * @param post
	 *            Nano-timestamp after.
	 */
	private void statementLength(Statement s, long pre, long post) {
		if (props.isPerformanceLog()) {
			long delta = post - pre;
			/*
			 * Hack to remove parameters added by JDBC4PreparedStatement. We
			 * don't want to log each statement preparation separately, but
			 * rather to aggregate across all calls.
			 */
			var sql = s.toString().replaceFirst(" \n parameters=.*", "");
			synchronized (statementLengths) {
				var stats = statementLengths.get(sql);
				stats.addValue(delta / NSEC_PER_USEC);
			}
		}
	}

	private static final Comparator<
			Map.Entry<?, SummaryStatistics>> STATS_COMP =
					Comparator.comparingDouble(e -> e.getValue().getMax());

	/**
	 * Writes the recorded statement execution times to the log if that is
	 * enabled.
	 */
	@PreDestroy
	private void logStatementExecutionTimes() {
		warnOnLongTransactions = false;
		if (props.isPerformanceLog() && log.isInfoEnabled()) {
			Map<String, SummaryStatistics> stats;
			synchronized (statementLengths) {
				stats = Map.copyOf(statementLengths);
			}
			// Anything new at this point we can ignore!
			stats.entrySet().stream()
					.filter(e -> e.getValue().getMax() >= props
							.getPerformanceThreshold())
					.sorted(STATS_COMP)
					.forEach(e -> logStatementPerformance(e.getKey(),
							e.getValue()));
		}
	}

	// NB: Doesn't use explainQueryPlan() for messy reasons
	private void logStatementPerformance(String sql, SummaryStatistics stats) {
		StringBuilder sb = new StringBuilder();
		if (props.isAutoExplain()) {
			/*
			 * This is the intent level of the children of a node in the query
			 * plan. The (non-existing) node zero has indent zero. Since we just
			 * use spaces for indentation, we don't need any sort of lookahead
			 * to compute the output.
			 */
			var levels = new HashMap<Integer, Integer>();
			try (var conn = getConnection();
					var s = ((ConnectionImpl) conn).createStatement();
					var r = s.executeQuery("EXPLAIN QUERY PLAN " + sql)) {
				while (r.next()) {
					int indent = levels.getOrDefault(r.getInt("parent"), 0);
					levels.put(r.getInt("id"), indent + 1);
					sb.append("\nEXPLAIN: ").append(" ".repeat(indent * 2))
							.append(r.getString("detail"));
				}
			} catch (SQLException | RuntimeException e) {
				log.warn("failed to dump statement explanation", e);
			}
		}
		log.info(
				"statement execution time {}us (max: {}us, SD: {}us) for: {}{}",
				stats.getMean(), stats.getMax(), stats.getStandardDeviation(),
				trimSQL(sql, TRIM_PERF_LOG_LENGTH), sb);
	}

	static Set<String> columnNames(ResultSetMetaData md) throws SQLException {
		var names = new LinkedHashSet<String>();
		for (int i = 1; i <= md.getColumnCount(); i++) {
			names.add(md.getColumnName(i));
		}
		return names;
	}

	@PostConstruct
	private void ensureDBsetup() {
		threadFactory.setTerminationCallback(this::optimiseDB);
		setupConfig();
		// Check that the connection is correct
		try (var conn = getConnection();
				var countMovements = conn.query(COUNT_MOVEMENTS)) {
			int numMovements = conn.transaction(false,
					() -> countMovements.call1().map(integer("c"))).orElse(-1);
			if (numMovements != EXPECTED_NUM_MOVEMENTS) {
				log.warn("database {} seems incomplete ({} != {})",
						dbConnectionUrl, numMovements, EXPECTED_NUM_MOVEMENTS);
			} else {
				log.debug("database {} ready", dbConnectionUrl);
			}
		} finally {
			warnOnLongTransactions = true;
		}
	}

	private void setupConfig() {
		config.enforceForeignKeys(true);
		config.setSynchronous(NORMAL);
		config.setBusyTimeout((int) props.getTimeout().toMillis());
		config.setDateClass("INTEGER");
		config.setJournalMode(WAL);
	}

	/**
	 * Create an engine interface for a particular database. This constructor
	 * assumes that it is being called by Spring.
	 *
	 * @param properties
	 *            The application configuration.
	 * @throws IllegalStateException
	 *             If the database and the tombstone database are the same file.
	 *             This is thrown hard here because otherwise you get the
	 *             <em>weirdest</em> and most misleading error out of SQLite.
	 *             The system won't work, so might as well make it very clear
	 *             immediately.
	 */
	@Autowired
	public DatabaseEngine(SpallocProperties properties) {
		dbPath = requireNonNull(properties.getDatabasePath(),
				"a database file must be given").getAbsoluteFile().toPath();
		tombstoneFile = requireNonNull(properties.getHistoricalData().getPath(),
				"an historical database file must be given").getAbsolutePath();
		if (dbPath.equals(Paths.get(tombstoneFile))) {
			throw new IllegalStateException(
					"tombstone DB is same as main DB (" + dbPath + ")");
		}
		dbConnectionUrl = "jdbc:sqlite:" + dbPath;
		props = properties.getSqlite();
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
		props = prototype.props;
		sqlDDLFile = prototype.sqlDDLFile;
		schemaUpdates = prototype.schemaUpdates;
		tombstoneDDLFile = prototype.tombstoneDDLFile;
		sqlInitDataFile = prototype.sqlInitDataFile;
		functions = prototype.functions;
		executor = prototype.executor;
		setupConfig();
		warnOnLongTransactions = true;
	}

	@Override
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
	@Target(TYPE)
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
	@Target(TYPE)
	public @interface Deterministic {
	}

	/**
	 * If placed on a bean of type {@link Function}, specifies that the function
	 * may be used in schema structures. The {@link DatabaseEngine} assumes that
	 * all functions that are not innocuous must be direct-only.
	 *
	 * @see SQLiteFlags#SQLITE_DIRECTONLY SQLITE_DIRECTONLY
	 * @see SQLiteFlags#SQLITE_INNOCUOUS SQLITE_INNOCUOUS
	 * @author Donal Fellows
	 * @deprecated Consult the SQLite documentation on innocuous functions very
	 *             carefully before enabling this. Only disable the deprecation
	 *             warning if you're sure the security concerns it talks about
	 *             have been accounted for.
	 */
	@Deprecated
	@Documented
	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface Innocuous {
	}

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
		if (name.isBlank()) {
			throw new UncategorizedScriptException("crazy function name");
		}

		int nArgs = -1;
		var c = findAnnotation(func.getClass(), ArgumentCount.class);
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
		var doInitMsg = !initMsg;
		if (doInitMsg) {
			initMsg = true;
		}
		try {
			if (doInitMsg) {
				log.info("initalising main DB ({}) schema from {}",
						conn.libversion(), sqlDDLFile);
			}
		} catch (SQLException e) {
			throw mapException(e, null);
		}
		// Note that we don't close the wrapper; this is deliberate!
		var wrapper = new ConnectionImpl(conn);
		wrapper.transaction(true, () -> {
			wrapper.exec(sqlDDLFile);
			if (wrapper.isHistoricalDBAvailable()) {
				if (doInitMsg) {
					log.info("initalising historical DB from schema {}",
							tombstoneDDLFile);
				}
				wrapper.exec(tombstoneDDLFile);
			}
			if (doInitMsg) {
				log.info("initalising DB static data from {}", sqlInitDataFile);
			}
			wrapper.exec(sqlInitDataFile);
			execSchemaUpdates(wrapper, doInitMsg);
			// Don't verify in-memory data!
			if (dbPath != null) {
				if (doInitMsg) {
					log.info("verifying main DB integrity");
				}
				wrapper.exec("SELECT COUNT(*) FROM jobs");
				if (wrapper.isHistoricalDBAvailable()) {
					if (doInitMsg) {
						log.info("verifying historical DB integrity");
					}
					wrapper.exec("SELECT COUNT(*) FROM tombstone.jobs");
				}
			}
		});
	}

	/**
	 * Applies schema updates from discovered schema update files.
	 *
	 * @param wrapper
	 *            Where to apply the updates.
	 * @param doInitMsg
	 *            Whether to write info-level log messages. Usually only do that
	 *            for the first time a database is connected to.
	 * @see #schemaUpdates
	 */
	private void execSchemaUpdates(ConnectionImpl wrapper, boolean doInitMsg) {
		for (var r : schemaUpdates) {
			try {
				wrapper.exec(r);
				if (doInitMsg) {
					log.info("applied schema update from {}", r);
				}
			} catch (DataAccessException e) {
				if (!e.getMessage().contains("duplicate column name")) {
					log.warn("failed to apply schema update from {}", r, e);
					throw e;
				}
			}
		}
	}

	/**
	 * Perform some actions immediately to condition the per-connection state.
	 *
	 * @param conn
	 *            The newly created, unwrapped connection
	 * @throws SQLException
	 *             Raw SQL exceptions
	 */
	private void prepareConnectionForService(SQLiteConnection conn)
			throws SQLException {
		for (var func : functions.entrySet()) {
			log.debug("installing function from bean '{}'", func.getKey());
			installFunction(conn, func.getKey(), func.getValue());
		}
		try {
			log.debug("attaching historical job DB ({})", tombstoneFile);
			try (var s =
					conn.prepareStatement("ATTACH DATABASE ? AS tombstone")) {
				s.setString(1, tombstoneFile);
				s.execute();
			}
		} catch (SQLiteException e) {
			if (isBusy(e)) {
				log.warn(
						"failed to attach historical job DB ({}); "
								+ "most normal operations unaffected",
						tombstoneFile, e);
			} else {
				throw e;
			}
		}
		conn.setAutoCommit(false);
		// We don't want to have a transaction active by default!
		conn.getDatabase().exec("rollback;", false);
		if (log.isDebugEnabled()) {
			conn.addCommitListener(new SQLiteCommitListener() {
				@Override
				public void onCommit() {
					log.debug("database is committing transaction");
				}

				@Override
				public void onRollback() {
					log.debug("database is rolling back transaction");
				}
			});
		}
	}

	@Override
	SQLiteConnection openDatabaseConnection() {
		log.debug("opening database connection {}", dbConnectionUrl);
		try {
			var conn = (SQLiteConnection) config
					.createConnection(dbConnectionUrl);
			prepareConnectionForService(conn);
			return conn;
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	/**
	 * Optimises the database. Called when the database connection is about to
	 * be closed due to the thread (in the thread pool) that owns it going away.
	 * <em>Called from the thread that owns the database connection in
	 * question.</em>
	 *
	 * @see <a href="https://sqlite.org/lang_analyze.html">SQLite
	 *      Documentation</a>
	 */
	private void optimiseDB() {
		withDBWriteLock(() -> {
			try {
				long start = currentTimeMillis();
				/*
				 * NB: Not a standard query! Safe, because we know we have an
				 * int
				 */
				try (var conn = getConnection();
						var c = (ConnectionImpl) conn) {
					c.unwrap(SQLiteConnection.class).setBusyTimeout(0);
					c.transaction(true, () -> {
						c.exec(format(
								"PRAGMA analysis_limit=%d; PRAGMA optimize;",
								props.getAnalysisLimit()));
					});
				} catch (DataAccessException e) {
					/*
					 * If we're busy, just don't bother; it's optional to
					 * optimise the DB at this point. If we miss one, eventually
					 * another thread will pick it up.
					 */
					if (!isBusy(e)) {
						throw e;
					}
				}
				long end = currentTimeMillis();
				log.debug("optimised the database in {}ms", end - start);
			} catch (SQLException e) {
				log.warn("failed to optimise DB pre-close", e);
			}
		});
	}

	private Object currentTransactionHolders() {
		synchronized (transactionHolders) {
			return transactionHolders.stream().map(Thread::getName)
					.collect(toList());
		}
	}

	/**
	 * Obtain some context for log messages when tracking what's happening in a
	 * transaction. The only useful operation possible with this object is to
	 * print it to the log!
	 *
	 * @return A printable object for debugging purposes. Might be the current
	 *         thread or might be a useful stack frame.
	 */
	private Object getDebugContext() {
		if (props.isEnableExpensiveTransactionDebugging()) {
			return getCaller();
		} else {
			return currentThread();
		}
	}

	private static final class MishandledTransactionException
			extends Exception {
		private static final long serialVersionUID = 1L;

		private MishandledTransactionException() {
			super("mishandled transaction");
		}

		static MishandledTransactionException generateException() {
			try {
				throw new MishandledTransactionException();
			} catch (MishandledTransactionException e) {
				return e;
			}
		}
	}

	private static final class StackTraceCaptureException extends Exception {
		// Never actually serialised in a way we care about
		private static final long serialVersionUID = 1L;

		private StackTraceCaptureException() {
			super("captured stack trace");
			// Drop the various framework frames that we don't care about
			fillInStackTrace();
			setStackTrace(Arrays.stream(getStackTrace()).filter(
					e -> e.getClassName().startsWith("uk.ac.manchester"))
					.collect(collectToArray(StackTraceElement[]::new)));
		}
	}

	/**
	 * State associated with a connection. It is important that this object hold
	 * no other state than the connection and this state object, because the
	 * state object is scoped to the correct object <em>and not to an ephemeral
	 * wrapper!</em>
	 * <p>
	 * In particular, <em>this</em> class holds state for
	 * {@link ConnectionImpl}. {@link #stateMap} is responsible for issuing the
	 * correct one for a connection.
	 */
	private static final class ConnectionState {
		private boolean inTransaction;

		private boolean isLockedForWrites;

		private final boolean canTombstone;

		ConnectionState(SQLiteConnection c) {
			inTransaction = false;
			isLockedForWrites = false;
			boolean tombstoneAvailable = false;
			try (var s = c.prepareStatement("PRAGMA database_list");
					var r = s.executeQuery()) {
				while (r.next()) {
					if ("tombstone".equals(r.getString("name"))) {
						tombstoneAvailable = true;
						break;
					}
				}
			} catch (SQLException ex) {
				log.error("problem when checking attached DB list", ex);
			}
			canTombstone = tombstoneAvailable;
		}
	}

	/**
	 * Connections made by the database engine bean. Its methods do not throw
	 * checked exceptions.
	 * <p>
	 * The underlying connection is thread-bound, and will be cleaned up
	 * correctly when the thread exits (ideal for thread pools) but instances of
	 * this class are technically ephemeral. Any state that you wish to hold
	 * must be either the connection passed in or an instance of
	 * {@link ConnectionState} that is manufactured to be keyed by the correct
	 * object (via {@link #stateMap}). <em>Getting the state handling wrong will
	 * cause <strong>extremely</strong>-difficult-to-diagnose deadlocks!</em>
	 */
	final class ConnectionImpl extends UncheckedConnection
			implements Connection {
		/**
		 * State associated with the connection. It is important that this
		 * object hold no other state than the connection and this state object,
		 * because the state object is scoped to the correct object <em>and not
		 * to an ephemeral wrapper!</em>
		 */
		private final ConnectionState state;

		private ConnectionImpl(java.sql.Connection c) {
			super(c);
			try {
				// This is horrible! And very very necessary!
				this.state = stateMap.computeIfAbsent(
						c.unwrap(SQLiteConnection.class),
						ConnectionState::new);
			} catch (SQLException ex) {
				log.error("problem when unwrapping SQLiteConnection", ex);
				throw new RuntimeException("unexpected error", ex);
			}
		}

		@Override
		public boolean isHistoricalDBAvailable() {
			return state.canTombstone;
		}

		private final class Locker implements AutoCloseable {
			private final long lockTimestamp;

			private final Object lockingContext;

			private final Future<?> lockWarningTimeout;

			private final long noteThreshold;

			private final boolean exclusive;

			private boolean unnecessary = false;

			/**
			 * @param lockForWriting
			 *            Whether to lock for writing. Multiple read locks can
			 *            be held at once, but only one write lock. Locks
			 *            <em>cannot</em> be upgraded (because that causes
			 *            deadlocks).
			 */
			@MustBeClosed
			Locker(boolean lockForWriting) {
				noteThreshold = props.getLockNoteThreshold().toNanos();
				exclusive = lockForWriting;
				state.isLockedForWrites = lockForWriting;
				lockingContext = getDebugContext();
				lockWarningTimeout =
						schedule(this::warnLock, props.getLockWarnThreshold());
				lockTimestamp = nanoTime();
			}

			@Override
			public void close() {
				long unlockTimestamp = nanoTime();
				lockWarningTimeout.cancel(false);
				long dt = unlockTimestamp - lockTimestamp;
				if (dt > noteThreshold) {
					log.info("transaction lock was held for {}ms",
							dt / NSEC_PER_MSEC,
							new StackTraceCaptureException());
				}
			}

			/**
			 * Issue a warning that a database transaction lock has been held
			 * for a long time.
			 */
			private void warnLock() {
				long dt = nanoTime() - lockTimestamp;
				log.warn(
						"transaction lock being held excessively by "
								+ "{} (> {}ms); current transactions are {}",
						lockingContext, dt / NSEC_PER_MSEC,
						currentTransactionHolders());
			}

			/**
			 * Start the transaction. Note that this result is why we do not
			 * begin the transaction as part of the creation of this object.
			 *
			 * @return True, if we <em>could not start the transaction because
			 *         the database was busy.</em> False otherwise (on success).
			 * @throws DataAccessException
			 *             If something else goes wrong.
			 */
			boolean begin() {
				try {
					realBegin(exclusive ? IMMEDIATE : DEFERRED);
					return false;
				} catch (PessimisticLockingFailureException e) {
					if (isBusy(e)) {
						return true;
					}
					throw e;
				}
			}

			private void commit() {
				realCommit();
				unnecessary = true;
			}

			@MustBeClosed
			RollbackHandler rollbackHandler(Object context) {
				return new RollbackHandler(context);
			}

			/**
			 * Rolls back the transaction <em>unless</em> it has been told not
			 * to by setting {@code unnecessary} to {@code true}.
			 */
			private class RollbackHandler implements AutoCloseable {
				private final Object context;

				private final boolean it;

				private final Thread holder;

				@MustBeClosed
				RollbackHandler(Object context) {
					this.context = context;
					it = state.inTransaction;
					holder = currentThread();
					if (!it) {
						state.inTransaction = true;
						synchronized (transactionHolders) {
							transactionHolders.add(holder);
						}
					}
				}

				@Override
				public void close() {
					if (!it) {
						state.inTransaction = false;
						synchronized (transactionHolders) {
							transactionHolders.remove(holder);
						}
					}
					try {
						if (!unnecessary) {
							log.debug("commence rollback: {}", context);
							realRollback();
						}
					} catch (DataAccessException e) {
						cantWarning("rollback", e);
						throw e;
					}
				}
			}
		}

		void checkInTransaction(boolean expectedLockType) {
			if (!state.inTransaction) {
				log.warn("executing not inside transaction: {}",
						getDebugContext(),
						MishandledTransactionException.generateException());
			} else if (expectedLockType && !state.isLockedForWrites) {
				log.warn("performing write inside read transaction: {}",
						getDebugContext(),
						MishandledTransactionException.generateException());
			}
		}

		@Override
		public void transaction(boolean lockForWriting, Transacted operation) {
			// Use the other method, ignoring the result value
			transaction(lockForWriting, () -> {
				operation.act();
				return this;
			});
		}

		@Override
		public void transaction(Transacted operation) {
			transaction(true, operation);
		}

		@Override
		public <T> T transaction(TransactedWithResult<T> operation) {
			return transaction(true, operation);
		}

		@Override
		public <T> T transaction(boolean lockForWriting,
				TransactedWithResult<T> operation) {
			return withDBReadLock(() -> {
				var context = getDebugContext();
				if (state.inTransaction) {
					if (lockForWriting && !state.isLockedForWrites) {
						log.warn("attempt to upgrade lock: {}", context);
					}
					// Already in a transaction; just run the operation
					return operation.act();
				}
				int tries = 0;
				while (true) {
					tries++;
					if (log.isDebugEnabled()) {
						log.debug("start transaction: {}", context);
					}
					try (var locker = new Locker(lockForWriting)) {
						if (locker.begin()) {
							continue;
						}
						try (var rollback = locker.rollbackHandler(context)) {
							var result = operation.act();
							log.debug("commence commit: {}", context);
							locker.commit();
							return result;
						} catch (DataAccessException e) {
							if (tries < props.getLockTries() && isBusy(e)) {
								log.warn("retrying transaction due to lock "
										+ "failure: {}", context);
								log.info("current transaction holders are {}",
										currentTransactionHolders());
								sleepUntilTimeToRetry();
								continue;
							}
							cantWarning("commit", e);
							throw e;
						}
					}
				}
			});
		}

		private void sleepUntilTimeToRetry() {
			try {
				sleep(props.getLockFailedDelay().toMillis());
			} catch (InterruptedException e) {
				log.trace("interrupted while waiting until time to "
						+ "retry transaction", e);
			}
		}

		private void cantWarning(String op, DataAccessException e) {
			if (e.getMostSpecificCause() instanceof SQLiteException ex
					&& ex.getMessage().contains(
							"cannot " + op + " - no transaction is active")) {
				log.warn(
						"failed to {} transaction: "
								+ "current transaction holders are {}",
						op, currentTransactionHolders());
			}
		}

		@Override
		public Query query(@CompileTimeConstant String sql) {
			return new QueryImpl(this, false, sql);
		}

		@Override
		public Query query(@CompileTimeConstant String sql, boolean lockType) {
			return new QueryImpl(this, lockType, sql);
		}

		@Override
		public Query query(Resource sqlResource) {
			return new QueryImpl(this, false, readSQL(sqlResource));
		}

		@Override
		public Query query(Resource sqlResource, boolean lockType) {
			return new QueryImpl(this, lockType, readSQL(sqlResource));
		}

		@Override
		public Update update(@CompileTimeConstant String sql) {
			return new UpdateImpl(this, sql);
		}

		@Override
		public Update update(Resource sqlResource) {
			return new UpdateImpl(this, readSQL(sqlResource));
		}

		/**
		 * Run some SQL where the result is of no interest.
		 *
		 * @param sql
		 *            The SQL to run. Probably DDL. This <em>may</em> contain
		 *            multiple statements. This <em>may</em> be generated code,
		 *            but if so you <em>must</em> ensure that there are no
		 *            possible SQL injection errors.
		 * @see #query(String)
		 * @see #update(String)
		 * @deprecated Prefer query() or update() wherever possible, as they are
		 *             proofed against SQL injection.
		 */
		@Deprecated
		public void exec(String sql) {
			checkInTransaction(true);
			try (var s = createStatement()) {
				// MUST be executeUpdate() to run multiple statements at once!
				s.executeUpdate(sql);
				/*
				 * Note that we do NOT record the execution time here. This is
				 * expected to be used for non-critical-path statements only
				 * (setting up connections).
				 */
			} catch (SQLException e) {
				throw mapException(e, sql);
			}
		}

		/**
		 * Run some SQL where the result is of no interest.
		 *
		 * @param sqlResource
		 *            Reference to the SQL to run. Probably DDL. This
		 *            <em>may</em> contain multiple statements.
		 * @see #query(Resource)
		 * @see #update(Resource)
		 * @deprecated Prefer query() or update() wherever possible, as they are
		 *             proofed against SQL injection.
		 */
		@Deprecated
		public void exec(Resource sqlResource) {
			exec(readSQL(sqlResource));
		}
	}

	@Override
	@MustBeClosed
	public Connection getConnection() {
		if (isNull(dbPath)) {
			// In-memory DB (dbPath null) always must be initialised
			var conn = openDatabaseConnection();
			initDBConn(conn);
			return new ConnectionImpl(threadBound(conn));
		}
		if (!isLongTermThread()) {
			var conn = getCachedDatabaseConnection();
			maybeInit(conn);
			return new ConnectionImpl(uncloseableThreadBound(conn));
		} else {
			var conn = openDatabaseConnection();
			maybeInit(conn);
			return new ConnectionImpl(threadBound(conn));
		}
	}

	private void maybeInit(SQLiteConnection conn) {
		boolean doInit = !initialised || !exists(dbPath);
		if (doInit) {
			withDBWriteLock(() -> initDBConn(conn));
			initialised = true;
		}
	}

	@Override
	public void createBackup(File backupFilename) {
		withDBWriteLock(() -> {
			try (var conn = getCachedDatabaseConnection()) {
				conn.getDatabase().backup(MAIN_DB_NAME,
						backupFilename.getAbsolutePath(),
						(remaining, pageCount) -> log.info(
								"BACKUP TO {} (remaining:{}, page count:{})",
								backupFilename, remaining, pageCount));
			} catch (SQLException e) {
				throw mapException(e, null);
			}
		});
	}

	@Override
	public void restoreFromBackup(File backupFilename) {
		if (!backupFilename.isFile() || !backupFilename.canRead()) {
			throw new PermissionDeniedDataAccessException(
					"backup file \"" + backupFilename
							+ "\" doesn't exist or isn't a readable file",
					new FileNotFoundException(backupFilename.toString()));
		}
		withDBWriteLock(() -> {
			try (var conn = getCachedDatabaseConnection()) {
				conn.getDatabase().restore(MAIN_DB_NAME,
						backupFilename.getAbsolutePath(),
						(remaining, pageCount) -> log.info(
								"RESTORE FROM {} (remaining:{}, page count:{})",
								backupFilename, remaining, pageCount));
			} catch (SQLException e) {
				throw mapException(e, null);
			}
		});
	}

	/**
	 * Get the location of the database.
	 *
	 * @return The path to the database.
	 */
	public Path getDatabasePath() {
		return dbPath;
	}

	private static final Set<String> FILTERED_CLASSES = Set.of(
			Query.class.getName(), Update.class.getName(),
			Connection.class.getName(), DatabaseAwareBean.class.getName(),
			"uk.ac.manchester.spinnaker.alloc.bmp."
					+ "BMPController$AbstractSQL");

	private static final Set<String> FILTERED_PREFIXES = Set.of(//
			"java", "javax", "sun");

	/**
	 * Get the stack frame description of the caller of the of the transaction.
	 * <p>
	 * This is an expensive operation.
	 *
	 * @return The (believed) caller of the transaction. {@code null} if this
	 *         can't be determined.
	 */
	private static StackTraceElement getCaller() {
		boolean found = false;
		for (var frame : currentThread().getStackTrace()) {
			var name = frame.getClassName();
			var first = name.substring(0, max(0, name.indexOf('.')));
			if (FILTERED_PREFIXES.contains(first)) {
				continue;
			}
			boolean found1 = FILTERED_CLASSES.contains(name);
			found |= found1;
			if (found && !found1) {
				return frame;
			}
		}
		return null;
	}

	@Override
	public void executeVoid(boolean lockForWriting, Connected operation) {
		try (var conn = getConnection()) {
			conn.transaction(lockForWriting, () -> {
				operation.act(conn);
				return this;
			});
		}
	}

	@Override
	public <T> T execute(boolean lockForWriting,
			ConnectedWithResult<T> operation) {
		try (var conn = getConnection()) {
			return conn.transaction(lockForWriting, () -> operation.act(conn));
		}
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
	private String readSQL(Resource resource) {
		synchronized (queryCache) {
			if (queryCache.containsKey(requireNonNull(resource,
					"undefined resource; check your constructors!"))) {
				return queryCache.get(resource);
			}
		}
		try (var is = resource.getInputStream()) {
			var s = IOUtils.toString(is, UTF_8);
			synchronized (queryCache) {
				// Not really a problem if it is put in twice
				queryCache.put(resource, s);
			}
			return s;
		} catch (IOException e) {
			throw new UncategorizedScriptException(
					"could not load SQL file from " + resource, e);
		}
	}

	private abstract static sealed class StatementWrapper
			implements StatementCommon permits QueryImpl, UpdateImpl {
		/** The statement being managed. */
		final PreparedStatement s;

		/** The result set from the statement that we will manage. */
		ResultSet rs;

		/** The database connection. */
		final ConnectionImpl conn;

		/** The text of the query. */
		private final String sql;

		StatementWrapper(ConnectionImpl conn, String sql) {
			this.conn = conn;
			s = conn.prepareStatement(sql);
			rs = null;
			this.sql = sql;
		}

		final void closeResults() {
			if (nonNull(rs)) {
				try {
					log.debug("closing result set");
					rs.close();
				} catch (SQLException e) {
					log.trace("failure when closing result set", e);
				}
				rs = null;
			}
		}

		/**
		 * Set the parameters for the prepared statement.
		 *
		 * @param arguments
		 *            The values to set the parameters to.
		 * @throws SQLException
		 *             If there's a DB problem.
		 * @throws InvalidDataAccessResourceUsageException
		 *             If given a bad number of arguments.
		 */
		final void setParams(Object[] arguments) throws SQLException {
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
			for (var arg : arguments) {
				// The classes we augment the DB driver with
				if (arg instanceof Optional<?> opt) {
					// Unpack one layer of Optional only; absent = NULL
					arg = opt.orElse(null);
				}
				if (arg instanceof Instant inst) {
					arg = inst.getEpochSecond();
				} else if (arg instanceof Duration d) {
					arg = d.getSeconds();
				} else if (arg instanceof Enum<?> e) {
					arg = e.ordinal();
				} else if (arg != null && arg instanceof Serializable
						&& !(arg instanceof String || arg instanceof Number
								|| arg instanceof Boolean
								|| arg instanceof byte[])) {
					try {
						arg = serialize(arg);
					} catch (IOException e) {
						arg = null;
					}
				}
				s.setObject(++idx, arg);
			}
		}

		@Override
		public final int getNumArguments() {
			try {
				return s.getParameterMetaData().getParameterCount();
			} catch (SQLException e) {
				throw mapException(e, s.toString());
			}
		}

		@Override
		public final Set<String> getRowColumnNames() {
			try {
				return columnNames(s.getMetaData());
			} catch (SQLException e) {
				throw mapException(e, s.toString());
			}
		}

		@Override
		public final void close() {
			closeResults();
			try {
				s.close();
			} catch (SQLException e) {
				throw mapException(e, s.toString());
			}
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + " : " + trimSQL(s.toString());
		}

		// NB: logStatementPerformance() doesn't use this for messy reasons
		/**
		 * {@inheritDoc} This is a list of rows (as strings) that form a tree;
		 * the indentation is pre-computed.
		 */
		@Override
		public List<String> explainQueryPlan() {
			var result = new ArrayList<String>();
			var levels = new HashMap<Integer, Integer>();
			try (var s = conn.createStatement();
					var r = s.executeQuery("EXPLAIN QUERY PLAN " + sql)) {
				while (r.next()) {
					int indent = levels.getOrDefault(r.getInt("parent"), 0);
					levels.put(r.getInt("id"), indent + 1);
					result.add("  ".repeat(indent) + r.getString("detail"));
				}
			} catch (SQLException e) {
				throw mapException(e, s.toString());
			}
			return result;
		}
	}

	/**
	 * Wrapping a prepared query to be more suitable for Java 8 onwards.
	 *
	 * @author Donal Fellows
	 */
	private final class QueryImpl extends StatementWrapper implements Query {
		private QueryImpl(ConnectionImpl conn, boolean lockType, String sql) {
			super(conn, sql);
			this.lockType = lockType;
		}

		private final boolean lockType;

		@Override
		public MappableIterable<Row> call(Object... arguments) {
			conn.checkInTransaction(lockType);
			closeResults();
			try {
				log.debug("opening result set in {}", getDebugContext());
				setParams(arguments);
				long pre = nanoTime();
				rs = s.executeQuery();
				long post = nanoTime();
				statementLength(s, pre, post);
			} catch (SQLException e) {
				throw mapException(e, s.toString());
			}
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
					conn.checkInTransaction(lockType);
					boolean result = false;
					try {
						result = rs.next();
					} catch (SQLException e) {
						throw mapException(e, s.toString());
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
						throw new NoSuchElementException();
					}
					consumed = true;
					return row;
				}
			};
		}

		@Override
		public Optional<Row> call1(Object... arguments) {
			conn.checkInTransaction(lockType);
			try {
				log.debug("opening result set in {}", getDebugContext());
				closeResults();
				setParams(arguments);
				long pre = nanoTime();
				rs = s.executeQuery();
				long post = nanoTime();
				statementLength(s, pre, post);
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

	private final class UpdateImpl extends StatementWrapper implements Update {
		private UpdateImpl(ConnectionImpl conn, String sql) {
			super(conn, sql);
		}

		@Override
		public int call(Object... arguments) {
			conn.checkInTransaction(true);
			closeResults();
			try {
				setParams(arguments);
				long pre = nanoTime();
				int result = s.executeUpdate();
				long post = nanoTime();
				statementLength(s, pre, post);
				return result;
			} catch (SQLException e) {
				throw mapException(e, s.toString());
			}
		}

		@Override
		public MappableIterable<Integer> keys(Object... arguments) {
			conn.checkInTransaction(true);
			/*
			 * In theory, the statement should have been prepared with the
			 * GET_GENERATED_KEYS flag set. In practice, the SQLite driver
			 * ignores that flag.
			 */
			closeResults();
			int numRows;
			long pre, post;
			try {
				setParams(arguments);
				log.debug("opening result set in {}", getDebugContext());
				pre = nanoTime();
				numRows = s.executeUpdate();
				post = nanoTime();
				rs = s.getGeneratedKeys();
				statementLength(s, pre, post);
			} catch (SQLException e) {
				throw mapException(e, s.toString());
			}
			return () -> new Iterator<Integer>() {
				private boolean finished = false;
				private int rowCount = 0;
				private boolean consumed = true;
				private Integer key = null;

				@Override
				public boolean hasNext() {
					if (finished || rowCount + 1 > numRows) {
						return false;
					}
					if (!consumed) {
						return true;
					}
					conn.checkInTransaction(true);
					boolean result = false;
					try {
						result = rs.next();
						rowCount++;
						if (result) {
							key = (Integer) rs.getObject(1);
						}
					} catch (SQLException e) {
						result = false;
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
						throw new NoSuchElementException();
					}
					consumed = true;
					return key;
				}
			};
		}

		@Override
		public Optional<Integer> key(Object... arguments) {
			conn.checkInTransaction(true);
			closeResults();
			try {
				setParams(arguments);
				long pre = nanoTime();
				int numRows = s.executeUpdate();
				long post = nanoTime();
				statementLength(s, pre, post);
				if (numRows < 1) {
					return Optional.empty();
				}
				log.debug("opening result set in {}", getDebugContext());
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
}
