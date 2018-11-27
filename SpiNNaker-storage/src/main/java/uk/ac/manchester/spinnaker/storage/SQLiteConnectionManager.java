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

	protected SQLiteConnectionManager(ConnectionProvider connProvider) {
		this.connProvider = connProvider;
	}

	@FunctionalInterface
	interface CallWithResult<T> {
		T call(Connection conn) throws SQLException;
	}

	@FunctionalInterface
	interface CallWithoutResult {
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
