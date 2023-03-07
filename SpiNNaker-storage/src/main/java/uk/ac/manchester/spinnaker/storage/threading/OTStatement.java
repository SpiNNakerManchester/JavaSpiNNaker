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
package uk.ac.manchester.spinnaker.storage.threading;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

/**
 * A single-threaded database statement wrapper.
 *
 * @author Donal Fellows
 */
class OTStatement extends OTWrapper implements Statement {
	private final Statement s;

	OTStatement(OneThread ot, Statement s) {
		super(ot, s);
		this.s = s;
	}

	@Override
	public final ResultSet executeQuery(String sql) throws SQLException {
		validateThread();
		return wrap(this, s.executeQuery(sql));
	}

	@Override
	public final int executeUpdate(String sql) throws SQLException {
		validateThread();
		return s.executeUpdate(sql);
	}

	@Override
	public final void close() throws SQLException {
		validateThread();
		s.close();
	}

	@Override
	public final int getMaxFieldSize() throws SQLException {
		validateThread();
		return s.getMaxFieldSize();
	}

	@Override
	public final void setMaxFieldSize(int max) throws SQLException {
		validateThread();
		s.setMaxFieldSize(max);
	}

	@Override
	public final int getMaxRows() throws SQLException {
		validateThread();
		return s.getMaxRows();
	}

	@Override
	public final void setMaxRows(int max) throws SQLException {
		validateThread();
		s.setMaxRows(max);
	}

	@Override
	public final void setEscapeProcessing(boolean enable) throws SQLException {
		validateThread();
		s.setEscapeProcessing(enable);
	}

	@Override
	public final int getQueryTimeout() throws SQLException {
		validateThread();
		return s.getQueryTimeout();
	}

	@Override
	public final void setQueryTimeout(int seconds) throws SQLException {
		validateThread();
		s.setQueryTimeout(seconds);
	}

	@Override
	public final void cancel() throws SQLException {
		validateThread();
		s.cancel();
	}

	@Override
	public final SQLWarning getWarnings() throws SQLException {
		validateThread();
		return s.getWarnings();
	}

	@Override
	public final void clearWarnings() throws SQLException {
		validateThread();
		s.clearWarnings();
	}

	@Override
	public final void setCursorName(String name) throws SQLException {
		validateThread();
		s.setCursorName(name);
	}

	@Override
	public final boolean execute(String sql) throws SQLException {
		validateThread();
		return s.execute(sql);
	}

	@Override
	public final ResultSet getResultSet() throws SQLException {
		validateThread();
		return wrap(this, s.getResultSet());
	}

	@Override
	public final int getUpdateCount() throws SQLException {
		validateThread();
		return s.getUpdateCount();
	}

	@Override
	public final boolean getMoreResults() throws SQLException {
		validateThread();
		return s.getMoreResults();
	}

	@Override
	public final void setFetchDirection(int direction) throws SQLException {
		validateThread();
		s.setFetchDirection(direction);
	}

	@Override
	public final int getFetchDirection() throws SQLException {
		validateThread();
		return s.getFetchDirection();
	}

	@Override
	public final void setFetchSize(int rows) throws SQLException {
		validateThread();
		s.setFetchSize(rows);
	}

	@Override
	public final int getFetchSize() throws SQLException {
		validateThread();
		return s.getFetchSize();
	}

	@Override
	public final int getResultSetConcurrency() throws SQLException {
		validateThread();
		return s.getResultSetConcurrency();
	}

	@Override
	public final int getResultSetType() throws SQLException {
		validateThread();
		return s.getResultSetType();
	}

	@Override
	public final void addBatch(String sql) throws SQLException {
		s.addBatch(sql);
	}

	@Override
	public final void clearBatch() throws SQLException {
		validateThread();
		s.clearBatch();
	}

	@Override
	public final int[] executeBatch() throws SQLException {
		validateThread();
		return s.executeBatch();
	}

	@Override
	public final Connection getConnection() throws SQLException {
		return wrap(s.getConnection());
	}

	@Override
	public final boolean getMoreResults(int current) throws SQLException {
		validateThread();
		return s.getMoreResults(current);
	}

	@Override
	public final ResultSet getGeneratedKeys() throws SQLException {
		validateThread();
		return wrap(this, s.getGeneratedKeys());
	}

	@Override
	public final int executeUpdate(String sql, int autoGeneratedKeys)
			throws SQLException {
		validateThread();
		return s.executeUpdate(sql, autoGeneratedKeys);
	}

	@Override
	public final int executeUpdate(String sql, int[] columnIndexes)
			throws SQLException {
		validateThread();
		return s.executeUpdate(sql, columnIndexes);
	}

	@Override
	public final int executeUpdate(String sql, String[] columnNames)
			throws SQLException {
		validateThread();
		return s.executeUpdate(sql, columnNames);
	}

	@Override
	public final boolean execute(String sql, int autoGeneratedKeys)
			throws SQLException {
		validateThread();
		return s.execute(sql, autoGeneratedKeys);
	}

	@Override
	public final boolean execute(String sql, int[] columnIndexes)
			throws SQLException {
		validateThread();
		return s.execute(sql, columnIndexes);
	}

	@Override
	public final boolean execute(String sql, String[] columnNames)
			throws SQLException {
		validateThread();
		return s.execute(sql, columnNames);
	}

	@Override
	public final int getResultSetHoldability() throws SQLException {
		validateThread();
		return s.getResultSetHoldability();
	}

	@Override
	public final boolean isClosed() throws SQLException {
		validateThread();
		return s.isClosed();
	}

	@Override
	public final void setPoolable(boolean poolable) throws SQLException {
		validateThread();
		s.setPoolable(poolable);
	}

	@Override
	public final boolean isPoolable() throws SQLException {
		validateThread();
		return s.isPoolable();
	}

	@Override
	public final void closeOnCompletion() throws SQLException {
		validateThread();
		s.closeOnCompletion();
	}

	@Override
	public final boolean isCloseOnCompletion() throws SQLException {
		validateThread();
		return s.isCloseOnCompletion();
	}
}
