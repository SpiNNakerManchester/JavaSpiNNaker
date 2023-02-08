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
	public int getColumnCount() throws SQLException {
		validateThread();
		return m.getColumnCount();
	}

	@Override
	public boolean isAutoIncrement(int column) throws SQLException {
		validateThread();
		return m.isAutoIncrement(column);
	}

	@Override
	public boolean isCaseSensitive(int column) throws SQLException {
		validateThread();
		return m.isCaseSensitive(column);
	}

	@Override
	public boolean isSearchable(int column) throws SQLException {
		validateThread();
		return m.isSearchable(column);
	}

	@Override
	public boolean isCurrency(int column) throws SQLException {
		validateThread();
		return m.isCurrency(column);
	}

	@Override
	public int isNullable(int column) throws SQLException {
		validateThread();
		return m.isNullable(column);
	}

	@Override
	public boolean isSigned(int column) throws SQLException {
		validateThread();
		return m.isSigned(column);
	}

	@Override
	public int getColumnDisplaySize(int column) throws SQLException {
		validateThread();
		return m.getColumnDisplaySize(column);
	}

	@Override
	public String getColumnLabel(int column) throws SQLException {
		validateThread();
		return m.getColumnLabel(column);
	}

	@Override
	public String getColumnName(int column) throws SQLException {
		validateThread();
		return m.getColumnName(column);
	}

	@Override
	public String getSchemaName(int column) throws SQLException {
		validateThread();
		return m.getSchemaName(column);
	}

	@Override
	public int getPrecision(int column) throws SQLException {
		validateThread();
		return m.getPrecision(column);
	}

	@Override
	public int getScale(int column) throws SQLException {
		validateThread();
		return m.getScale(column);
	}

	@Override
	public String getTableName(int column) throws SQLException {
		validateThread();
		return m.getTableName(column);
	}

	@Override
	public String getCatalogName(int column) throws SQLException {
		validateThread();
		return m.getCatalogName(column);
	}

	@Override
	public int getColumnType(int column) throws SQLException {
		validateThread();
		return m.getColumnType(column);
	}

	@Override
	public String getColumnTypeName(int column) throws SQLException {
		validateThread();
		return m.getColumnTypeName(column);
	}

	@Override
	public boolean isReadOnly(int column) throws SQLException {
		validateThread();
		return m.isReadOnly(column);
	}

	@Override
	public boolean isWritable(int column) throws SQLException {
		validateThread();
		return m.isWritable(column);
	}

	@Override
	public boolean isDefinitelyWritable(int column) throws SQLException {
		validateThread();
		return m.isDefinitelyWritable(column);
	}

	@Override
	public String getColumnClassName(int column) throws SQLException {
		validateThread();
		return m.getColumnClassName(column);
	}
}
