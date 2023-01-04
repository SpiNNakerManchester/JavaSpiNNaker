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
		if (exception.getMostSpecificCause() instanceof SQLiteException cause) {
			return isBusy(cause);
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
		if (exception instanceof SQLiteException sqliteExn) {
			return switch (sqliteExn.getResultCode()) {
			case SQLITE_BUSY -> true;
			case SQLITE_BUSY_SNAPSHOT -> true;
			case SQLITE_BUSY_TIMEOUT -> true;
			case SQLITE_BUSY_RECOVERY -> true;
			default -> false;
			};
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
		if (!(exception instanceof SQLiteException exn)) {
			if (exception.getMessage().contains("no such column: ")) {
				return restack(new InvalidResultSetAccessException(
						exception.getMessage(), trimSQLComments(sql),
						exception));
			}
			return restack(new UncategorizedSQLException(
					"general SQL exception", trimSQLComments(sql), exception));
		}
		var msg = exn.getMessage();
		boolean replaced = false;
		if (msg.contains("SQL error or missing database (")) {
			msg = msg.replaceFirst("SQL error or missing database \\((.*)\\)",
					"$1");
			replaced = true;
		}
		return switch (exn.getResultCode()) {
		case SQLITE_CONSTRAINT, SQLITE_CONSTRAINT_CHECK,
				SQLITE_CONSTRAINT_COMMITHOOK, SQLITE_CONSTRAINT_FOREIGNKEY,
				SQLITE_CONSTRAINT_FUNCTION, SQLITE_CONSTRAINT_PRIMARYKEY,
				SQLITE_CONSTRAINT_UNIQUE, SQLITE_CONSTRAINT_NOTNULL,
				SQLITE_CONSTRAINT_TRIGGER, SQLITE_CONSTRAINT_ROWID,
				SQLITE_CONSTRAINT_VTAB, SQLITE_MISMATCH ->
			restack(new DataIntegrityViolationException(msg, exn));

		case SQLITE_BUSY, SQLITE_BUSY_RECOVERY, SQLITE_BUSY_SNAPSHOT,
				SQLITE_BUSY_TIMEOUT, SQLITE_LOCKED, SQLITE_LOCKED_SHAREDCACHE ->
			restack(new PessimisticLockingFailureException(msg, exn));

		case SQLITE_ABORT, SQLITE_ABORT_ROLLBACK, SQLITE_FULL, SQLITE_EMPTY ->
			restack(new RecoverableDataAccessException(msg, exn));

		case SQLITE_SCHEMA, SQLITE_TOOBIG, SQLITE_RANGE ->
			restack(new InvalidDataAccessResourceUsageException(msg, exn));

		case SQLITE_IOERR, SQLITE_IOERR_SHORT_READ, SQLITE_IOERR_READ,
				SQLITE_IOERR_WRITE, SQLITE_IOERR_FSYNC, SQLITE_IOERR_DIR_FSYNC,
				SQLITE_IOERR_TRUNCATE, SQLITE_IOERR_FSTAT, SQLITE_IOERR_UNLOCK,
				SQLITE_IOERR_RDLOCK, SQLITE_IOERR_DELETE, SQLITE_IOERR_NOMEM,
				SQLITE_IOERR_ACCESS, SQLITE_IOERR_CHECKRESERVEDLOCK,
				SQLITE_IOERR_LOCK, SQLITE_IOERR_CLOSE, SQLITE_IOERR_SHMOPEN,
				SQLITE_IOERR_SHMSIZE, SQLITE_IOERR_SHMMAP, SQLITE_IOERR_SEEK,
				SQLITE_IOERR_DELETE_NOENT, SQLITE_IOERR_MMAP,
				SQLITE_IOERR_GETTEMPPATH, SQLITE_IOERR_CONVPATH, SQLITE_PERM,
				SQLITE_READONLY, SQLITE_READONLY_RECOVERY,
				SQLITE_READONLY_CANTLOCK, SQLITE_READONLY_ROLLBACK,
				SQLITE_READONLY_DBMOVED, SQLITE_AUTH, SQLITE_MISUSE,
				SQLITE_NOLFS, SQLITE_CORRUPT, SQLITE_CORRUPT_VTAB,
				SQLITE_CANTOPEN, SQLITE_CANTOPEN_ISDIR,
				SQLITE_CANTOPEN_FULLPATH, SQLITE_CANTOPEN_CONVPATH,
				SQLITE_NOTADB, SQLITE_FORMAT ->
			restack(new DataAccessResourceFailureException(msg, exn));

		default -> restack(replaced
				? new BadSqlGrammarException(msg, trimSQLComments(sql), exn)
				: new UncategorizedSQLException("general SQL exception",
						trimSQLComments(sql), exn));
		};
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
