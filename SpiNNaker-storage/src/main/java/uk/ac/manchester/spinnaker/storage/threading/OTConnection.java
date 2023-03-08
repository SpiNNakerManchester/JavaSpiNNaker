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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * A single-threaded database connection wrapper.
 *
 * @author Donal Fellows
 */
final class OTConnection extends OTWrapper implements Connection {
	private final Connection conn;

	/**
	 * Whether the {@link #close()} method is enabled. If disabled, it is a
	 * no-op.
	 */
	final boolean closeable;

	OTConnection(OneThread ot, Connection conn, boolean closeable) {
		super(ot, conn);
		this.conn = conn;
		this.closeable = closeable;
	}

	@Override
	public Statement createStatement() throws SQLException {
		validateThread();
		return wrap(conn.createStatement());
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		validateThread();
		return wrap(conn.prepareStatement(sql));
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		return wrap(conn.prepareCall(sql));
	}

	@Override
	public String nativeSQL(String sql) throws SQLException {
		validateThread();
		return conn.nativeSQL(sql);
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		validateThread();
		conn.setAutoCommit(autoCommit);
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		validateThread();
		return conn.getAutoCommit();
	}

	@Override
	public void commit() throws SQLException {
		validateThread();
		conn.commit();
	}

	@Override
	public void rollback() throws SQLException {
		validateThread();
		conn.rollback();
	}

	@Override
	public void close() throws SQLException {
		validateThread();
		if (closeable) {
			conn.close();
		}
	}

	@Override
	public boolean isClosed() throws SQLException {
		validateThread();
		return conn.isClosed();
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		validateThread();
		return wrap(conn.getMetaData());
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		validateThread();
		conn.setReadOnly(readOnly);
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		validateThread();
		return conn.isReadOnly();
	}

	@Override
	public void setCatalog(String catalog) throws SQLException {
		validateThread();
		conn.setCatalog(catalog);
	}

	@Override
	public String getCatalog() throws SQLException {
		validateThread();
		return conn.getCatalog();
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		validateThread();
		conn.setTransactionIsolation(level);
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		validateThread();
		return conn.getTransactionIsolation();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		validateThread();
		return conn.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException {
		validateThread();
		conn.clearWarnings();
	}

	@Override
	public Statement createStatement(int resultSetType,
			int resultSetConcurrency) throws SQLException {
		validateThread();
		return wrap(conn.createStatement(resultSetType, resultSetConcurrency));
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		validateThread();
		return wrap(conn.prepareStatement(sql, resultSetType,
				resultSetConcurrency));
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		validateThread();
		return wrap(conn.prepareCall(sql, resultSetType, resultSetConcurrency));
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		validateThread();
		return conn.getTypeMap();
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		validateThread();
		conn.setTypeMap(map);
	}

	@Override
	public void setHoldability(int holdability) throws SQLException {
		validateThread();
		conn.setHoldability(holdability);
	}

	@Override
	public int getHoldability() throws SQLException {
		validateThread();
		return conn.getHoldability();
	}

	@Override
	public Savepoint setSavepoint() throws SQLException {
		validateThread();
		return conn.setSavepoint();
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		validateThread();
		return conn.setSavepoint(name);
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		validateThread();
		conn.rollback(savepoint);
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		validateThread();
		conn.releaseSavepoint(savepoint);
	}

	@Override
	public Statement createStatement(int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		validateThread();
		return wrap(conn.createStatement(resultSetType, resultSetConcurrency,
				resultSetHoldability));
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		validateThread();
		return wrap(conn.prepareStatement(sql, resultSetType,
				resultSetConcurrency, resultSetHoldability));
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		validateThread();
		return wrap(conn.prepareCall(sql, resultSetType, resultSetConcurrency,
				resultSetHoldability));
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
			throws SQLException {
		validateThread();
		return wrap(conn.prepareStatement(sql, autoGeneratedKeys));
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
			throws SQLException {
		validateThread();
		return wrap(conn.prepareStatement(sql, columnIndexes));
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames)
			throws SQLException {
		validateThread();
		return wrap(conn.prepareStatement(sql, columnNames));
	}

	@Override
	public Clob createClob() throws SQLException {
		validateThread();
		return conn.createClob();
	}

	@Override
	public Blob createBlob() throws SQLException {
		validateThread();
		return conn.createBlob();
	}

	@Override
	public NClob createNClob() throws SQLException {
		validateThread();
		return conn.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		validateThread();
		return conn.createSQLXML();
	}

	@Override
	public boolean isValid(int timeout) throws SQLException {
		validateThread();
		return conn.isValid(timeout);
	}

	@Override
	public void setClientInfo(String name, String value)
			throws SQLClientInfoException {
		validateThread();
		conn.setClientInfo(name, value);
	}

	@Override
	public void setClientInfo(Properties properties)
			throws SQLClientInfoException {
		validateThread();
		conn.setClientInfo(properties);
	}

	@Override
	public String getClientInfo(String name) throws SQLException {
		validateThread();
		return conn.getClientInfo(name);
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		validateThread();
		return conn.getClientInfo();
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements)
			throws SQLException {
		validateThread();
		return conn.createArrayOf(typeName, elements);
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes)
			throws SQLException {
		validateThread();
		return conn.createStruct(typeName, attributes);
	}

	@Override
	public void setSchema(String schema) throws SQLException {
		validateThread();
		conn.setSchema(schema);
	}

	@Override
	public String getSchema() throws SQLException {
		validateThread();
		return conn.getSchema();
	}

	@Override
	public void abort(Executor executor) throws SQLException {
		validateThread();
		conn.abort(executor);
	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds)
			throws SQLException {
		validateThread();
		conn.setNetworkTimeout(executor, milliseconds);
	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		validateThread();
		return conn.getNetworkTimeout();
	}
}
