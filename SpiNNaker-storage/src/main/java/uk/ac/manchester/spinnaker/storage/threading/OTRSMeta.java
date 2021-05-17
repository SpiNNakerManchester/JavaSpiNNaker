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
package uk.ac.manchester.spinnaker.storage.threading;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * A single-threaded database result set metadata wrapper.
 *
 * @author Donal Fellows
 */
final class OTRSMeta extends OTWrapper implements ResultSetMetaData {
	private final ResultSetMetaData m;

	OTRSMeta(OneThread ot, ResultSetMetaData metaData) {
		super(ot, metaData);
		this.m = metaData;
	}

	@Override
	public final int getColumnCount() throws SQLException {
		validateThread();
		return m.getColumnCount();
	}

	@Override
	public final boolean isAutoIncrement(int column) throws SQLException {
		validateThread();
		return m.isAutoIncrement(column);
	}

	@Override
	public final boolean isCaseSensitive(int column) throws SQLException {
		validateThread();
		return m.isCaseSensitive(column);
	}

	@Override
	public final boolean isSearchable(int column) throws SQLException {
		validateThread();
		return m.isSearchable(column);
	}

	@Override
	public final boolean isCurrency(int column) throws SQLException {
		validateThread();
		return m.isCurrency(column);
	}

	@Override
	public final int isNullable(int column) throws SQLException {
		validateThread();
		return m.isNullable(column);
	}

	@Override
	public final boolean isSigned(int column) throws SQLException {
		validateThread();
		return m.isSigned(column);
	}

	@Override
	public final int getColumnDisplaySize(int column) throws SQLException {
		validateThread();
		return m.getColumnDisplaySize(column);
	}

	@Override
	public final String getColumnLabel(int column) throws SQLException {
		validateThread();
		return m.getColumnLabel(column);
	}

	@Override
	public final String getColumnName(int column) throws SQLException {
		validateThread();
		return m.getColumnName(column);
	}

	@Override
	public final String getSchemaName(int column) throws SQLException {
		validateThread();
		return m.getSchemaName(column);
	}

	@Override
	public final int getPrecision(int column) throws SQLException {
		validateThread();
		return m.getPrecision(column);
	}

	@Override
	public final int getScale(int column) throws SQLException {
		validateThread();
		return m.getScale(column);
	}

	@Override
	public final String getTableName(int column) throws SQLException {
		validateThread();
		return m.getTableName(column);
	}

	@Override
	public final String getCatalogName(int column) throws SQLException {
		validateThread();
		return m.getCatalogName(column);
	}

	@Override
	public final int getColumnType(int column) throws SQLException {
		validateThread();
		return m.getColumnType(column);
	}

	@Override
	public final String getColumnTypeName(int column) throws SQLException {
		validateThread();
		return m.getColumnTypeName(column);
	}

	@Override
	public final boolean isReadOnly(int column) throws SQLException {
		validateThread();
		return m.isReadOnly(column);
	}

	@Override
	public final boolean isWritable(int column) throws SQLException {
		validateThread();
		return m.isWritable(column);
	}

	@Override
	public final boolean isDefinitelyWritable(int column) throws SQLException {
		validateThread();
		return m.isDefinitelyWritable(column);
	}

	@Override
	public final String getColumnClassName(int column) throws SQLException {
		validateThread();
		return m.getColumnClassName(column);
	}
}
