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

import static uk.ac.manchester.spinnaker.alloc.IOUtils.deserialize;
import static uk.ac.manchester.spinnaker.alloc.db.DatabaseEngineSQLiteImpl.columnNames;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.mapException;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.TypeMismatchDataAccessException;

import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * A restricted form of result set. Note that this object <em>must not</em> be
 * saved outside the context of iteration over its' query's results.
 *
 * @author Donal Fellows
 */
@UsedInJavadocOnly(DataAccessException.class)
public final class RowResultSetImpl implements Row {
	private final ResultSet rs;

	private Set<String> columns;

	RowResultSetImpl(ResultSet rs) {
		this.rs = rs;
	}

	private String getSQL() {
		try {
			return rs.getStatement().toString();
		} catch (SQLException e) {
			return null;
		}
	}

	/**
	 * Produce a value or throw. Only used in {@link Row}.
	 *
	 * @param <T>
	 *            The type of value to produce.
	 */
	private interface Getter<T> {
		/**
		 * Produce a value or throw.
		 *
		 * @return The value produced.
		 * @throws SQLException
		 *             If things fail.
		 */
		T get() throws SQLException;
	}

	/**
	 * Handles exception mapping if an exception is thrown.
	 *
	 * @param <T>
	 *            The type of the result.
	 * @param getter
	 *            How to get the result. May throw.
	 * @return The result.
	 * @throws DataAccessException
	 *             If the interior code throws an {@link SQLException}.
	 */
	private <T> T get(Getter<T> getter) {
		try {
			return getter.get();
		} catch (SQLException e) {
			throw mapException(e, getSQL());
		}
	}

	/**
	 * Get the column names from this row.
	 *
	 * @return The set of column names; all lookup of columns is by name, so the
	 *         order is unimportant. (The set returned will iterate over the
	 *         names in the order they are in the underlying result set, but
	 *         this is considered "unimportant".)
	 * @throws DataAccessException
	 *             If the column names can't be retrieved.
	 */
	public Set<String> getColumnNames() {
		if (columns != null) {
			return columns;
		}
		return get(() -> {
			columns = columnNames(rs.getMetaData());
			return columns;
		});
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A string, or {@code null} on {@code NULL}.
	 * @throws DataAccessException
	 *             If the column's contents can't be retrieved.
	 */
	public String getString(String columnLabel) {
		return get(() -> rs.getString(columnLabel));
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A boolean, or {@code false} on {@code NULL}.
	 * @throws DataAccessException
	 *             If the column's contents can't be retrieved.
	 */
	public boolean getBoolean(String columnLabel) {
		return get(() -> rs.getBoolean(columnLabel));
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return An integer, or {@code 0} on {@code NULL}.
	 * @throws DataAccessException
	 *             If the column's contents can't be retrieved.
	 */
	public int getInt(String columnLabel) {
		return get(() -> rs.getInt(columnLabel));
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return An integer or {@code null}.
	 * @throws DataAccessException
	 *             If the column's contents can't be retrieved.
	 */
	public Integer getInteger(String columnLabel) {
		return get(() -> (Integer) rs.getObject(columnLabel));
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A byte array, or {@code null} on {@code NULL}.
	 * @throws DataAccessException
	 *             If the column's contents can't be retrieved.
	 */
	public byte[] getBytes(String columnLabel) {
		return get(() -> rs.getBytes(columnLabel));
	}

	/**
	 * Get the contents of the named column by deserialization.
	 *
	 * @param <T>
	 *            The type of value expected.
	 * @param columnLabel
	 *            The name of the column.
	 * @param cls
	 *            The type of value expected.
	 * @return A deserialized object, or {@code null} on {@code NULL}.
	 * @throws DataAccessException
	 *             If the column's contents can't be retrieved.
	 * @throws TypeMismatchDataAccessException
	 *             If the object is not of the required type.
	 */
	public <T> T getSerial(String columnLabel, Class<T> cls) {
		var bytes = getBytes(columnLabel);
		if (bytes == null) {
			return null;
		}
		try {
			return deserialize(bytes, cls);
		} catch (IOException | ClassNotFoundException | ClassCastException e) {
			throw new TypeMismatchDataAccessException(
					"bad data in column " + columnLabel, e);
		}
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return An instant, or {@code null} on {@code NULL}.
	 * @throws DataAccessException
	 *             If the column's contents can't be retrieved.
	 */
	public Instant getInstant(String columnLabel) {
		return get(() -> {
			long moment = rs.getLong(columnLabel);
			if (rs.wasNull()) {
				return null;
			}
			return Instant.ofEpochSecond(moment);
		});
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A duration, or {@code null} on {@code NULL}.
	 * @throws DataAccessException
	 *             If the column's contents can't be retrieved.
	 */
	public Duration getDuration(String columnLabel) {
		return get(() -> {
			long span = rs.getLong(columnLabel);
			if (rs.wasNull()) {
				return null;
			}
			return Duration.ofSeconds(span);
		});
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return An automatically-decoded object, or {@code null} on {@code NULL}.
	 *         (Only returns basic types due to the way SQLite type affinities
	 *         work; {@link Integer}, {@link Double}, {@link String}, or
	 *         {@code byte[]}.)
	 * @throws DataAccessException
	 *             If the column's contents can't be retrieved.
	 */
	public Object getObject(String columnLabel) {
		return get(() -> rs.getObject(columnLabel));
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param <T>
	 *            The enumeration type.
	 * @param columnLabel
	 *            The name of the column.
	 * @param type
	 *            The enumeration type class.
	 * @return An enum value, or {@code null} on {@code NULL}.
	 * @throws DataAccessException
	 *             If the column's contents can't be retrieved.
	 */
	public <T extends Enum<T>> T getEnum(String columnLabel, Class<T> type) {
		return get(() -> {
			int value = rs.getInt(columnLabel);
			if (rs.wasNull()) {
				return null;
			}
			return type.getEnumConstants()[value];
		});
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A long value, or {@code null} on {@code NULL}.
	 * @throws DataAccessException
	 *             If the column's contents can't be retrieved.
	 */
	public Long getLong(String columnLabel) {
		return get(() -> {
			var value = (Number) rs.getObject(columnLabel);
			if (rs.wasNull()) {
				return null;
			}
			return value.longValue();
		});
	}

	@Override
	public String toString() {
		var sb = new StringBuilder("Row(");
		try {
			var md = rs.getMetaData();
			var sep = "";
			for (int i = 1; i <= md.getColumnCount(); i++) {
				var col = md.getColumnName(i);
				var val = rs.getObject(i);
				sb.append(sep).append(col).append(":").append(val);
				sep = ", ";
			}
		} catch (Exception e) {
			// Can't get the contents of the row after all
			sb.append("...");
		}
		return sb.append(")").toString();
	}
}
