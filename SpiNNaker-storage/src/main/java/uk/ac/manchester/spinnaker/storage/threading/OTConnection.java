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
class OTConnection extends OTWrapper implements Connection {
	private final Connection conn;

	OTConnection(OneThread ot, Connection conn) {
		super(ot, conn);
		this.conn = conn;
	}

	@Override
	public final Statement createStatement() throws SQLException {
		validateThread();
		return wrap(conn.createStatement());
	}

	@Override
	public final PreparedStatement prepareStatement(String sql)
			throws SQLException {
		validateThread();
		return wrap(conn.prepareStatement(sql));
	}

	@Override
	public final CallableStatement prepareCall(String sql) throws SQLException {
		return wrap(conn.prepareCall(sql));
	}

	@Override
	public final String nativeSQL(String sql) throws SQLException {
		validateThread();
		return conn.nativeSQL(sql);
	}

	@Override
	public final void setAutoCommit(boolean autoCommit) throws SQLException {
		validateThread();
		conn.setAutoCommit(autoCommit);
	}

	@Override
	public final boolean getAutoCommit() throws SQLException {
		validateThread();
		return conn.getAutoCommit();
	}

	@Override
	public final void commit() throws SQLException {
		validateThread();
		conn.commit();
	}

	@Override
	public final void rollback() throws SQLException {
		validateThread();
		conn.rollback();
	}

	@Override
	public final void close() throws SQLException {
		validateThread();
		conn.close();
	}

	@Override
	public final boolean isClosed() throws SQLException {
		validateThread();
		return conn.isClosed();
	}

	@Override
	public final DatabaseMetaData getMetaData() throws SQLException {
		validateThread();
		return wrap(conn.getMetaData());
	}

	@Override
	public final void setReadOnly(boolean readOnly) throws SQLException {
		validateThread();
		conn.setReadOnly(readOnly);
	}

	@Override
	public final boolean isReadOnly() throws SQLException {
		validateThread();
		return conn.isReadOnly();
	}

	@Override
	public final void setCatalog(String catalog) throws SQLException {
		validateThread();
		conn.setCatalog(catalog);
	}

	@Override
	public final String getCatalog() throws SQLException {
		validateThread();
		return conn.getCatalog();
	}

	@Override
	public final void setTransactionIsolation(int level) throws SQLException {
		validateThread();
		conn.setTransactionIsolation(level);
	}

	@Override
	public final int getTransactionIsolation() throws SQLException {
		validateThread();
		return conn.getTransactionIsolation();
	}

	@Override
	public final SQLWarning getWarnings() throws SQLException {
		validateThread();
		return conn.getWarnings();
	}

	@Override
	public final void clearWarnings() throws SQLException {
		validateThread();
		conn.clearWarnings();
	}

	@Override
	public final Statement createStatement(int resultSetType,
			int resultSetConcurrency) throws SQLException {
		validateThread();
		return wrap(conn.createStatement(resultSetType, resultSetConcurrency));
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			int resultSetType, int resultSetConcurrency) throws SQLException {
		validateThread();
		return wrap(conn.prepareStatement(sql, resultSetType,
				resultSetConcurrency));
	}

	@Override
	public final CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		validateThread();
		return wrap(conn.prepareCall(sql, resultSetType, resultSetConcurrency));
	}

	@Override
	public final Map<String, Class<?>> getTypeMap() throws SQLException {
		validateThread();
		return conn.getTypeMap();
	}

	@Override
	public final void setTypeMap(Map<String, Class<?>> map)
			throws SQLException {
		validateThread();
		conn.setTypeMap(map);
	}

	@Override
	public final void setHoldability(int holdability) throws SQLException {
		validateThread();
		conn.setHoldability(holdability);
	}

	@Override
	public final int getHoldability() throws SQLException {
		validateThread();
		return conn.getHoldability();
	}

	@Override
	public final Savepoint setSavepoint() throws SQLException {
		validateThread();
		return conn.setSavepoint();
	}

	@Override
	public final Savepoint setSavepoint(String name) throws SQLException {
		validateThread();
		return conn.setSavepoint(name);
	}

	@Override
	public final void rollback(Savepoint savepoint) throws SQLException {
		validateThread();
		conn.rollback(savepoint);
	}

	@Override
	public final void releaseSavepoint(Savepoint savepoint)
			throws SQLException {
		validateThread();
		conn.releaseSavepoint(savepoint);
	}

	@Override
	public final Statement createStatement(int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		validateThread();
		return wrap(conn.createStatement(resultSetType, resultSetConcurrency,
				resultSetHoldability));
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		validateThread();
		return wrap(conn.prepareStatement(sql, resultSetType,
				resultSetConcurrency, resultSetHoldability));
	}

	@Override
	public final CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		validateThread();
		return wrap(conn.prepareCall(sql, resultSetType, resultSetConcurrency,
				resultSetHoldability));
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			int autoGeneratedKeys) throws SQLException {
		validateThread();
		return wrap(conn.prepareStatement(sql, autoGeneratedKeys));
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			int[] columnIndexes) throws SQLException {
		validateThread();
		return wrap(conn.prepareStatement(sql, columnIndexes));
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			String[] columnNames) throws SQLException {
		validateThread();
		return wrap(conn.prepareStatement(sql, columnNames));
	}

	@Override
	public final Clob createClob() throws SQLException {
		validateThread();
		return conn.createClob();
	}

	@Override
	public final Blob createBlob() throws SQLException {
		validateThread();
		return conn.createBlob();
	}

	@Override
	public final NClob createNClob() throws SQLException {
		validateThread();
		return conn.createNClob();
	}

	@Override
	public final SQLXML createSQLXML() throws SQLException {
		validateThread();
		return conn.createSQLXML();
	}

	@Override
	public final boolean isValid(int timeout) throws SQLException {
		validateThread();
		return conn.isValid(timeout);
	}

	@Override
	public final void setClientInfo(String name, String value)
			throws SQLClientInfoException {
		validateThread();
		conn.setClientInfo(name, value);
	}

	@Override
	public final void setClientInfo(Properties properties)
			throws SQLClientInfoException {
		validateThread();
		conn.setClientInfo(properties);
	}

	@Override
	public final String getClientInfo(String name) throws SQLException {
		validateThread();
		return conn.getClientInfo(name);
	}

	@Override
	public final Properties getClientInfo() throws SQLException {
		validateThread();
		return conn.getClientInfo();
	}

	@Override
	public final Array createArrayOf(String typeName, Object[] elements)
			throws SQLException {
		validateThread();
		return conn.createArrayOf(typeName, elements);
	}

	@Override
	public final Struct createStruct(String typeName, Object[] attributes)
			throws SQLException {
		validateThread();
		return conn.createStruct(typeName, attributes);
	}

	@Override
	public final void setSchema(String schema) throws SQLException {
		validateThread();
		conn.setSchema(schema);
	}

	@Override
	public final String getSchema() throws SQLException {
		validateThread();
		return conn.getSchema();
	}

	@Override
	public final void abort(Executor executor) throws SQLException {
		validateThread();
		conn.abort(executor);
	}

	@Override
	public final void setNetworkTimeout(Executor executor, int milliseconds)
			throws SQLException {
		validateThread();
		conn.setNetworkTimeout(executor, milliseconds);
	}

	@Override
	public final int getNetworkTimeout() throws SQLException {
		validateThread();
		return conn.getNetworkTimeout();
	}
}
