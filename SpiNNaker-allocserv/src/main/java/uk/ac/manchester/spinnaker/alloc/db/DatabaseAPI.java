/*
 * Copyright (c) 2018-2022 The University of Manchester
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

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.dao.PermissionDeniedDataAccessException;

import com.google.errorprone.annotations.CompileTimeConstant;
import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.storage.GeneratesID;
import uk.ac.manchester.spinnaker.storage.SingleRowResult;
import uk.ac.manchester.spinnaker.utils.MappableIterable;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * The primary interface to the database. It conceptually represents an SQLite
 * database that can have connections opened on it.
 *
 * @author Donal Fellows
 */
public interface DatabaseAPI {
	/**
	 * Get a connection. This connection is thread-bound and pooled; it <em>must
	 * not</em> be passed to other threads. They should get their own
	 * connections instead. The connection has auto-commit disabled; use the
	 * {@link Connection#transaction(DatabaseEngine.TransactedWithResult)
	 * transaction()} method instead.
	 * <p>
	 * Note that if an in-memory database is used (see
	 * {@link #getInMemoryDB()}), that DB can <em>only</em> be accessed from the
	 * connection returned from this method; the next call to this method
	 * (whether from the current thread or another one) will get an independent
	 * database. Such in-memory databases are not subject to thread-bound
	 * cleanup actions; they're simply deleted from memory when no longer used
	 * (but the connection should be {@code close()}d after use for efficiency
	 * nonetheless).
	 * <p>
	 * This would be marked with {@link MustBeClosed} except that causes a mess
	 * elsewhere.
	 *
	 * @return A configured initialised connection to the database.
	 */
	@MustBeClosed
	Connection getConnection();

	/**
	 * Create an engine interface for an in-memory database. This is intended
	 * mainly for testing purposes. Note that various coupled automatic services
	 * are disabled, in particular connections are not closed automatically.
	 *
	 * @return The in-memory database interface.
	 */
	DatabaseAPI getInMemoryDB();

	/**
	 * A connection manager and transaction runner. If the {@code operation}
	 * completes normally (and this isn't a nested use), the transaction
	 * commits. If an exception is thrown, the transaction is rolled back. The
	 * connection is closed up in any case.
	 *
	 * @param lockForWriting
	 *            Whether to lock for writing. Multiple read locks can be held
	 *            at once, but only one write lock. Locks <em>cannot</em> be
	 *            upgraded (because that causes deadlocks).
	 * @param operation
	 *            The operation to run
	 * @throws RuntimeException
	 *             If something goes wrong with the database access or the
	 *             contained code.
	 */
	void executeVoid(boolean lockForWriting, Connected operation);

	/**
	 * A connection manager and transaction runner. If the {@code operation}
	 * completes normally (and this isn't a nested use), the transaction
	 * commits. If an exception is thrown, the transaction is rolled back. The
	 * connection is closed up in any case. This uses a write lock.
	 *
	 * @param operation
	 *            The operation to run
	 * @throws RuntimeException
	 *             If something goes wrong with the database access or the
	 *             contained code.
	 */
	default void executeVoid(Connected operation) {
		executeVoid(true, operation);
	}

	/**
	 * A connection manager and transaction runner. If the {@code operation}
	 * completes normally (and this isn't a nested use), the transaction
	 * commits. If an exception is thrown, the transaction is rolled back. The
	 * connection is closed up in any case.
	 *
	 * @param <T>
	 *            The type of the result of {@code operation}
	 * @param lockForWriting
	 *            Whether to lock for writing. Multiple read locks can be held
	 *            at once, but only one write lock. Locks <em>cannot</em> be
	 *            upgraded (because that causes deadlocks).
	 * @param operation
	 *            The operation to run
	 * @return the value returned by {@code operation}
	 * @throws RuntimeException
	 *             If something other than database access goes wrong with the
	 *             contained code.
	 */
	<T> T execute(boolean lockForWriting, ConnectedWithResult<T> operation);

	/**
	 * A connection manager and transaction runner. If the {@code operation}
	 * completes normally (and this isn't a nested use), the transaction
	 * commits. If an exception is thrown, the transaction is rolled back. The
	 * connection is closed up in any case. This uses a write lock.
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
	default <T> T execute(ConnectedWithResult<T> operation) {
		return execute(true, operation);
	}

	/**
	 * Creates a backup of the database. <em>This operation should only be
	 * called by administrators.</em>
	 *
	 * @param backupFilename
	 *            The backup file to create.
	 */
	void createBackup(File backupFilename);

	/**
	 * Restores the database from backup. <em>This operation should only be
	 * called by administrators.</em>
	 *
	 * @param backupFilename
	 *            The backup file to restore from.
	 * @throws PermissionDeniedDataAccessException
	 *             If the backup cannot be read.
	 */
	void restoreFromBackup(File backupFilename);

	/**
	 * Connections made by the database engine bean. Its methods do not throw
	 * checked exceptions. The connection is thread-bound, and will be cleaned
	 * up correctly when the thread exits (ideal for thread pools).
	 */
	interface Connection extends AutoCloseable {
		/**
		 * Closes this connection and releases any resources. The actual
		 * underlying connection may remain open if the connection pool wishes
		 * to maintain it, but this handle should not be retained by the caller
		 * after this point.
		 *
		 * @see java.sql.Connection#close()
		 */
		@Override
		void close();

		/**
		 * Undoes all changes made in the current transaction and releases any
		 * database locks currently held by this connection.
		 * <p>
		 * This method should be used only when in a transaction; it is only
		 * required when the transaction is to be rolled back without throwing
		 * an exception, as the normal behaviour of the internal transaction
		 * manager is to roll the transaction back when an exception leaves the
		 * code inside the transaction boundary.
		 *
		 * @see java.sql.Connection#rollback()
		 */
		void rollback();

		/**
		 * Retrieves whether this connection is in read-only mode.
		 *
		 * @return {@code true} if this connection is read-only; {@code false}
		 *         otherwise.
		 * @see java.sql.Connection#isReadOnly()
		 */
		boolean isReadOnly();

		/**
		 * Whether the historical data DB is available. If it isn't, you can't
		 * move any data to longer-term storage, but ordinary operations should
		 * be fine.
		 *
		 * @return Whether the historical data DB is available.
		 */
		boolean isHistoricalDBAvailable();

		/**
		 * A nestable transaction runner. If the {@code operation} completes
		 * normally (and this isn't a nested use), the transaction commits. If
		 * an exception is thrown, the transaction is rolled back.
		 *
		 * @param lockForWriting
		 *            Whether to lock for writing. Multiple read locks can be
		 *            held at once, but only one write lock. Locks
		 *            <em>cannot</em> be upgraded (because that causes
		 *            deadlocks).
		 * @param operation
		 *            The operation to run
		 * @see #transaction(DatabaseEngine.TransactedWithResult)
		 */
		void transaction(boolean lockForWriting, Transacted operation);

		/**
		 * A nestable transaction runner. If the {@code operation} completes
		 * normally (and this isn't a nested use), the transaction commits. If
		 * an exception is thrown, the transaction is rolled back. This uses a
		 * write lock.
		 *
		 * @param operation
		 *            The operation to run
		 * @see #transaction(DatabaseEngine.TransactedWithResult)
		 */
		void transaction(Transacted operation);

		/**
		 * A nestable transaction runner. If the {@code operation} completes
		 * normally (and this isn't a nested use), the transaction commits. If
		 * an exception is thrown, the transaction is rolled back. This uses a
		 * write lock.
		 *
		 * @param <T>
		 *            The type of the result of {@code operation}
		 * @param operation
		 *            The operation to run
		 * @return the value returned by {@code operation}
		 * @see #transaction(DatabaseEngine.Transacted)
		 */
		<T> T transaction(TransactedWithResult<T> operation);

		/**
		 * A nestable transaction runner. If the {@code operation} completes
		 * normally (and this isn't a nested use), the transaction commits. If
		 * an exception is thrown, the transaction is rolled back.
		 *
		 * @param <T>
		 *            The type of the result of {@code operation}
		 * @param lockForWriting
		 *            Whether to lock for writing. Multiple read locks can be
		 *            held at once, but only one write lock. Locks
		 *            <em>cannot</em> be upgraded (because that causes
		 *            deadlocks).
		 * @param operation
		 *            The operation to run
		 * @return the value returned by {@code operation}
		 * @see #transaction(DatabaseEngine.Transacted)
		 */
		<T> T transaction(boolean lockForWriting,
				TransactedWithResult<T> operation);

		// @formatter:off
		/**
		 * Create a new query. Usage pattern:
		 * <pre>
		 * try (var q = conn.query(SQL_SELECT)) {
		 *     for (var row : u.call(argument1, argument2)) {
		 *         // Do something with the row
		 *     }
		 * }
		 * </pre>
		 * or:
		 * <pre>
		 * try (var q = conn.query(SQL_SELECT)) {
		 *     u.call(argument1, argument2).forEach(row -&gt; {
		 *         // Do something with the row
		 *     });
		 * }
		 * </pre>
		 *
		 * @param sql
		 *            The SQL of the query.
		 * @return The query object.
		 * @see #query(Resource)
		 * @see #update(String)
		 * @see SQLQueries
		 */
		// @formatter:on
		Query query(@CompileTimeConstant String sql);

		// @formatter:off
		/**
		 * Create a new query. Usage pattern:
		 * <pre>
		 * try (var q = conn.query(SQL_SELECT)) {
		 *     for (var row : u.call(argument1, argument2)) {
		 *         // Do something with the row
		 *     }
		 * }
		 * </pre>
		 * or:
		 * <pre>
		 * try (var q = conn.query(SQL_SELECT)) {
		 *     u.call(argument1, argument2).forEach(row -&gt; {
		 *         // Do something with the row
		 *     });
		 * }
		 * </pre>
		 *
		 * @param sql
		 *            The SQL of the query.
		 * @param lockType
		 *            Whether we expect to have a write lock. This is vital
		 * @return The query object.
		 * @see #query(Resource)
		 * @see #update(String)
		 * @see SQLQueries
		 */
		// @formatter:on
		Query query(@CompileTimeConstant String sql, boolean lockType);

		// @formatter:off
		/**
		 * Create a new query.
		 * <pre>
		 * try (var q = conn.query(sqlSelectResource)) {
		 *     for (var row : u.call(argument1, argument2)) {
		 *         // Do something with the row
		 *     }
		 * }
		 * </pre>
		 * or:
		 * <pre>
		 * try (var q = conn.query(sqlSelectResource)) {
		 *     u.call(argument1, argument2).forEach(row -&gt; {
		 *         // Do something with the row
		 *     });
		 * }
		 * </pre>
		 *
		 * @param sqlResource
		 *            Reference to the SQL of the query.
		 * @return The query object.
		 * @see #query(String)
		 * @see #update(Resource)
		 * @see SQLQueries
		 */
		// @formatter:on
		Query query(Resource sqlResource);

		// @formatter:off
		/**
		 * Create a new query.
		 * <pre>
		 * try (var q = conn.query(sqlSelectResource)) {
		 *     for (var row : u.call(argument1, argument2)) {
		 *         // Do something with the row
		 *     }
		 * }
		 * </pre>
		 * or:
		 * <pre>
		 * try (var q = conn.query(sqlSelectResource)) {
		 *     u.call(argument1, argument2).forEach(row -&gt; {
		 *         // Do something with the row
		 *     });
		 * }
		 * </pre>
		 *
		 * @param sqlResource
		 *            Reference to the SQL of the query.
		 * @param lockType
		 *            Whether we expect to have a write lock. This is vital
		 *            only when the query is an {@code UPDATE RETURNING}.
		 * @return The query object.
		 * @see #query(String)
		 * @see #update(Resource)
		 * @see SQLQueries
		 */
		// @formatter:on
		Query query(Resource sqlResource, boolean lockType);

		// @formatter:off
		/**
		 * Create a new update. Usage pattern:
		 * <pre>
		 * try (var u = conn.update(SQL_UPDATE)) {
		 *     int numRows = u.call(argument1, argument2);
		 * }
		 * </pre>
		 * or:
		 * <pre>
		 * try (var u = conn.update(SQL_INSERT)) {
		 *     for (var key : u.keys(argument1, argument2)) {
		 *         // Do something with the key
		 *     }
		 * }
		 * </pre>
		 * or even:
		 * <pre>
		 * try (var u = conn.update(SQL_INSERT)) {
		 *     u.key(argument1, argument2).ifPresent(key -&gt; {
		 *         // Do something with the key
		 *     });
		 * }
		 * </pre>
		 * <p>
		 * <strong>Note:</strong> If you use a {@code RETURNING} clause then
		 * you should use a {@link Query} with the {@code lockType} set to
		 * {@code true}.
		 *
		 * @param sql
		 *            The SQL of the update.
		 * @return The update object.
		 * @see #update(Resource)
		 * @see #query(String)
		 * @see SQLQueries
		 */
		// @formatter:on
		Update update(@CompileTimeConstant String sql);

		// @formatter:off
		/**
		 * Create a new update.
		 * <pre>
		 * try (var u = conn.update(sqlUpdateResource)) {
		 *     int numRows = u.call(argument1, argument2);
		 * }
		 * </pre>
		 * or:
		 * <pre>
		 * try (var u = conn.update(sqlInsertResource)) {
		 *     for (var key : u.keys(argument1, argument2)) {
		 *         // Do something with the key
		 *     }
		 * }
		 * </pre>
		 * or even:
		 * <pre>
		 * try (var u = conn.update(sqlInsertResource)) {
		 *     u.key(argument1, argument2).ifPresent(key -&gt; {
		 *         // Do something with the key
		 *     });
		 * }
		 * </pre>
		 * <p>
		 * <strong>Note:</strong> If you use a {@code RETURNING} clause then
		 * you should use a {@link Query} with the {@code lockType} set to
		 * {@code true}.
		 *
		 * @param sqlResource
		 *            Reference to the SQL of the update.
		 * @return The update object.
		 * @see #update(String)
		 * @see #query(Resource)
		 * @see SQLQueries
		 */
		// @formatter:on
		Update update(Resource sqlResource);
	}

	/**
	 * Some code that may be run within a transaction.
	 */
	@FunctionalInterface
	interface Transacted {
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
	interface TransactedWithResult<T> {
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
	interface Connected {
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
	interface ConnectedWithResult<T> {
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
	 * Common shared API between {@link Query} and {@link Update}.
	 *
	 * @author Donal Fellows
	 */
	interface StatementCommon extends AutoCloseable {
		/**
		 * Get the number of arguments expected when calling this statement.
		 *
		 * @return The number of arguments. Types are arbitrary (because SQLite)
		 */
		int getNumArguments();

		/**
		 * Get the set of names of columns produced when calling this statement.
		 *
		 * @return A set of names. The order is the order in the SQL producing
		 *         the result set, but this should normally be insignificant.
		 */
		Set<String> getRowColumnNames();

		/**
		 * Close this statement. This never throws a checked exception.
		 */
		@Override
		void close();

		/**
		 * Get the query plan explanation.
		 *
		 * @return A list of lines that describe the query plan.
		 * @see <a href="https://www.sqlite.org/eqp.html">SQLite
		 *      documentation</a>
		 */
		List<String> explainQueryPlan();
	}

	/**
	 * Wrapping a prepared query to be more suitable for Java 8 onwards.
	 *
	 * @author Donal Fellows
	 */
	interface Query extends StatementCommon {
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
		MappableIterable<Row> call(Object... arguments);

		/**
		 * Run the query on the given arguments. The query must be one that only
		 * produces a single row result.
		 *
		 * @param arguments
		 *            Positional argument to the query
		 * @return The single row with the results, or empty if there is no such
		 *         row.
		 * @see SingleRowResult
		 */
		Optional<Row> call1(Object... arguments);
	}

	/**
	 * Wrapping a prepared update to be more suitable for Java 8 onwards.
	 *
	 * @author Donal Fellows
	 */
	@UsedInJavadocOnly(GeneratesID.class)
	interface Update extends StatementCommon {
		/**
		 * Run the update on the given arguments.
		 *
		 * @param arguments
		 *            Positional argument to the query
		 * @return The number of rows updated
		 */
		int call(Object... arguments);

		/**
		 * Run the update on the given arguments.
		 *
		 * @param arguments
		 *            Positional arguments to the query
		 * @return The integer primary keys generated by the update.
		 * @see GeneratesID
		 */
		MappableIterable<Integer> keys(Object... arguments);

		/**
		 * Run the update on the given arguments. This is expected to generate a
		 * single integer primary key (common with {@code INSERT}).
		 *
		 * @param arguments
		 *            Positional arguments to the query
		 * @return The integer primary key generated by the update.
		 * @see GeneratesID
		 */
		Optional<Integer> key(Object... arguments);
	}
}
