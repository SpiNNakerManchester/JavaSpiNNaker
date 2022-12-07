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

import static java.lang.System.arraycopy;
import static java.util.Objects.isNull;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.sqlite.SQLiteException;

/**
 * Miscellaneous database-related utility operations.
 *
 * @author Donal Fellows
 */
public abstract class Utils {
	private static final int TRIM_LENGTH = 80;

	private static final String ELLIPSIS = "...";

	private Utils() {
	}

	/**
	 * Exclude comments and compress whitespace from the SQL of a statement.
	 *
	 * @param sql
	 *            The text of the SQL to trim.
	 * @return The trimmed SQL.
	 */
	public static String trimSQL(String sql) {
		return trimSQL(sql, TRIM_LENGTH);
	}

	/**
	 * Exclude comments and compress whitespace from the SQL of a statement.
	 *
	 * @param sql
	 *            The text of the SQL to trim.
	 * @param length
	 *            The point to insert an ellipsis if required.
	 * @return The trimmed SQL.
	 */
	public static String trimSQL(String sql, int length) {
		if (isNull(sql)) {
			return null;
		}
		sql = trimSQLComments(sql);
		// Try to trim long queries to no more than TRIM_LENGTH...
		var sql2 = sql.replaceAll("^(.{0," + length + "})\\b.*$", "$1");
		// If that did nothing, return the whole string
		if (sql2.equals(sql)) {
			return sql2;
		}
		// We're using the trimming, so add an ellipsis
		return sql2 + ELLIPSIS;
	}

	private static String trimSQLComments(String sql) {
		if (isNull(sql)) {
			return null;
		}
		return sql.replaceAll("--[^\n]*\n", " ").replaceAll("\\s+", " ")
				.strip();
	}

	/**
	 * Utility for testing whether an exception was thrown because the database
	 * was busy.
	 *
	 * @param exception
	 *            The outer wrapping exception.
	 * @return Whether it was caused by the database being busy.
	 */
	public static boolean isBusy(DataAccessException exception) {
		var root = exception.getMostSpecificCause();
		if (root instanceof SQLiteException) {
			return isBusy((SQLiteException) root);
		}
		return false;
	}

	/**
	 * Utility for testing whether an exception was thrown because the database
	 * was busy.
	 *
	 * @param exception
	 *            The exception to test.
	 * @return Whether it was caused by the database being busy.
	 */
	public static boolean isBusy(SQLException exception) {
		if (exception instanceof SQLiteException) {
			switch (((SQLiteException) exception).getResultCode()) {
			case SQLITE_BUSY:
			case SQLITE_BUSY_SNAPSHOT:
			case SQLITE_BUSY_TIMEOUT:
			case SQLITE_BUSY_RECOVERY:
				return true;
			default:
				return false;
			}
		}
		return false;
	}

	/**
	 * Convert an SQL-related exception into an unchecked exception.
	 *
	 * @param exception
	 *            The exception to convert.
	 * @param sql
	 *            Optional SQL that caused the problem. May be {@code null}.
	 * @return The converted exception; this is <em>not</em> thrown by this
	 *         method!
	 */
	public static DataAccessException mapException(SQLException exception,
			String sql) {
		if (!(exception instanceof SQLiteException)) {
			if (exception.getMessage().contains("no such column: ")) {
				return restack(new InvalidResultSetAccessException(
						exception.getMessage(), trimSQLComments(sql),
						exception));
			}
			return restack(new UncategorizedSQLException(
					"general SQL exception", trimSQLComments(sql), exception));
		}
		var exn = (SQLiteException) exception;
		var msg = exn.getMessage();
		boolean replaced = false;
		if (msg.contains("SQL error or missing database (")) {
			msg = msg.replaceFirst("SQL error or missing database \\((.*)\\)",
					"$1");
			replaced = true;
		}
		switch (exn.getResultCode()) {
		case SQLITE_CONSTRAINT:
		case SQLITE_CONSTRAINT_CHECK:
		case SQLITE_CONSTRAINT_COMMITHOOK:
		case SQLITE_CONSTRAINT_FOREIGNKEY:
		case SQLITE_CONSTRAINT_FUNCTION:
		case SQLITE_CONSTRAINT_PRIMARYKEY:
		case SQLITE_CONSTRAINT_UNIQUE:
		case SQLITE_CONSTRAINT_NOTNULL:
		case SQLITE_CONSTRAINT_TRIGGER:
		case SQLITE_CONSTRAINT_ROWID:
		case SQLITE_CONSTRAINT_VTAB:
		case SQLITE_MISMATCH:
			return restack(new DataIntegrityViolationException(msg, exn));

		case SQLITE_BUSY:
		case SQLITE_BUSY_RECOVERY:
		case SQLITE_BUSY_SNAPSHOT:
		case SQLITE_LOCKED:
		case SQLITE_LOCKED_SHAREDCACHE:
			return restack(new PessimisticLockingFailureException(msg, exn));

		case SQLITE_ABORT:
		case SQLITE_ABORT_ROLLBACK:
		case SQLITE_FULL:
		case SQLITE_EMPTY:
			return restack(new RecoverableDataAccessException(msg, exn));

		case SQLITE_SCHEMA:
		case SQLITE_TOOBIG:
		case SQLITE_RANGE:
			return restack(
					new InvalidDataAccessResourceUsageException(msg, exn));

		case SQLITE_IOERR:
		case SQLITE_IOERR_SHORT_READ:
		case SQLITE_IOERR_READ:
		case SQLITE_IOERR_WRITE:
		case SQLITE_IOERR_FSYNC:
		case SQLITE_IOERR_DIR_FSYNC:
		case SQLITE_IOERR_TRUNCATE:
		case SQLITE_IOERR_FSTAT:
		case SQLITE_IOERR_UNLOCK:
		case SQLITE_IOERR_RDLOCK:
		case SQLITE_IOERR_DELETE:
		case SQLITE_IOERR_NOMEM:
		case SQLITE_IOERR_ACCESS:
		case SQLITE_IOERR_CHECKRESERVEDLOCK:
		case SQLITE_IOERR_LOCK:
		case SQLITE_IOERR_CLOSE:
		case SQLITE_IOERR_SHMOPEN:
		case SQLITE_IOERR_SHMSIZE:
		case SQLITE_IOERR_SHMMAP:
		case SQLITE_IOERR_SEEK:
		case SQLITE_IOERR_DELETE_NOENT:
		case SQLITE_IOERR_MMAP:
		case SQLITE_IOERR_GETTEMPPATH:
		case SQLITE_IOERR_CONVPATH:
		case SQLITE_PERM:
		case SQLITE_READONLY:
		case SQLITE_READONLY_RECOVERY:
		case SQLITE_READONLY_CANTLOCK:
		case SQLITE_READONLY_ROLLBACK:
		case SQLITE_READONLY_DBMOVED:
		case SQLITE_AUTH:
		case SQLITE_MISUSE:
		case SQLITE_NOLFS:
		case SQLITE_CORRUPT:
		case SQLITE_CORRUPT_VTAB:
		case SQLITE_CANTOPEN:
		case SQLITE_CANTOPEN_ISDIR:
		case SQLITE_CANTOPEN_FULLPATH:
		case SQLITE_CANTOPEN_CONVPATH:
		case SQLITE_NOTADB:
		case SQLITE_FORMAT:
			return restack(new DataAccessResourceFailureException(msg, exn));

		default:
			if (replaced) {
				return restack(new BadSqlGrammarException(msg,
						trimSQLComments(sql), exn));
			}
			return restack(new UncategorizedSQLException(
					"general SQL exception", trimSQLComments(sql), exn));
		}
	}

	// 2 = restack() and mapException() themselves
	private static final int CHOP_LEN = 2;

	private static <T extends Throwable> T restack(T exn) {
		exn.fillInStackTrace();
		var st = exn.getStackTrace();
		var newst = new StackTraceElement[st.length - CHOP_LEN];
		arraycopy(st, CHOP_LEN, newst, 0, newst.length);
		exn.setStackTrace(newst);
		return exn;
	}
}
