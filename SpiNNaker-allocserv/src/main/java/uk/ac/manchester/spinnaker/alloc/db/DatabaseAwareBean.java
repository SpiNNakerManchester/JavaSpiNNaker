/*
 * Copyright (c) 2021 The University of Manchester
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

import static java.util.Objects.requireNonNull;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.ConnectedWithResult;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.TransactedWithResult;

/**
 * A support class to make accessing the database a bit simpler. It factors out
 * some common patterns.
 *
 * @author Donal Fellows
 */
public abstract class DatabaseAwareBean extends SQLQueries {
	/** The application database. */
	private DatabaseEngine db;

	@Autowired
	final void setDatabaseEngine(DatabaseEngine db) {
		this.db = requireNonNull(db, "DatabaseEngine must not be null");
	}

	/**
	 * Get a connection to the application database. Connections <em>may</em> be
	 * shared, but might not be (especially in the case of testing databases).
	 *
	 * @return Database connection. Requires closing.
	 */
	@MustBeClosed
	protected final Connection getConnection() {
		return db.getConnection();
	}

	/**
	 * A connection manager and nestable transaction runner. If the
	 * {@code operation} completes normally (and this isn't a nested use), the
	 * transaction commits. If an exception is thrown, the transaction is rolled
	 * back. The connection is closed up in any case. A write lock is used.
	 * <p>
	 * It is the caller's responsibility to ensure that the correct transaction
	 * type is used.
	 *
	 * @param <T>
	 *            The type of the result of {@code operation}
	 * @param operation
	 *            The operation to run
	 * @return the value returned by {@code operation}
	 * @throws RuntimeException
	 *             If something goes wrong with the contained code.
	 */
	protected <T> T execute(ConnectedWithResult<T> operation) {
		return db.execute(operation);
	}

	/**
	 * A connection manager and nestable read-only transaction runner (it is an
	 * error to do an {@code UPDATE} using this transaction). If the
	 * {@code operation} completes normally (and this isn't a nested use), the
	 * transaction commits. If an exception is thrown, the transaction is rolled
	 * back. The connection is closed up in any case. A read lock (i.e., shared)
	 * is used.
	 * <p>
	 * It is the caller's responsibility to ensure that the correct transaction
	 * type is used; read locks <em>may not</em> be upgraded to write locks (due
	 * to deadlock risk).
	 *
	 * @param <T>
	 *            The type of the result of {@code operation}
	 * @param operation
	 *            The operation to run
	 * @return the value returned by {@code operation}
	 * @throws RuntimeException
	 *             If something goes wrong with the contained code.
	 */
	protected <T> T executeRead(ConnectedWithResult<T> operation) {
		return db.execute(false, operation);
	}

	/**
	 * Encapsulation of a connection. Can either do the management itself or use
	 * a connection managed outside; the difference is important mainly during
	 * testing as tests often use in-memory DBs.
	 *
	 * @author Donal Fellows
	 */
	protected abstract class AbstractSQL implements AutoCloseable {
		/** The connection. */
		protected final Connection conn;

		private final boolean doClose;

		/** Manage a connection ourselves. */
		// Should be @MustBeClosed but that gets messy in subclasses
		@SuppressWarnings("MustBeClosed")
		protected AbstractSQL() {
			conn = requireNonNull(db, "DatabaseEngine not set").getConnection();
			doClose = true;
		}

		/**
		 * Use an existing connection. Caller looks after its management.
		 *
		 * @param conn
		 *            The connection to piggy-back onto.
		 */
		protected AbstractSQL(Connection conn) {
			this.conn = requireNonNull(conn, "a connection must be given");
			doClose = false;
		}

		/**
		 * A nestable transaction runner. If the {@code action} completes
		 * normally (and this isn't a nested use), the transaction commits. If a
		 * runtime exception is thrown, the transaction is rolled back (and the
		 * exception flows through). A write lock is used.
		 *
		 * @param <T>
		 *            The type of the result of {@code action}
		 * @param action
		 *            The code to run inside the transaction.
		 * @return Whatever the {@code action} returns.
		 */
		public final <T> T transaction(TransactedWithResult<T> action) {
			return conn.transaction(action);
		}

		/**
		 * A nestable transaction runner. If the {@code action} completes
		 * normally (and this isn't a nested use), the transaction commits. If a
		 * runtime exception is thrown, the transaction is rolled back (and the
		 * exception flows through).
		 *
		 * @param <T>
		 *            The type of the result of {@code action}
		 * @param lockForWriting
		 *            Whether to lock for writing. Multiple read locks can be
		 *            held at once, but only one write lock. Locks
		 *            <em>cannot</em> be upgraded (because that causes
		 *            deadlocks).
		 * @param action
		 *            The code to run inside the transaction.
		 * @return Whatever the {@code action} returns.
		 */
		public final <T> T transaction(boolean lockForWriting,
				TransactedWithResult<T> action) {
			return conn.transaction(lockForWriting, action);
		}

		/** @return The encapsulated connection. */
		public Connection getConnection() {
			return conn;
		}

		@Override
		public void close() {
			if (doClose) {
				conn.close();
			}
		}
	}
}
