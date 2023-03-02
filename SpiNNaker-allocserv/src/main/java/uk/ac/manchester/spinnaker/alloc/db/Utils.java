/*
 * Copyright (c) 2021 The University of Manchester
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

import static java.lang.System.arraycopy;
import static java.util.Objects.isNull;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransientConnectionException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.UncategorizedSQLException;

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
		if (root instanceof SQLException) {
			return isBusy((SQLException) root);
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
		return exception instanceof SQLTimeoutException
				|| exception instanceof SQLTransientConnectionException;
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
		if (exception.getMessage().contains("no such column: ")) {
			return restack(new InvalidResultSetAccessException(
					exception.getMessage(), trimSQLComments(sql), exception));
		}
		return restack(new UncategorizedSQLException("general SQL exception",
				trimSQLComments(sql), exception));
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
