/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.db;

import java.util.List;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;

import com.google.errorprone.annotations.CompileTimeConstant;
import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.storage.GeneratesID;
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
	 *
	 * @return A configured initialised connection to the database.
	 */
	@MustBeClosed
	Connection getConnection();

	/**
	 * Whether the historical data DB is available. If it isn't, you can't
	 * move any data to longer-term storage, but ordinary operations should
	 * be fine.
	 *
	 * @return Whether the historical data DB is available.
	 */
	boolean isHistoricalDBAvailable();

	/**
	 * Get a connection to the historical database, similar to the above.
	 *
	 * @return A configured initialised connection to the historical database.
	 */
	@MustBeClosed
	Connection getHistoricalConnection();

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
		 * Close this statement. This never throws a checked exception.
		 */
		@Override
		void close();

		/**
		 * Get the list of parameters to the statement. This may be an expensive
		 * operation.
		 *
		 * @return List of parameter names. Unnamed parameters <em>may</em> be
		 *         mapped as {@code null}.
		 */
		List<String> getParameters();
	}

	/**
	 * Wrapping a prepared query to be more suitable for Java 8 onwards.
	 *
	 * @author Donal Fellows
	 */
	interface Query extends StatementCommon {
		/**
		 * Run the query on the given arguments to read a list of objects.
		 *
		 * @param <T> The type of the result.
		 * @param mapper
		 *            Mapper that gets an object from a row set.
		 * @param arguments
		 *            Positional argument to the query
		 * @return The resulting list of objects.
		 */
		<T> List<T> call(RowMapper<T> mapper, Object... arguments);

		/**
		 * Run the query on the given arguments to read a single object.
		 *
		 * @param <T> The type of the result.
		 * @param mapper
		 *            Mapper that gets an object from a row set.
		 * @param arguments
		 *            Positional argument to the query.
		 * @return The resulting object.
		 */
		<T> Optional<T> call1(RowMapper<T> mapper, Object... arguments);

		/**
		 * Get the column names from the result set. This may be an expensive
		 * operation.
		 *
		 * @return The list of column names. The names of columns where they are
		 *         not assigned by the query is arbitrary and up to the database
		 *         engine. This will be {@code null} if the manipulation of
		 *         metadata fails; this isn't expected.
		 * @throws DataAccessException
		 *             If the statement is invalid SQL.
		 */
		List<String> getColumns() throws DataAccessException;
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

	/**
	 * Maps database Row to an object.
	 *
	 * @param <T> The type of object returned
	 */
	@FunctionalInterface
	interface RowMapper<T> {

		/**
		 * Map a row to an object.
		 *
		 * @param row The row to map.
		 *
		 * @return The object created from the row.
		 */
		T mapRow(Row row);
	}
}
