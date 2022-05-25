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

import static uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.columnNames;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.mapException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * A restricted form of result set. Note that this object <em>must not</em> be
 * saved outside the context of iteration over its' query's results.
 *
 * @author Donal Fellows
 */
public final class Row {
	private final ResultSet rs;

	Row(ResultSet rs) {
		this.rs = rs;
	}

	/**
	 * Get the column names from this row.
	 *
	 * @return The set of column names; all lookup of columns is by name, so the
	 *         order is unimportant. (The set returned will iterate over the
	 *         names in the order they are in the underlying result set, but
	 *         this is considered "unimportant".)
	 */
	public Set<String> getColumnNames() {
		try {
			return columnNames(rs.getMetaData());
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A string, or {@code null} on {@code NULL}.
	 */
	public String getString(String columnLabel) {
		try {
			return rs.getString(columnLabel);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	/**
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the string from the column of a row.
	 */
	public static Function<Row, String> string(String columnLabel) {
		return r -> r.getString(columnLabel);
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A boolean, or {@code false} on {@code NULL}.
	 */
	public boolean getBoolean(String columnLabel) {
		try {
			return rs.getBoolean(columnLabel);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	/**
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the {@code boolean} from the column of a row.
	 */
	public static Function<Row, Boolean> bool(String columnLabel) {
		return r -> r.getBoolean(columnLabel);
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return An integer, or {@code 0} on {@code NULL}.
	 */
	public int getInt(String columnLabel) {
		try {
			return rs.getInt(columnLabel);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	/**
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the {@code int} from the column of a row.
	 */
	public static ToIntFunction<Row> int32(String columnLabel) {
		return r -> r.getInt(columnLabel);
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return An integer or {@code null}.
	 */
	public Integer getInteger(String columnLabel) {
		try {
			return (Integer) rs.getObject(columnLabel);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	/**
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the nullable integer from the column of a row.
	 */
	public static Function<Row, Integer> integer(String columnLabel) {
		return r -> r.getInteger(columnLabel);
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A byte array, or {@code null} on {@code NULL}.
	 */
	public byte[] getBytes(String columnLabel) {
		try {
			return rs.getBytes(columnLabel);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	/**
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the byte array from the column of a row.
	 */
	public static Function<Row, byte[]> bytes(String columnLabel) {
		return r -> r.getBytes(columnLabel);
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return An instant, or {@code null} on {@code NULL}.
	 */
	public Instant getInstant(String columnLabel) {
		try {
			long moment = rs.getLong(columnLabel);
			if (rs.wasNull()) {
				return null;
			}
			return Instant.ofEpochSecond(moment);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	/**
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the instant from the column of a row.
	 */
	public static Function<Row, Instant> instant(String columnLabel) {
		return r -> r.getInstant(columnLabel);
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A duration, or {@code null} on {@code NULL}.
	 */
	public Duration getDuration(String columnLabel) {
		try {
			long span = rs.getLong(columnLabel);
			if (rs.wasNull()) {
				return null;
			}
			return Duration.ofSeconds(span);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	/**
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the duration from the column of a row.
	 */
	public static Function<Row, Duration> duration(String columnLabel) {
		return r -> r.getDuration(columnLabel);
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return An automatically-decoded object, or {@code null} on {@code NULL}.
	 *         (Only returns basic types.)
	 */
	public Object getObject(String columnLabel) {
		try {
			return rs.getObject(columnLabel);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	/**
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the object from the column of a row.
	 */
	public static Function<Row, Object> object(String columnLabel) {
		return r -> r.getObject(columnLabel);
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
	 */
	public <T extends Enum<T>> T getEnum(String columnLabel, Class<T> type) {
		try {
			int value = rs.getInt(columnLabel);
			if (rs.wasNull()) {
				return null;
			}
			return type.getEnumConstants()[value];
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	/**
	 * Get a function to get the contents of the named column.
	 *
	 * @param <T>
	 *            The enumeration type.
	 * @param columnLabel
	 *            The name of the column.
	 * @param type
	 *            The enumeration type class.
	 * @return A function to get the {@code enum} from the column of a row.
	 */
	public static <T extends Enum<T>> Function<Row, T>
			enumerate(String columnLabel, Class<T> type) {
		return r -> r.getEnum(columnLabel, type);
	}

	/**
	 * Get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A long value, or {@code null} on {@code NULL}.
	 */
	public Long getLong(String columnLabel) {
		try {
			var value = (Number) rs.getObject(columnLabel);
			if (rs.wasNull()) {
				return null;
			}
			return value.longValue();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	/**
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the nullable {@code long} from the column of a
	 *         row.
	 */
	public static Function<Row, Long> int64(String columnLabel) {
		return r -> r.getLong(columnLabel);
	}
}
