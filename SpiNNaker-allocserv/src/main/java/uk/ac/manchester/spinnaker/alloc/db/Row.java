/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.db;

import static uk.ac.manchester.spinnaker.alloc.IOUtils.deserialize;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.mapException;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.TypeMismatchDataAccessException;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.RowMapper;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.utils.MappableIterable;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * A restricted form of result set. Note that this object <em>must not</em> be
 * saved outside the context of iteration over its' query's results.
 *
 * @author Donal Fellows
 */
@UsedInJavadocOnly(DataAccessException.class)
public final class Row {
	private final ResultSet rs;

	private Set<String> columns;

	Row(ResultSet rs) {
		this.rs = rs;
	}

	static Set<String> columnNames(ResultSetMetaData md) throws SQLException {
		var names = new LinkedHashSet<String>();
		for (int i = 1; i <= md.getColumnCount(); i++) {
			names.add(md.getColumnLabel(i));
		}
		return names;
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
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the string from the column of a row.
	 */
	public static RowMapper<String> string(String columnLabel) {
		return r -> r.getString(columnLabel);
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
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the {@code boolean} from the column of a row.
	 */
	public static RowMapper<Boolean> bool(String columnLabel) {
		return r -> r.getBoolean(columnLabel);
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
	 * @throws DataAccessException
	 *             If the column's contents can't be retrieved.
	 */
	public Integer getInteger(String columnLabel) {
		return get(() -> {
			var obj = rs.getObject(columnLabel);
			if (obj instanceof Long) {
				return ((Long) obj).intValue();
			}
			if (obj instanceof BigDecimal) {
				return ((BigDecimal) obj).intValue();
			}
			return (Integer) obj;
		});
	}

	/**
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the nullable integer from the column of a row.
	 */
	public static RowMapper<Integer> integer(String columnLabel) {
		return r -> r.getInteger(columnLabel);
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
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the byte array from the column of a row.
	 */
	public static RowMapper<byte[]> bytes(String columnLabel) {
		return r -> r.getBytes(columnLabel);
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
	 * Get a function to get the contents of the named column.
	 *
	 * @param <T>
	 *            The type of value expected.
	 * @param columnLabel
	 *            The name of the column.
	 * @param cls
	 *            The type of value expected.
	 * @return A function to get the deserialized object from the column of a
	 *         row.
	 */
	public static <T> RowMapper<T> serial(String columnLabel,
			Class<T> cls) {
		return r -> r.getSerial(columnLabel, cls);
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
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the instant from the column of a row.
	 */
	public static RowMapper<Instant> instant(String columnLabel) {
		return r -> r.getInstant(columnLabel);
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
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the duration from the column of a row.
	 */
	public static RowMapper<Duration> duration(String columnLabel) {
		return r -> r.getDuration(columnLabel);
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
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the object from the column of a row.
	 */
	public static RowMapper<Object> object(String columnLabel) {
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
	public static <T extends Enum<T>> RowMapper<T>
			enumerate(String columnLabel, Class<T> type) {
		return r -> r.getEnum(columnLabel, type);
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

	/**
	 * Get a function to get the contents of the named column.
	 *
	 * @param columnLabel
	 *            The name of the column.
	 * @return A function to get the nullable {@code long} from the column of a
	 *         row.
	 */
	public static RowMapper<Long> int64(String columnLabel) {
		return r -> r.getLong(columnLabel);
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

	/**
	 * Get a chip from a result set row.
	 *
	 * @param x
	 *            The <em>name</em> of the column with the X coordinate.
	 * @param y
	 *            The <em>name</em> of the column with the Y coordinate.
	 * @return The chip location.
	 */
	public ChipLocation getChip(String x, String y) {
		return get(() -> new ChipLocation(rs.getInt(x), rs.getInt(y)));
	}

	/**
	 * Create a function for extracting a chip from a result set row.
	 *
	 * @param x
	 *            The <em>name</em> of the column with the X coordinate.
	 * @param y
	 *            The <em>name</em> of the column with the Y coordinate.
	 * @return The mapping function.
	 */
	public static RowMapper<ChipLocation> chip(String x, String y) {
		return row -> new ChipLocation(row.getInt(x), row.getInt(y));
	}

	/**
	 * Get a core from a result set row.
	 *
	 * @param x
	 *            The <em>name</em> of the column with the X coordinate.
	 * @param y
	 *            The <em>name</em> of the column with the Y coordinate.
	 * @param p
	 *            The <em>name</em> of the column with the core ID.
	 * @return The core location.
	 */
	public CoreLocation getCore(String x, String y, String p) {
		return get(() -> new CoreLocation(rs.getInt(x), rs.getInt(y),
				rs.getInt(p)));
	}

	/**
	 * Create a function for extracting a core from a result set row.
	 *
	 * @param x
	 *            The <em>name</em> of the column with the X coordinate.
	 * @param y
	 *            The <em>name</em> of the column with the Y coordinate.
	 * @param p
	 *            The <em>name</em> of the column with the core ID.
	 * @return The mapping function.
	 */
	public static RowMapper<CoreLocation> core(String x, String y, String p) {
		return row -> new CoreLocation(row.getInt(x), row.getInt(y),
				row.getInt(p));
	}

	/**
	 * Make a mappable iterator out of a list.
	 *
	 * @param <T>
	 *            The type of the list.
	 * @param lst
	 *            The list to convert.
	 * @return A mappable iterator.
	 */
	public static <T> MappableIterable<T> stream(List<T> lst) {
		return () -> lst.iterator();
	}
}
