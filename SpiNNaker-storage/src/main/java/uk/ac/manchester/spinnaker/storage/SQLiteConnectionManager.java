package uk.ac.manchester.spinnaker.storage;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wrapper that handles how to perform transactions.
 *
 * @author Donal Fellows
 */
abstract class SQLiteConnectionManager {
	private final ConnectionProvider connProvider;

	/**
	 * @param connProvider
	 *            The source of database connections.
	 * @see Connection
	 */
	protected SQLiteConnectionManager(ConnectionProvider connProvider) {
		this.connProvider = connProvider;
	}

	/**
	 * A wrapped piece of code that produces a result.
	 *
	 * @param <T>
	 *            The type of the result of the call
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	interface CallWithResult<T> {
		/**
		 * The wrapped code.
		 *
		 * @param conn
		 *            The connection that has a running transaction on it.
		 * @return The result of the code.
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		T call(Connection conn) throws SQLException;
	}

	/**
	 * A wrapped piece of code that doesn't produce a result.
	 *
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	interface CallWithoutResult {
		/**
		 * The wrapped code.
		 *
		 * @param conn
		 *            The connection that has a running transaction on it.
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		void call(Connection conn) throws SQLException;
	}

	/**
	 * Wrapper for applying a transaction correctly.
	 *
	 * @param call
	 *            What is wrapped
	 * @param actionDescription
	 *            Extra message to use with wrapping exception
	 * @return The value returned by the call
	 * @throws StorageException
	 *             If anything goes wrong
	 */
	final <T> T callR(CallWithResult<T> call, String actionDescription)
			throws StorageException {
		try (Connection conn = connProvider.getConnection()) {
			conn.setAutoCommit(false);
			try {
				T result = call.call(conn);
				conn.commit();
				return result;
			} catch (Exception e) {
				conn.rollback();
				throw e;
			}
		} catch (SQLException | IllegalStateException e) {
			throw new StorageException("while " + actionDescription, e);
		}
	}

	/**
	 * Wrapper for applying a transaction correctly.
	 *
	 * @param call
	 *            What is wrapped
	 * @param actionDescription
	 *            Extra message to use with wrapping exception
	 * @throws StorageException
	 *             If anything goes wrong
	 */
	final void callV(CallWithoutResult call, String actionDescription)
			throws StorageException {
		try (Connection conn = connProvider.getConnection()) {
			conn.setAutoCommit(false);
			try {
				call.call(conn);
				conn.commit();
				return;
			} catch (Exception e) {
				conn.rollback();
				throw e;
			}
		} catch (SQLException | IllegalStateException e) {
			throw new StorageException("while " + actionDescription, e);
		}
	}
}
