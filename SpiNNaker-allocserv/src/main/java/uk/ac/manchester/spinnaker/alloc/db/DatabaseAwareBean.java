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

import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.ConnectedWithResult;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.TransactedWithResult;

/**
 * A support class to make accessing the database a bit simpler. It factors out
 * some common patterns.
 *
 * @author Donal Fellows
 */
public abstract class DatabaseAwareBean extends SQLQueries {
	/** The application database. */
	@Autowired
	private DatabaseEngine db;

	/**
	 * Get a connection to the application database. Connections <em>may</em> be
	 * shared, but might not be (especially in the case of testing databases).
	 *
	 * @return Database connection. Requires closing.
	 */
	protected final Connection getConnection() {
		return db.getConnection();
	}

	/**
	 * A connection manager and nestable transaction runner. If the
	 * {@code operation} completes normally (and this isn't a nested use), the
	 * transaction commits. If an exception is thrown, the transaction is rolled
	 * back. The connection is closed up in any case.
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
		protected AbstractSQL() {
			conn = db.getConnection();
			doClose = true;
		}

		/**
		 * Use an existing connection. Caller looks after its management.
		 *
		 * @param conn
		 *            The connection to piggy-back onto.
		 */
		protected AbstractSQL(Connection conn) {
			this.conn = conn;
			doClose = false;
		}

		/**
		 * A nestable transaction runner. If the {@code action} completes
		 * normally (and this isn't a nested use), the transaction commits. If a
		 * runtime exception is thrown, the transaction is rolled back (and the
		 * exception flows through).
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
