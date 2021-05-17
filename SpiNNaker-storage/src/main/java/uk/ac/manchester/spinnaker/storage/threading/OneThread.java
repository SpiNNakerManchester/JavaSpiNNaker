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

import static java.lang.Thread.currentThread;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.RowIdLifetime;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Wrapper;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * A wrapper for an SQL connection that enforces that it is only used from its
 * creating thread.
 *
 * @author Donal Fellows
 */
public class OneThread {
	private final Thread thread;

	/**
	 * Create the wrapper.
	 *
	 * @param conn
	 *            The connection that is being protected.
	 */
	public static Connection threadBound(Connection conn) {
		OneThread ot = new OneThread();
		return ot.new OTConnection(conn);
	}

	private OneThread() {
		this.thread = currentThread();
	}

	private void validateThread() {
		if (currentThread() != thread) {
			throw new IllegalStateException(
					"use of database connection outside its owner thread");
		}
	}

	private class OTConnection extends OTWrapper implements Connection {
		private final Connection conn;

		private OTConnection(Connection conn) {
			super(conn);
			this.conn = conn;
		}

		@Override
		public Statement createStatement() throws SQLException {
			validateThread();
			return new OTStatement(conn.createStatement());
		}

		@Override
		public PreparedStatement prepareStatement(String sql)
				throws SQLException {
			validateThread();
			return new OTPrepared(conn.prepareStatement(sql));
		}

		@Override
		public CallableStatement prepareCall(String sql) throws SQLException {
			return new OTCallable(conn.prepareCall(sql));
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
			conn.close();
		}

		@Override
		public boolean isClosed() throws SQLException {
			validateThread();
			return conn.isClosed();
		}

		@Override
		public DatabaseMetaData getMetaData() throws SQLException {
			validateThread();
			return new OTMeta(conn.getMetaData());
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
			return new OTStatement(
					conn.createStatement(resultSetType, resultSetConcurrency));
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType,
				int resultSetConcurrency) throws SQLException {
			validateThread();
			return new OTPrepared(conn.prepareStatement(sql, resultSetType,
					resultSetConcurrency));
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType,
				int resultSetConcurrency) throws SQLException {
			validateThread();
			return new OTCallable(
					conn.prepareCall(sql, resultSetType, resultSetConcurrency));
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
			return new OTStatement(conn.createStatement(resultSetType,
					resultSetConcurrency, resultSetHoldability));
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType,
				int resultSetConcurrency, int resultSetHoldability)
				throws SQLException {
			validateThread();
			return new OTPrepared(conn.prepareStatement(sql, resultSetType,
					resultSetConcurrency, resultSetHoldability));
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType,
				int resultSetConcurrency, int resultSetHoldability)
				throws SQLException {
			validateThread();
			return new OTCallable(conn.prepareCall(sql, resultSetType,
					resultSetConcurrency, resultSetHoldability));
		}

		@Override
		public PreparedStatement prepareStatement(String sql,
				int autoGeneratedKeys) throws SQLException {
			validateThread();
			return new OTPrepared(
					conn.prepareStatement(sql, autoGeneratedKeys));
		}

		@Override
		public PreparedStatement prepareStatement(String sql,
				int[] columnIndexes) throws SQLException {
			validateThread();
			return new OTPrepared(conn.prepareStatement(sql, columnIndexes));
		}

		@Override
		public PreparedStatement prepareStatement(String sql,
				String[] columnNames) throws SQLException {
			validateThread();
			return new OTPrepared(conn.prepareStatement(sql, columnNames));
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

	private class OTWrapper implements Wrapper {
		private final Wrapper w;

		OTWrapper(Wrapper w) {
			this.w = w;
		}

		@Override
		public final <T> T unwrap(Class<T> iface) throws SQLException {
			validateThread();
			return w.unwrap(iface);
		}

		@Override
		public final boolean isWrapperFor(Class<?> iface) throws SQLException {
			validateThread();
			return w.isWrapperFor(iface);
		}
	}

	private class OTStatement extends OTWrapper implements Statement {
		private final Statement s;

		OTStatement(Statement s) {
			super(s);
			this.s = s;
		}

		@Override
		public ResultSet executeQuery(String sql) throws SQLException {
			validateThread();
			return new OTResults(this, s.executeQuery(sql));
		}

		@Override
		public int executeUpdate(String sql) throws SQLException {
			validateThread();
			return s.executeUpdate(sql);
		}

		@Override
		public void close() throws SQLException {
			validateThread();
			s.close();
		}

		@Override
		public int getMaxFieldSize() throws SQLException {
			validateThread();
			return s.getMaxFieldSize();
		}

		@Override
		public void setMaxFieldSize(int max) throws SQLException {
			validateThread();
			s.setMaxFieldSize(max);
		}

		@Override
		public int getMaxRows() throws SQLException {
			validateThread();
			return s.getMaxRows();
		}

		@Override
		public void setMaxRows(int max) throws SQLException {
			validateThread();
			s.setMaxRows(max);
		}

		@Override
		public void setEscapeProcessing(boolean enable) throws SQLException {
			validateThread();
			s.setEscapeProcessing(enable);
		}

		@Override
		public int getQueryTimeout() throws SQLException {
			validateThread();
			return s.getQueryTimeout();
		}

		@Override
		public void setQueryTimeout(int seconds) throws SQLException {
			validateThread();
			s.setQueryTimeout(seconds);
		}

		@Override
		public void cancel() throws SQLException {
			validateThread();
			s.cancel();
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			validateThread();
			return s.getWarnings();
		}

		@Override
		public void clearWarnings() throws SQLException {
			validateThread();
			s.clearWarnings();
		}

		@Override
		public void setCursorName(String name) throws SQLException {
			validateThread();
			s.setCursorName(name);
		}

		@Override
		public boolean execute(String sql) throws SQLException {
			validateThread();
			return s.execute(sql);
		}

		@Override
		public ResultSet getResultSet() throws SQLException {
			validateThread();
			return new OTResults(this, s.getResultSet());
		}

		@Override
		public int getUpdateCount() throws SQLException {
			validateThread();
			return s.getUpdateCount();
		}

		@Override
		public boolean getMoreResults() throws SQLException {
			validateThread();
			return s.getMoreResults();
		}

		@Override
		public void setFetchDirection(int direction) throws SQLException {
			validateThread();
			s.setFetchDirection(direction);
		}

		@Override
		public int getFetchDirection() throws SQLException {
			validateThread();
			return s.getFetchDirection();
		}

		@Override
		public void setFetchSize(int rows) throws SQLException {
			validateThread();
			s.setFetchSize(rows);
		}

		@Override
		public int getFetchSize() throws SQLException {
			validateThread();
			return s.getFetchSize();
		}

		@Override
		public int getResultSetConcurrency() throws SQLException {
			validateThread();
			return s.getResultSetConcurrency();
		}

		@Override
		public int getResultSetType() throws SQLException {
			validateThread();
			return s.getResultSetType();
		}

		@Override
		public void addBatch(String sql) throws SQLException {
			s.addBatch(sql);
		}

		@Override
		public void clearBatch() throws SQLException {
			validateThread();
			s.clearBatch();
		}

		@Override
		public int[] executeBatch() throws SQLException {
			validateThread();
			return s.executeBatch();
		}

		@Override
		public Connection getConnection() throws SQLException {
			return new OTConnection(s.getConnection());
		}

		@Override
		public boolean getMoreResults(int current) throws SQLException {
			validateThread();
			return s.getMoreResults(current);
		}

		@Override
		public ResultSet getGeneratedKeys() throws SQLException {
			validateThread();
			return new OTResults(this, s.getGeneratedKeys());
		}

		@Override
		public int executeUpdate(String sql, int autoGeneratedKeys)
				throws SQLException {
			validateThread();
			return s.executeUpdate(sql, autoGeneratedKeys);
		}

		@Override
		public int executeUpdate(String sql, int[] columnIndexes)
				throws SQLException {
			validateThread();
			return s.executeUpdate(sql, columnIndexes);
		}

		@Override
		public int executeUpdate(String sql, String[] columnNames)
				throws SQLException {
			validateThread();
			return s.executeUpdate(sql, columnNames);
		}

		@Override
		public boolean execute(String sql, int autoGeneratedKeys)
				throws SQLException {
			validateThread();
			return s.execute(sql, autoGeneratedKeys);
		}

		@Override
		public boolean execute(String sql, int[] columnIndexes)
				throws SQLException {
			validateThread();
			return s.execute(sql, columnIndexes);
		}

		@Override
		public boolean execute(String sql, String[] columnNames)
				throws SQLException {
			validateThread();
			return s.execute(sql, columnNames);
		}

		@Override
		public int getResultSetHoldability() throws SQLException {
			validateThread();
			return s.getResultSetHoldability();
		}

		@Override
		public boolean isClosed() throws SQLException {
			validateThread();
			return s.isClosed();
		}

		@Override
		public void setPoolable(boolean poolable) throws SQLException {
			validateThread();
			s.setPoolable(poolable);
		}

		@Override
		public boolean isPoolable() throws SQLException {
			validateThread();
			return s.isPoolable();
		}

		@Override
		public void closeOnCompletion() throws SQLException {
			validateThread();
			s.closeOnCompletion();
		}

		@Override
		public boolean isCloseOnCompletion() throws SQLException {
			validateThread();
			return s.isCloseOnCompletion();
		}
	}

	private class OTPrepared extends OTStatement implements PreparedStatement {
		private final PreparedStatement s;

		OTPrepared(PreparedStatement s) {
			super(s);
			this.s = s;
		}

		@Override
		public ResultSet executeQuery() throws SQLException {
			validateThread();
			return new OTResults(this, s.executeQuery());
		}

		@Override
		public int executeUpdate() throws SQLException {
			validateThread();
			return s.executeUpdate();
		}

		@Override
		public void setNull(int parameterIndex, int sqlType)
				throws SQLException {
			validateThread();
			s.setNull(parameterIndex, sqlType);
		}

		@Override
		public void setBoolean(int parameterIndex, boolean x)
				throws SQLException {
			validateThread();
			s.setBoolean(parameterIndex, x);
		}

		@Override
		public void setByte(int parameterIndex, byte x) throws SQLException {
			validateThread();
			s.setByte(parameterIndex, x);
		}

		@Override
		public void setShort(int parameterIndex, short x) throws SQLException {
			validateThread();
			s.setShort(parameterIndex, x);
		}

		@Override
		public void setInt(int parameterIndex, int x) throws SQLException {
			validateThread();
			s.setInt(parameterIndex, x);
		}

		@Override
		public void setLong(int parameterIndex, long x) throws SQLException {
			validateThread();
			s.setLong(parameterIndex, x);
		}

		@Override
		public void setFloat(int parameterIndex, float x) throws SQLException {
			validateThread();
			s.setFloat(parameterIndex, x);
		}

		@Override
		public void setDouble(int parameterIndex, double x)
				throws SQLException {
			validateThread();
			s.setDouble(parameterIndex, x);
		}

		@Override
		public void setBigDecimal(int parameterIndex, BigDecimal x)
				throws SQLException {
			validateThread();
			s.setBigDecimal(parameterIndex, x);
		}

		@Override
		public void setString(int parameterIndex, String x)
				throws SQLException {
			validateThread();
			s.setString(parameterIndex, x);
		}

		@Override
		public void setBytes(int parameterIndex, byte[] x) throws SQLException {
			validateThread();
			s.setBytes(parameterIndex, x);
		}

		@Override
		public void setDate(int parameterIndex, Date x) throws SQLException {
			validateThread();
			s.setDate(parameterIndex, x);
		}

		@Override
		public void setTime(int parameterIndex, Time x) throws SQLException {
			validateThread();
			s.setTime(parameterIndex, x);
		}

		@Override
		public void setTimestamp(int parameterIndex, Timestamp x)
				throws SQLException {
			validateThread();
			s.setTimestamp(parameterIndex, x);
		}

		@Override
		public void setAsciiStream(int parameterIndex, InputStream x,
				int length) throws SQLException {
			validateThread();
			s.setAsciiStream(parameterIndex, x, length);
		}

		@Override
		@Deprecated
		public void setUnicodeStream(int parameterIndex, InputStream x,
				int length) throws SQLException {
			validateThread();
			s.setUnicodeStream(parameterIndex, x, length);
		}

		@Override
		public void setBinaryStream(int parameterIndex, InputStream x,
				int length) throws SQLException {
			validateThread();
			s.setBinaryStream(parameterIndex, x, length);
		}

		@Override
		public void clearParameters() throws SQLException {
			validateThread();
			s.clearParameters();
		}

		@Override
		public void setObject(int parameterIndex, Object x, int targetSqlType)
				throws SQLException {
			validateThread();
			s.setObject(parameterIndex, x, targetSqlType);
		}

		@Override
		public void setObject(int parameterIndex, Object x)
				throws SQLException {
			validateThread();
			s.setObject(parameterIndex, x);
		}

		@Override
		public boolean execute() throws SQLException {
			validateThread();
			return s.execute();
		}

		@Override
		public void addBatch() throws SQLException {
			validateThread();
			s.addBatch();
		}

		@Override
		public void setCharacterStream(int parameterIndex, Reader reader,
				int length) throws SQLException {
			validateThread();
			s.setCharacterStream(parameterIndex, reader, length);
		}

		@Override
		public void setRef(int parameterIndex, Ref x) throws SQLException {
			validateThread();
			s.setRef(parameterIndex, x);
		}

		@Override
		public void setBlob(int parameterIndex, Blob x) throws SQLException {
			validateThread();
			s.setBlob(parameterIndex, x);
		}

		@Override
		public void setClob(int parameterIndex, Clob x) throws SQLException {
			validateThread();
			s.setClob(parameterIndex, x);
		}

		@Override
		public void setArray(int parameterIndex, Array x) throws SQLException {
			validateThread();
			s.setArray(parameterIndex, x);
		}

		@Override
		public ResultSetMetaData getMetaData() throws SQLException {
			validateThread();
			return s.getMetaData();
		}

		@Override
		public void setDate(int parameterIndex, Date x, Calendar cal)
				throws SQLException {
			validateThread();
			s.setDate(parameterIndex, x, cal);
		}

		@Override
		public void setTime(int parameterIndex, Time x, Calendar cal)
				throws SQLException {
			validateThread();
			s.setTime(parameterIndex, x, cal);
		}

		@Override
		public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
				throws SQLException {
			validateThread();
			s.setTimestamp(parameterIndex, x, cal);
		}

		@Override
		public void setNull(int parameterIndex, int sqlType, String typeName)
				throws SQLException {
			validateThread();
			s.setNull(parameterIndex, sqlType, typeName);
		}

		@Override
		public void setURL(int parameterIndex, URL x) throws SQLException {
			validateThread();
			s.setURL(parameterIndex, x);
		}

		@Override
		public ParameterMetaData getParameterMetaData() throws SQLException {
			validateThread();
			return s.getParameterMetaData();
		}

		@Override
		public void setRowId(int parameterIndex, RowId x) throws SQLException {
			validateThread();
			s.setRowId(parameterIndex, x);
		}

		@Override
		public void setNString(int parameterIndex, String value)
				throws SQLException {
			validateThread();
			s.setNString(parameterIndex, value);
		}

		@Override
		public void setNCharacterStream(int parameterIndex, Reader value,
				long length) throws SQLException {
			validateThread();
			s.setNCharacterStream(parameterIndex, value, length);
		}

		@Override
		public void setNClob(int parameterIndex, NClob value)
				throws SQLException {
			validateThread();
			s.setNClob(parameterIndex, value);
		}

		@Override
		public void setClob(int parameterIndex, Reader reader, long length)
				throws SQLException {
			validateThread();
			s.setClob(parameterIndex, reader, length);
		}

		@Override
		public void setBlob(int parameterIndex, InputStream inputStream,
				long length) throws SQLException {
			validateThread();
			s.setBlob(parameterIndex, inputStream, length);
		}

		@Override
		public void setNClob(int parameterIndex, Reader reader, long length)
				throws SQLException {
			validateThread();
			s.setNClob(parameterIndex, reader, length);
		}

		@Override
		public void setSQLXML(int parameterIndex, SQLXML xmlObject)
				throws SQLException {
			validateThread();
			s.setSQLXML(parameterIndex, xmlObject);
		}

		@Override
		public void setObject(int parameterIndex, Object x, int targetSqlType,
				int scaleOrLength) throws SQLException {
			validateThread();
			s.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
		}

		@Override
		public void setAsciiStream(int parameterIndex, InputStream x,
				long length) throws SQLException {
			validateThread();
			s.setAsciiStream(parameterIndex, x, length);
		}

		@Override
		public void setBinaryStream(int parameterIndex, InputStream x,
				long length) throws SQLException {
			validateThread();
			s.setBinaryStream(parameterIndex, x, length);
		}

		@Override
		public void setCharacterStream(int parameterIndex, Reader reader,
				long length) throws SQLException {
			validateThread();
			s.setCharacterStream(parameterIndex, reader, length);
		}

		@Override
		public void setAsciiStream(int parameterIndex, InputStream x)
				throws SQLException {
			validateThread();
			s.setAsciiStream(parameterIndex, x);
		}

		@Override
		public void setBinaryStream(int parameterIndex, InputStream x)
				throws SQLException {
			validateThread();
			s.setBinaryStream(parameterIndex, x);
		}

		@Override
		public void setCharacterStream(int parameterIndex, Reader reader)
				throws SQLException {
			validateThread();
			s.setCharacterStream(parameterIndex, reader);
		}

		@Override
		public void setNCharacterStream(int parameterIndex, Reader value)
				throws SQLException {
			validateThread();
			s.setNCharacterStream(parameterIndex, value);
		}

		@Override
		public void setClob(int parameterIndex, Reader reader)
				throws SQLException {
			validateThread();
			s.setClob(parameterIndex, reader);
		}

		@Override
		public void setBlob(int parameterIndex, InputStream inputStream)
				throws SQLException {
			validateThread();
			s.setBlob(parameterIndex, inputStream);
		}

		@Override
		public void setNClob(int parameterIndex, Reader reader)
				throws SQLException {
			validateThread();
			s.setNClob(parameterIndex, reader);
		}
	}

	private class OTCallable extends OTPrepared implements CallableStatement {
		private final CallableStatement s;

		OTCallable(CallableStatement s) {
			super(s);
			this.s = s;
		}

		@Override
		public void registerOutParameter(int parameterIndex, int sqlType)
				throws SQLException {
			validateThread();
			s.registerOutParameter(parameterIndex, sqlType);
		}

		@Override
		public void registerOutParameter(int parameterIndex, int sqlType,
				int scale) throws SQLException {
			validateThread();
			s.registerOutParameter(parameterIndex, sqlType, scale);
		}

		@Override
		public boolean wasNull() throws SQLException {
			validateThread();
			return s.wasNull();
		}

		@Override
		public String getString(int parameterIndex) throws SQLException {
			validateThread();
			return s.getString(parameterIndex);
		}

		@Override
		public boolean getBoolean(int parameterIndex) throws SQLException {
			validateThread();
			return s.getBoolean(parameterIndex);
		}

		@Override
		public byte getByte(int parameterIndex) throws SQLException {
			validateThread();
			return s.getByte(parameterIndex);
		}

		@Override
		public short getShort(int parameterIndex) throws SQLException {
			validateThread();
			return s.getShort(parameterIndex);
		}

		@Override
		public int getInt(int parameterIndex) throws SQLException {
			validateThread();
			return s.getInt(parameterIndex);
		}

		@Override
		public long getLong(int parameterIndex) throws SQLException {
			validateThread();
			return s.getLong(parameterIndex);
		}

		@Override
		public float getFloat(int parameterIndex) throws SQLException {
			validateThread();
			return s.getFloat(parameterIndex);
		}

		@Override
		public double getDouble(int parameterIndex) throws SQLException {
			validateThread();
			return s.getDouble(parameterIndex);
		}

		@Override
		@Deprecated
		public BigDecimal getBigDecimal(int parameterIndex, int scale)
				throws SQLException {
			validateThread();
			return s.getBigDecimal(parameterIndex, scale);
		}

		@Override
		public byte[] getBytes(int parameterIndex) throws SQLException {
			validateThread();
			return s.getBytes(parameterIndex);
		}

		@Override
		public Date getDate(int parameterIndex) throws SQLException {
			validateThread();
			return s.getDate(parameterIndex);
		}

		@Override
		public Time getTime(int parameterIndex) throws SQLException {
			validateThread();
			return s.getTime(parameterIndex);
		}

		@Override
		public Timestamp getTimestamp(int parameterIndex) throws SQLException {
			validateThread();
			return s.getTimestamp(parameterIndex);
		}

		@Override
		public Object getObject(int parameterIndex) throws SQLException {
			validateThread();
			return s.getObject(parameterIndex);
		}

		@Override
		public BigDecimal getBigDecimal(int parameterIndex)
				throws SQLException {
			validateThread();
			return s.getBigDecimal(parameterIndex);
		}

		@Override
		public Object getObject(int parameterIndex, Map<String, Class<?>> map)
				throws SQLException {
			validateThread();
			return s.getObject(parameterIndex, map);
		}

		@Override
		public Ref getRef(int parameterIndex) throws SQLException {
			validateThread();
			return s.getRef(parameterIndex);
		}

		@Override
		public Blob getBlob(int parameterIndex) throws SQLException {
			validateThread();
			return s.getBlob(parameterIndex);
		}

		@Override
		public Clob getClob(int parameterIndex) throws SQLException {
			validateThread();
			return s.getClob(parameterIndex);
		}

		@Override
		public Array getArray(int parameterIndex) throws SQLException {
			validateThread();
			return s.getArray(parameterIndex);
		}

		@Override
		public Date getDate(int parameterIndex, Calendar cal)
				throws SQLException {
			validateThread();
			return s.getDate(parameterIndex, cal);
		}

		@Override
		public Time getTime(int parameterIndex, Calendar cal)
				throws SQLException {
			validateThread();
			return s.getTime(parameterIndex, cal);
		}

		@Override
		public Timestamp getTimestamp(int parameterIndex, Calendar cal)
				throws SQLException {
			validateThread();
			return s.getTimestamp(parameterIndex, cal);
		}

		@Override
		public void registerOutParameter(int parameterIndex, int sqlType,
				String typeName) throws SQLException {
			validateThread();
			s.registerOutParameter(parameterIndex, sqlType, typeName);
		}

		@Override
		public void registerOutParameter(String parameterName, int sqlType)
				throws SQLException {
			validateThread();
			s.registerOutParameter(parameterName, sqlType);
		}

		@Override
		public void registerOutParameter(String parameterName, int sqlType,
				int scale) throws SQLException {
			validateThread();
			s.registerOutParameter(parameterName, sqlType, scale);
		}

		@Override
		public void registerOutParameter(String parameterName, int sqlType,
				String typeName) throws SQLException {
			validateThread();
			s.registerOutParameter(parameterName, sqlType, typeName);
		}

		@Override
		public URL getURL(int parameterIndex) throws SQLException {
			validateThread();
			return s.getURL(parameterIndex);
		}

		@Override
		public void setURL(String parameterName, URL val) throws SQLException {
			validateThread();
			s.setURL(parameterName, val);
		}

		@Override
		public void setNull(String parameterName, int sqlType)
				throws SQLException {
			validateThread();
			s.setNull(parameterName, sqlType);
		}

		@Override
		public void setBoolean(String parameterName, boolean x)
				throws SQLException {
			validateThread();
			s.setBoolean(parameterName, x);
		}

		@Override
		public void setByte(String parameterName, byte x) throws SQLException {
			validateThread();
			s.setByte(parameterName, x);
		}

		@Override
		public void setShort(String parameterName, short x)
				throws SQLException {
			validateThread();
			s.setShort(parameterName, x);
		}

		@Override
		public void setInt(String parameterName, int x) throws SQLException {
			validateThread();
			s.setInt(parameterName, x);
		}

		@Override
		public void setLong(String parameterName, long x) throws SQLException {
			validateThread();
			s.setLong(parameterName, x);
		}

		@Override
		public void setFloat(String parameterName, float x)
				throws SQLException {
			validateThread();
			s.setFloat(parameterName, x);
		}

		@Override
		public void setDouble(String parameterName, double x)
				throws SQLException {
			validateThread();
			s.setDouble(parameterName, x);
		}

		@Override
		public void setBigDecimal(String parameterName, BigDecimal x)
				throws SQLException {
			validateThread();
			s.setBigDecimal(parameterName, x);
		}

		@Override
		public void setString(String parameterName, String x)
				throws SQLException {
			validateThread();
			s.setString(parameterName, x);
		}

		@Override
		public void setBytes(String parameterName, byte[] x)
				throws SQLException {
			validateThread();
			s.setBytes(parameterName, x);
		}

		@Override
		public void setDate(String parameterName, Date x) throws SQLException {
			validateThread();
			s.setDate(parameterName, x);
		}

		@Override
		public void setTime(String parameterName, Time x) throws SQLException {
			validateThread();
			s.setTime(parameterName, x);
		}

		@Override
		public void setTimestamp(String parameterName, Timestamp x)
				throws SQLException {
			validateThread();
			s.setTimestamp(parameterName, x);
		}

		@Override
		public void setAsciiStream(String parameterName, InputStream x,
				int length) throws SQLException {
			validateThread();
			s.setAsciiStream(parameterName, x, length);
		}

		@Override
		public void setBinaryStream(String parameterName, InputStream x,
				int length) throws SQLException {
			validateThread();
			s.setBinaryStream(parameterName, x, length);
		}

		@Override
		public void setObject(String parameterName, Object x, int targetSqlType,
				int scale) throws SQLException {
			validateThread();
			s.setObject(parameterName, x, targetSqlType, scale);
		}

		@Override
		public void setObject(String parameterName, Object x, int targetSqlType)
				throws SQLException {
			validateThread();
			s.setObject(parameterName, x, targetSqlType);
		}

		@Override
		public void setObject(String parameterName, Object x)
				throws SQLException {
			validateThread();
			s.setObject(parameterName, x);
		}

		@Override
		public void setCharacterStream(String parameterName, Reader reader,
				int length) throws SQLException {
			validateThread();
			s.setCharacterStream(parameterName, reader, length);
		}

		@Override
		public void setDate(String parameterName, Date x, Calendar cal)
				throws SQLException {
			validateThread();
			s.setDate(parameterName, x, cal);
		}

		@Override
		public void setTime(String parameterName, Time x, Calendar cal)
				throws SQLException {
			validateThread();
			s.setTime(parameterName, x, cal);
		}

		@Override
		public void setTimestamp(String parameterName, Timestamp x,
				Calendar cal) throws SQLException {
			validateThread();
			s.setTimestamp(parameterName, x, cal);
		}

		@Override
		public void setNull(String parameterName, int sqlType, String typeName)
				throws SQLException {
			validateThread();
			s.setNull(parameterName, sqlType, typeName);
		}

		@Override
		public String getString(String parameterName) throws SQLException {
			validateThread();
			return s.getString(parameterName);
		}

		@Override
		public boolean getBoolean(String parameterName) throws SQLException {
			validateThread();
			return s.getBoolean(parameterName);
		}

		@Override
		public byte getByte(String parameterName) throws SQLException {
			validateThread();
			return s.getByte(parameterName);
		}

		@Override
		public short getShort(String parameterName) throws SQLException {
			validateThread();
			return s.getShort(parameterName);
		}

		@Override
		public int getInt(String parameterName) throws SQLException {
			validateThread();
			return s.getInt(parameterName);
		}

		@Override
		public long getLong(String parameterName) throws SQLException {
			validateThread();
			return s.getLong(parameterName);
		}

		@Override
		public float getFloat(String parameterName) throws SQLException {
			validateThread();
			return s.getFloat(parameterName);
		}

		@Override
		public double getDouble(String parameterName) throws SQLException {
			validateThread();
			return s.getDouble(parameterName);
		}

		@Override
		public byte[] getBytes(String parameterName) throws SQLException {
			validateThread();
			return s.getBytes(parameterName);
		}

		@Override
		public Date getDate(String parameterName) throws SQLException {
			validateThread();
			return s.getDate(parameterName);
		}

		@Override
		public Time getTime(String parameterName) throws SQLException {
			validateThread();
			return s.getTime(parameterName);
		}

		@Override
		public Timestamp getTimestamp(String parameterName)
				throws SQLException {
			validateThread();
			return s.getTimestamp(parameterName);
		}

		@Override
		public Object getObject(String parameterName) throws SQLException {
			validateThread();
			return s.getObject(parameterName);
		}

		@Override
		public BigDecimal getBigDecimal(String parameterName)
				throws SQLException {
			validateThread();
			return s.getBigDecimal(parameterName);
		}

		@Override
		public Object getObject(String parameterName, Map<String, Class<?>> map)
				throws SQLException {
			validateThread();
			return s.getObject(parameterName, map);
		}

		@Override
		public Ref getRef(String parameterName) throws SQLException {
			validateThread();
			return s.getRef(parameterName);
		}

		@Override
		public Blob getBlob(String parameterName) throws SQLException {
			validateThread();
			return s.getBlob(parameterName);
		}

		@Override
		public Clob getClob(String parameterName) throws SQLException {
			validateThread();
			return s.getClob(parameterName);
		}

		@Override
		public Array getArray(String parameterName) throws SQLException {
			validateThread();
			return s.getArray(parameterName);
		}

		@Override
		public Date getDate(String parameterName, Calendar cal)
				throws SQLException {
			validateThread();
			return s.getDate(parameterName, cal);
		}

		@Override
		public Time getTime(String parameterName, Calendar cal)
				throws SQLException {
			validateThread();
			return s.getTime(parameterName, cal);
		}

		@Override
		public Timestamp getTimestamp(String parameterName, Calendar cal)
				throws SQLException {
			validateThread();
			return s.getTimestamp(parameterName, cal);
		}

		@Override
		public URL getURL(String parameterName) throws SQLException {
			validateThread();
			return s.getURL(parameterName);
		}

		@Override
		public RowId getRowId(int parameterIndex) throws SQLException {
			validateThread();
			return s.getRowId(parameterIndex);
		}

		@Override
		public RowId getRowId(String parameterName) throws SQLException {
			validateThread();
			return s.getRowId(parameterName);
		}

		@Override
		public void setRowId(String parameterName, RowId x)
				throws SQLException {
			validateThread();
			s.setRowId(parameterName, x);
		}

		@Override
		public void setNString(String parameterName, String value)
				throws SQLException {
			validateThread();
			s.setNString(parameterName, value);
		}

		@Override
		public void setNCharacterStream(String parameterName, Reader value,
				long length) throws SQLException {
			validateThread();
			s.setNCharacterStream(parameterName, value, length);
		}

		@Override
		public void setNClob(String parameterName, NClob value)
				throws SQLException {
			validateThread();
			s.setNClob(parameterName, value);
		}

		@Override
		public void setClob(String parameterName, Reader reader, long length)
				throws SQLException {
			validateThread();
			s.setNClob(parameterName, reader, length);
		}

		@Override
		public void setBlob(String parameterName, InputStream inputStream,
				long length) throws SQLException {
			validateThread();
			s.setBlob(parameterName, inputStream, length);
		}

		@Override
		public void setNClob(String parameterName, Reader reader, long length)
				throws SQLException {
			validateThread();
			s.setNClob(parameterName, reader, length);
		}

		@Override
		public NClob getNClob(int parameterIndex) throws SQLException {
			validateThread();
			return s.getNClob(parameterIndex);
		}

		@Override
		public NClob getNClob(String parameterName) throws SQLException {
			validateThread();
			return s.getNClob(parameterName);
		}

		@Override
		public void setSQLXML(String parameterName, SQLXML xmlObject)
				throws SQLException {
			validateThread();
			s.setSQLXML(parameterName, xmlObject);
		}

		@Override
		public SQLXML getSQLXML(int parameterIndex) throws SQLException {
			validateThread();
			return s.getSQLXML(parameterIndex);
		}

		@Override
		public SQLXML getSQLXML(String parameterName) throws SQLException {
			validateThread();
			return s.getSQLXML(parameterName);
		}

		@Override
		public String getNString(int parameterIndex) throws SQLException {
			validateThread();
			return s.getNString(parameterIndex);
		}

		@Override
		public String getNString(String parameterName) throws SQLException {
			validateThread();
			return s.getNString(parameterName);
		}

		@Override
		public Reader getNCharacterStream(int parameterIndex)
				throws SQLException {
			validateThread();
			return s.getNCharacterStream(parameterIndex);
		}

		@Override
		public Reader getNCharacterStream(String parameterName)
				throws SQLException {
			validateThread();
			return s.getNCharacterStream(parameterName);
		}

		@Override
		public Reader getCharacterStream(int parameterIndex)
				throws SQLException {
			validateThread();
			return s.getCharacterStream(parameterIndex);
		}

		@Override
		public Reader getCharacterStream(String parameterName)
				throws SQLException {
			validateThread();
			return s.getCharacterStream(parameterName);
		}

		@Override
		public void setBlob(String parameterName, Blob x) throws SQLException {
			validateThread();
			s.setBlob(parameterName, x);
		}

		@Override
		public void setClob(String parameterName, Clob x) throws SQLException {
			validateThread();
			s.setClob(parameterName, x);
		}

		@Override
		public void setAsciiStream(String parameterName, InputStream x,
				long length) throws SQLException {
			validateThread();
			s.setAsciiStream(parameterName, x, length);
		}

		@Override
		public void setBinaryStream(String parameterName, InputStream x,
				long length) throws SQLException {
			validateThread();
			s.setBinaryStream(parameterName, x, length);
		}

		@Override
		public void setCharacterStream(String parameterName, Reader reader,
				long length) throws SQLException {
			validateThread();
			s.setCharacterStream(parameterName, reader, length);
		}

		@Override
		public void setAsciiStream(String parameterName, InputStream x)
				throws SQLException {
			validateThread();
			s.setAsciiStream(parameterName, x);
		}

		@Override
		public void setBinaryStream(String parameterName, InputStream x)
				throws SQLException {
			validateThread();
			s.setBinaryStream(parameterName, x);
		}

		@Override
		public void setCharacterStream(String parameterName, Reader reader)
				throws SQLException {
			validateThread();
			s.setCharacterStream(parameterName, reader);
		}

		@Override
		public void setNCharacterStream(String parameterName, Reader value)
				throws SQLException {
			validateThread();
			s.setNCharacterStream(parameterName, value);
		}

		@Override
		public void setClob(String parameterName, Reader reader)
				throws SQLException {
			validateThread();
			s.setClob(parameterName, reader);
		}

		@Override
		public void setBlob(String parameterName, InputStream inputStream)
				throws SQLException {
			validateThread();
			s.setBlob(parameterName, inputStream);
		}

		@Override
		public void setNClob(String parameterName, Reader reader)
				throws SQLException {
			validateThread();
			s.setNClob(parameterName, reader);
		}

		@Override
		public <T> T getObject(int parameterIndex, Class<T> type)
				throws SQLException {
			validateThread();
			return s.getObject(parameterIndex, type);
		}

		@Override
		public <T> T getObject(String parameterName, Class<T> type)
				throws SQLException {
			validateThread();
			return s.getObject(parameterName, type);
		}
	}

	private class OTResults extends OTWrapper implements ResultSet {
		private final ResultSet r;
		private final Statement s;

		OTResults(Statement s, ResultSet r) {
			super(r);
			this.s = s;
			this.r = r;
		}

		OTResults(ResultSet r) {
			super(r);
			this.s = null;
			this.r = r;
		}

		@Override
		public boolean next() throws SQLException {
			validateThread();
			return r.next();
		}

		@Override
		public void close() throws SQLException {
			validateThread();
			r.close();
		}

		@Override
		public boolean wasNull() throws SQLException {
			validateThread();
			return r.wasNull();
		}

		@Override
		public String getString(int columnIndex) throws SQLException {
			validateThread();
			return r.getString(columnIndex);
		}

		@Override
		public boolean getBoolean(int columnIndex) throws SQLException {
			validateThread();
			return r.getBoolean(columnIndex);
		}

		@Override
		public byte getByte(int columnIndex) throws SQLException {
			validateThread();
			return r.getByte(columnIndex);
		}

		@Override
		public short getShort(int columnIndex) throws SQLException {
			validateThread();
			return r.getShort(columnIndex);
		}

		@Override
		public int getInt(int columnIndex) throws SQLException {
			validateThread();
			return r.getInt(columnIndex);
		}

		@Override
		public long getLong(int columnIndex) throws SQLException {
			validateThread();
			return r.getLong(columnIndex);
		}

		@Override
		public float getFloat(int columnIndex) throws SQLException {
			validateThread();
			return r.getFloat(columnIndex);
		}

		@Override
		public double getDouble(int columnIndex) throws SQLException {
			validateThread();
			return r.getDouble(columnIndex);
		}

		@Override
		@Deprecated
		public BigDecimal getBigDecimal(int columnIndex, int scale)
				throws SQLException {
			validateThread();
			return r.getBigDecimal(columnIndex, scale);
		}

		@Override
		public byte[] getBytes(int columnIndex) throws SQLException {
			validateThread();
			return r.getBytes(columnIndex);
		}

		@Override
		public Date getDate(int columnIndex) throws SQLException {
			validateThread();
			return r.getDate(columnIndex);
		}

		@Override
		public Time getTime(int columnIndex) throws SQLException {
			validateThread();
			return r.getTime(columnIndex);
		}

		@Override
		public Timestamp getTimestamp(int columnIndex) throws SQLException {
			validateThread();
			return r.getTimestamp(columnIndex);
		}

		@Override
		public InputStream getAsciiStream(int columnIndex) throws SQLException {
			validateThread();
			return r.getAsciiStream(columnIndex);
		}

		@Override
		@Deprecated
		public InputStream getUnicodeStream(int columnIndex)
				throws SQLException {
			validateThread();
			return r.getUnicodeStream(columnIndex);
		}

		@Override
		public InputStream getBinaryStream(int columnIndex)
				throws SQLException {
			validateThread();
			return r.getBinaryStream(columnIndex);
		}

		@Override
		public String getString(String columnLabel) throws SQLException {
			validateThread();
			return r.getString(columnLabel);
		}

		@Override
		public boolean getBoolean(String columnLabel) throws SQLException {
			validateThread();
			return r.getBoolean(columnLabel);
		}

		@Override
		public byte getByte(String columnLabel) throws SQLException {
			validateThread();
			return r.getByte(columnLabel);
		}

		@Override
		public short getShort(String columnLabel) throws SQLException {
			validateThread();
			return r.getShort(columnLabel);
		}

		@Override
		public int getInt(String columnLabel) throws SQLException {
			validateThread();
			return r.getInt(columnLabel);
		}

		@Override
		public long getLong(String columnLabel) throws SQLException {
			validateThread();
			return r.getLong(columnLabel);
		}

		@Override
		public float getFloat(String columnLabel) throws SQLException {
			validateThread();
			return r.getFloat(columnLabel);
		}

		@Override
		public double getDouble(String columnLabel) throws SQLException {
			validateThread();
			return r.getDouble(columnLabel);
		}

		@Override
		@Deprecated
		public BigDecimal getBigDecimal(String columnLabel, int scale)
				throws SQLException {
			validateThread();
			return r.getBigDecimal(columnLabel, scale);
		}

		@Override
		public byte[] getBytes(String columnLabel) throws SQLException {
			validateThread();
			return r.getBytes(columnLabel);
		}

		@Override
		public Date getDate(String columnLabel) throws SQLException {
			validateThread();
			return r.getDate(columnLabel);
		}

		@Override
		public Time getTime(String columnLabel) throws SQLException {
			validateThread();
			return r.getTime(columnLabel);
		}

		@Override
		public Timestamp getTimestamp(String columnLabel) throws SQLException {
			validateThread();
			return r.getTimestamp(columnLabel);
		}

		@Override
		public InputStream getAsciiStream(String columnLabel)
				throws SQLException {
			validateThread();
			return r.getAsciiStream(columnLabel);
		}

		@Override
		@Deprecated
		public InputStream getUnicodeStream(String columnLabel)
				throws SQLException {
			validateThread();
			return r.getUnicodeStream(columnLabel);
		}

		@Override
		public InputStream getBinaryStream(String columnLabel)
				throws SQLException {
			validateThread();
			return r.getBinaryStream(columnLabel);
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			validateThread();
			return r.getWarnings();
		}

		@Override
		public void clearWarnings() throws SQLException {
			validateThread();
			r.clearWarnings();
		}

		@Override
		public String getCursorName() throws SQLException {
			validateThread();
			return r.getCursorName();
		}

		@Override
		public ResultSetMetaData getMetaData() throws SQLException {
			validateThread();
			return new OTRSMeta(r.getMetaData());
		}

		@Override
		public Object getObject(int columnIndex) throws SQLException {
			validateThread();
			return r.getObject(columnIndex);
		}

		@Override
		public Object getObject(String columnLabel) throws SQLException {
			validateThread();
			return r.getObject(columnLabel);
		}

		@Override
		public int findColumn(String columnLabel) throws SQLException {
			validateThread();
			return r.findColumn(columnLabel);
		}

		@Override
		public Reader getCharacterStream(int columnIndex) throws SQLException {
			validateThread();
			return r.getCharacterStream(columnIndex);
		}

		@Override
		public Reader getCharacterStream(String columnLabel)
				throws SQLException {
			validateThread();
			return r.getCharacterStream(columnLabel);
		}

		@Override
		public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
			validateThread();
			return r.getBigDecimal(columnIndex);
		}

		@Override
		public BigDecimal getBigDecimal(String columnLabel)
				throws SQLException {
			validateThread();
			return r.getBigDecimal(columnLabel);
		}

		@Override
		public boolean isBeforeFirst() throws SQLException {
			validateThread();
			return r.isBeforeFirst();
		}

		@Override
		public boolean isAfterLast() throws SQLException {
			validateThread();
			return r.isAfterLast();
		}

		@Override
		public boolean isFirst() throws SQLException {
			validateThread();
			return r.isFirst();
		}

		@Override
		public boolean isLast() throws SQLException {
			validateThread();
			return r.isLast();
		}

		@Override
		public void beforeFirst() throws SQLException {
			validateThread();
			r.beforeFirst();
		}

		@Override
		public void afterLast() throws SQLException {
			validateThread();
			r.afterLast();
		}

		@Override
		public boolean first() throws SQLException {
			validateThread();
			return r.first();
		}

		@Override
		public boolean last() throws SQLException {
			validateThread();
			return r.last();
		}

		@Override
		public int getRow() throws SQLException {
			validateThread();
			return r.getRow();
		}

		@Override
		public boolean absolute(int row) throws SQLException {
			validateThread();
			return r.absolute(row);
		}

		@Override
		public boolean relative(int rows) throws SQLException {
			validateThread();
			return r.relative(rows);
		}

		@Override
		public boolean previous() throws SQLException {
			validateThread();
			return r.previous();
		}

		@Override
		public void setFetchDirection(int direction) throws SQLException {
			validateThread();
			r.setFetchDirection(direction);
		}

		@Override
		public int getFetchDirection() throws SQLException {
			validateThread();
			return r.getFetchDirection();
		}

		@Override
		public void setFetchSize(int rows) throws SQLException {
			validateThread();
			r.setFetchSize(rows);
		}

		@Override
		public int getFetchSize() throws SQLException {
			validateThread();
			return r.getFetchSize();
		}

		@Override
		public int getType() throws SQLException {
			validateThread();
			return r.getType();
		}

		@Override
		public int getConcurrency() throws SQLException {
			validateThread();
			return r.getConcurrency();
		}

		@Override
		public boolean rowUpdated() throws SQLException {
			validateThread();
			return r.rowUpdated();
		}

		@Override
		public boolean rowInserted() throws SQLException {
			validateThread();
			return r.rowInserted();
		}

		@Override
		public boolean rowDeleted() throws SQLException {
			validateThread();
			return r.rowDeleted();
		}

		@Override
		public void updateNull(int columnIndex) throws SQLException {
			validateThread();
			r.updateNull(columnIndex);
		}

		@Override
		public void updateBoolean(int columnIndex, boolean x)
				throws SQLException {
			validateThread();
			r.updateBoolean(columnIndex, x);
		}

		@Override
		public void updateByte(int columnIndex, byte x) throws SQLException {
			validateThread();
			r.updateByte(columnIndex, x);
		}

		@Override
		public void updateShort(int columnIndex, short x) throws SQLException {
			validateThread();
			r.updateShort(columnIndex, x);
		}

		@Override
		public void updateInt(int columnIndex, int x) throws SQLException {
			validateThread();
			r.updateInt(columnIndex, x);
		}

		@Override
		public void updateLong(int columnIndex, long x) throws SQLException {
			validateThread();
			r.updateLong(columnIndex, x);
		}

		@Override
		public void updateFloat(int columnIndex, float x) throws SQLException {
			validateThread();
			r.updateFloat(columnIndex, x);
		}

		@Override
		public void updateDouble(int columnIndex, double x)
				throws SQLException {
			validateThread();
			r.updateDouble(columnIndex, x);
		}

		@Override
		public void updateBigDecimal(int columnIndex, BigDecimal x)
				throws SQLException {
			validateThread();
			r.updateBigDecimal(columnIndex, x);
		}

		@Override
		public void updateString(int columnIndex, String x)
				throws SQLException {
			validateThread();
			r.updateString(columnIndex, x);
		}

		@Override
		public void updateBytes(int columnIndex, byte[] x) throws SQLException {
			validateThread();
			r.updateBytes(columnIndex, x);
		}

		@Override
		public void updateDate(int columnIndex, Date x) throws SQLException {
			validateThread();
			r.updateDate(columnIndex, x);
		}

		@Override
		public void updateTime(int columnIndex, Time x) throws SQLException {
			validateThread();
			r.updateTime(columnIndex, x);
		}

		@Override
		public void updateTimestamp(int columnIndex, Timestamp x)
				throws SQLException {
			validateThread();
			r.updateTimestamp(columnIndex, x);
		}

		@Override
		public void updateAsciiStream(int columnIndex, InputStream x,
				int length) throws SQLException {
			validateThread();
			r.updateAsciiStream(columnIndex, x, length);
		}

		@Override
		public void updateBinaryStream(int columnIndex, InputStream x,
				int length) throws SQLException {
			validateThread();
			r.updateBinaryStream(columnIndex, x, length);
		}

		@Override
		public void updateCharacterStream(int columnIndex, Reader x, int length)
				throws SQLException {
			validateThread();
			r.updateCharacterStream(columnIndex, x, length);
		}

		@Override
		public void updateObject(int columnIndex, Object x, int scaleOrLength)
				throws SQLException {
			validateThread();
			r.updateObject(columnIndex, x, scaleOrLength);
		}

		@Override
		public void updateObject(int columnIndex, Object x)
				throws SQLException {
			validateThread();
			r.updateObject(columnIndex, x);
		}

		@Override
		public void updateNull(String columnLabel) throws SQLException {
			validateThread();
			r.updateNull(columnLabel);
		}

		@Override
		public void updateBoolean(String columnLabel, boolean x)
				throws SQLException {
			validateThread();
			r.updateBoolean(columnLabel, x);
		}

		@Override
		public void updateByte(String columnLabel, byte x) throws SQLException {
			validateThread();
			r.updateByte(columnLabel, x);
		}

		@Override
		public void updateShort(String columnLabel, short x)
				throws SQLException {
			validateThread();
			r.updateShort(columnLabel, x);
		}

		@Override
		public void updateInt(String columnLabel, int x) throws SQLException {
			validateThread();
			r.updateInt(columnLabel, x);
		}

		@Override
		public void updateLong(String columnLabel, long x) throws SQLException {
			validateThread();
			r.updateLong(columnLabel, x);
		}

		@Override
		public void updateFloat(String columnLabel, float x)
				throws SQLException {
			validateThread();
			r.updateFloat(columnLabel, x);
		}

		@Override
		public void updateDouble(String columnLabel, double x)
				throws SQLException {
			validateThread();
			r.updateDouble(columnLabel, x);
		}

		@Override
		public void updateBigDecimal(String columnLabel, BigDecimal x)
				throws SQLException {
			validateThread();
			r.updateBigDecimal(columnLabel, x);
		}

		@Override
		public void updateString(String columnLabel, String x)
				throws SQLException {
			validateThread();
			r.updateString(columnLabel, x);
		}

		@Override
		public void updateBytes(String columnLabel, byte[] x)
				throws SQLException {
			validateThread();
			r.updateBytes(columnLabel, x);
		}

		@Override
		public void updateDate(String columnLabel, Date x) throws SQLException {
			validateThread();
			r.updateDate(columnLabel, x);
		}

		@Override
		public void updateTime(String columnLabel, Time x) throws SQLException {
			validateThread();
			r.updateTime(columnLabel, x);
		}

		@Override
		public void updateTimestamp(String columnLabel, Timestamp x)
				throws SQLException {
			validateThread();
			r.updateTimestamp(columnLabel, x);
		}

		@Override
		public void updateAsciiStream(String columnLabel, InputStream x,
				int length) throws SQLException {
			validateThread();
			r.updateAsciiStream(columnLabel, x, length);
		}

		@Override
		public void updateBinaryStream(String columnLabel, InputStream x,
				int length) throws SQLException {
			validateThread();
			r.updateBinaryStream(columnLabel, x, length);
		}

		@Override
		public void updateCharacterStream(String columnLabel, Reader reader,
				int length) throws SQLException {
			validateThread();
			r.updateCharacterStream(columnLabel, reader, length);
		}

		@Override
		public void updateObject(String columnLabel, Object x,
				int scaleOrLength) throws SQLException {
			validateThread();
			r.updateObject(columnLabel, x, scaleOrLength);
		}

		@Override
		public void updateObject(String columnLabel, Object x)
				throws SQLException {
			validateThread();
			r.updateObject(columnLabel, x);
		}

		@Override
		public void insertRow() throws SQLException {
			validateThread();
			r.insertRow();
		}

		@Override
		public void updateRow() throws SQLException {
			validateThread();
			r.updateRow();
		}

		@Override
		public void deleteRow() throws SQLException {
			validateThread();
			r.deleteRow();
		}

		@Override
		public void refreshRow() throws SQLException {
			validateThread();
			r.refreshRow();
		}

		@Override
		public void cancelRowUpdates() throws SQLException {
			validateThread();
			r.cancelRowUpdates();
		}

		@Override
		public void moveToInsertRow() throws SQLException {
			validateThread();
			r.moveToInsertRow();
		}

		@Override
		public void moveToCurrentRow() throws SQLException {
			validateThread();
			r.moveToCurrentRow();
		}

		@Override
		public Statement getStatement() throws SQLException {
			if (s != null) {
				return s;
			}
			validateThread();
			Statement st = r.getStatement();
			if (st instanceof CallableStatement) {
				return new OTCallable((CallableStatement) st);
			} else if (st instanceof PreparedStatement) {
				return new OTPrepared((PreparedStatement) st);
			} else {
				return new OTStatement(st);
			}
		}

		@Override
		public Object getObject(int columnIndex, Map<String, Class<?>> map)
				throws SQLException {
			validateThread();
			return r.getObject(columnIndex, map);
		}

		@Override
		public Ref getRef(int columnIndex) throws SQLException {
			validateThread();
			return r.getRef(columnIndex);
		}

		@Override
		public Blob getBlob(int columnIndex) throws SQLException {
			validateThread();
			return r.getBlob(columnIndex);
		}

		@Override
		public Clob getClob(int columnIndex) throws SQLException {
			validateThread();
			return r.getClob(columnIndex);
		}

		@Override
		public Array getArray(int columnIndex) throws SQLException {
			validateThread();
			return r.getArray(columnIndex);
		}

		@Override
		public Object getObject(String columnLabel, Map<String, Class<?>> map)
				throws SQLException {
			validateThread();
			return r.getObject(columnLabel, map);
		}

		@Override
		public Ref getRef(String columnLabel) throws SQLException {
			validateThread();
			return r.getRef(columnLabel);
		}

		@Override
		public Blob getBlob(String columnLabel) throws SQLException {
			validateThread();
			return r.getBlob(columnLabel);
		}

		@Override
		public Clob getClob(String columnLabel) throws SQLException {
			validateThread();
			return r.getClob(columnLabel);
		}

		@Override
		public Array getArray(String columnLabel) throws SQLException {
			validateThread();
			return r.getArray(columnLabel);
		}

		@Override
		public Date getDate(int columnIndex, Calendar cal) throws SQLException {
			validateThread();
			return r.getDate(columnIndex, cal);
		}

		@Override
		public Date getDate(String columnLabel, Calendar cal)
				throws SQLException {
			validateThread();
			return r.getDate(columnLabel, cal);
		}

		@Override
		public Time getTime(int columnIndex, Calendar cal) throws SQLException {
			validateThread();
			return r.getTime(columnIndex, cal);
		}

		@Override
		public Time getTime(String columnLabel, Calendar cal)
				throws SQLException {
			validateThread();
			return r.getTime(columnLabel, cal);
		}

		@Override
		public Timestamp getTimestamp(int columnIndex, Calendar cal)
				throws SQLException {
			validateThread();
			return r.getTimestamp(columnIndex, cal);
		}

		@Override
		public Timestamp getTimestamp(String columnLabel, Calendar cal)
				throws SQLException {
			validateThread();
			return r.getTimestamp(columnLabel, cal);
		}

		@Override
		public URL getURL(int columnIndex) throws SQLException {
			validateThread();
			return r.getURL(columnIndex);
		}

		@Override
		public URL getURL(String columnLabel) throws SQLException {
			validateThread();
			return r.getURL(columnLabel);
		}

		@Override
		public void updateRef(int columnIndex, Ref x) throws SQLException {
			validateThread();
			r.updateRef(columnIndex, x);
		}

		@Override
		public void updateRef(String columnLabel, Ref x) throws SQLException {
			validateThread();
			r.updateRef(columnLabel, x);
		}

		@Override
		public void updateBlob(int columnIndex, Blob x) throws SQLException {
			validateThread();
			r.updateBlob(columnIndex, x);
		}

		@Override
		public void updateBlob(String columnLabel, Blob x) throws SQLException {
			validateThread();
			r.updateBlob(columnLabel, x);
		}

		@Override
		public void updateClob(int columnIndex, Clob x) throws SQLException {
			validateThread();
			r.updateClob(columnIndex, x);
		}

		@Override
		public void updateClob(String columnLabel, Clob x) throws SQLException {
			validateThread();
			r.updateClob(columnLabel, x);
		}

		@Override
		public void updateArray(int columnIndex, Array x) throws SQLException {
			validateThread();
			r.updateArray(columnIndex, x);
		}

		@Override
		public void updateArray(String columnLabel, Array x)
				throws SQLException {
			validateThread();
			r.updateArray(columnLabel, x);
		}

		@Override
		public RowId getRowId(int columnIndex) throws SQLException {
			validateThread();
			return r.getRowId(columnIndex);
		}

		@Override
		public RowId getRowId(String columnLabel) throws SQLException {
			validateThread();
			return r.getRowId(columnLabel);
		}

		@Override
		public void updateRowId(int columnIndex, RowId x) throws SQLException {
			validateThread();
			r.updateRowId(columnIndex, x);
		}

		@Override
		public void updateRowId(String columnLabel, RowId x)
				throws SQLException {
			validateThread();
			r.updateRowId(columnLabel, x);
		}

		@Override
		public int getHoldability() throws SQLException {
			validateThread();
			return r.getHoldability();
		}

		@Override
		public boolean isClosed() throws SQLException {
			validateThread();
			return r.isClosed();
		}

		@Override
		public void updateNString(int columnIndex, String nString)
				throws SQLException {
			validateThread();
			r.updateNString(columnIndex, nString);
		}

		@Override
		public void updateNString(String columnLabel, String nString)
				throws SQLException {
			validateThread();
			r.updateNString(columnLabel, nString);
		}

		@Override
		public void updateNClob(int columnIndex, NClob nClob)
				throws SQLException {
			validateThread();
			r.updateNClob(columnIndex, nClob);
		}

		@Override
		public void updateNClob(String columnLabel, NClob nClob)
				throws SQLException {
			validateThread();
			r.updateNClob(columnLabel, nClob);
		}

		@Override
		public NClob getNClob(int columnIndex) throws SQLException {
			validateThread();
			return r.getNClob(columnIndex);
		}

		@Override
		public NClob getNClob(String columnLabel) throws SQLException {
			validateThread();
			return r.getNClob(columnLabel);
		}

		@Override
		public SQLXML getSQLXML(int columnIndex) throws SQLException {
			validateThread();
			return r.getSQLXML(columnIndex);
		}

		@Override
		public SQLXML getSQLXML(String columnLabel) throws SQLException {
			validateThread();
			return r.getSQLXML(columnLabel);
		}

		@Override
		public void updateSQLXML(int columnIndex, SQLXML xmlObject)
				throws SQLException {
			validateThread();
			r.updateSQLXML(columnIndex, xmlObject);
		}

		@Override
		public void updateSQLXML(String columnLabel, SQLXML xmlObject)
				throws SQLException {
			validateThread();
			r.updateSQLXML(columnLabel, xmlObject);
		}

		@Override
		public String getNString(int columnIndex) throws SQLException {
			validateThread();
			return r.getNString(columnIndex);
		}

		@Override
		public String getNString(String columnLabel) throws SQLException {
			validateThread();
			return r.getNString(columnLabel);
		}

		@Override
		public Reader getNCharacterStream(int columnIndex) throws SQLException {
			validateThread();
			return r.getNCharacterStream(columnIndex);
		}

		@Override
		public Reader getNCharacterStream(String columnLabel)
				throws SQLException {
			validateThread();
			return r.getNCharacterStream(columnLabel);
		}

		@Override
		public void updateNCharacterStream(int columnIndex, Reader x,
				long length) throws SQLException {
			validateThread();
			r.updateNCharacterStream(columnIndex, x, length);
		}

		@Override
		public void updateNCharacterStream(String columnLabel, Reader reader,
				long length) throws SQLException {
			validateThread();
			r.updateNCharacterStream(columnLabel, reader, length);
		}

		@Override
		public void updateAsciiStream(int columnIndex, InputStream x,
				long length) throws SQLException {
			validateThread();
			r.updateAsciiStream(columnIndex, x, length);
		}

		@Override
		public void updateBinaryStream(int columnIndex, InputStream x,
				long length) throws SQLException {
			validateThread();
			r.updateBinaryStream(columnIndex, x, length);
		}

		@Override
		public void updateCharacterStream(int columnIndex, Reader x,
				long length) throws SQLException {
			validateThread();
			r.updateCharacterStream(columnIndex, x, length);
		}

		@Override
		public void updateAsciiStream(String columnLabel, InputStream x,
				long length) throws SQLException {
			validateThread();
			r.updateAsciiStream(columnLabel, x, length);
		}

		@Override
		public void updateBinaryStream(String columnLabel, InputStream x,
				long length) throws SQLException {
			validateThread();
			r.updateBinaryStream(columnLabel, x, length);
		}

		@Override
		public void updateCharacterStream(String columnLabel, Reader reader,
				long length) throws SQLException {
			validateThread();
			r.updateCharacterStream(columnLabel, reader, length);
		}

		@Override
		public void updateBlob(int columnIndex, InputStream inputStream,
				long length) throws SQLException {
			validateThread();
			r.updateBlob(columnIndex, inputStream, length);
		}

		@Override
		public void updateBlob(String columnLabel, InputStream inputStream,
				long length) throws SQLException {
			validateThread();
			r.updateBlob(columnLabel, inputStream, length);
		}

		@Override
		public void updateClob(int columnIndex, Reader reader, long length)
				throws SQLException {
			validateThread();
			r.updateClob(columnIndex, reader, length);
		}

		@Override
		public void updateClob(String columnLabel, Reader reader, long length)
				throws SQLException {
			validateThread();
			r.updateClob(columnLabel, reader, length);
		}

		@Override
		public void updateNClob(int columnIndex, Reader reader, long length)
				throws SQLException {
			validateThread();
			r.updateNClob(columnIndex, reader, length);
		}

		@Override
		public void updateNClob(String columnLabel, Reader reader, long length)
				throws SQLException {
			validateThread();
			r.updateNClob(columnLabel, reader, length);
		}

		@Override
		public void updateNCharacterStream(int columnIndex, Reader x)
				throws SQLException {
			validateThread();
			r.updateNCharacterStream(columnIndex, x);
		}

		@Override
		public void updateNCharacterStream(String columnLabel, Reader reader)
				throws SQLException {
			validateThread();
			r.updateNCharacterStream(columnLabel, reader);
		}

		@Override
		public void updateAsciiStream(int columnIndex, InputStream x)
				throws SQLException {
			validateThread();
			r.updateAsciiStream(columnIndex, x);
		}

		@Override
		public void updateBinaryStream(int columnIndex, InputStream x)
				throws SQLException {
			validateThread();
			r.updateBinaryStream(columnIndex, x);
		}

		@Override
		public void updateCharacterStream(int columnIndex, Reader x)
				throws SQLException {
			validateThread();
			r.updateCharacterStream(columnIndex, x);
		}

		@Override
		public void updateAsciiStream(String columnLabel, InputStream x)
				throws SQLException {
			validateThread();
			r.updateAsciiStream(columnLabel, x);
		}

		@Override
		public void updateBinaryStream(String columnLabel, InputStream x)
				throws SQLException {
			validateThread();
			r.updateBinaryStream(columnLabel, x);
		}

		@Override
		public void updateCharacterStream(String columnLabel, Reader reader)
				throws SQLException {
			validateThread();
			r.updateCharacterStream(columnLabel, reader);
		}

		@Override
		public void updateBlob(int columnIndex, InputStream inputStream)
				throws SQLException {
			validateThread();
			r.updateBlob(columnIndex, inputStream);
		}

		@Override
		public void updateBlob(String columnLabel, InputStream inputStream)
				throws SQLException {
			validateThread();
			r.updateBlob(columnLabel, inputStream);
		}

		@Override
		public void updateClob(int columnIndex, Reader reader)
				throws SQLException {
			validateThread();
			r.updateClob(columnIndex, reader);
		}

		@Override
		public void updateClob(String columnLabel, Reader reader)
				throws SQLException {
			validateThread();
			r.updateClob(columnLabel, reader);
		}

		@Override
		public void updateNClob(int columnIndex, Reader reader)
				throws SQLException {
			validateThread();
			r.updateNClob(columnIndex, reader);
		}

		@Override
		public void updateNClob(String columnLabel, Reader reader)
				throws SQLException {
			validateThread();
			r.updateNClob(columnLabel, reader);
		}

		@Override
		public <T> T getObject(int columnIndex, Class<T> type)
				throws SQLException {
			validateThread();
			return r.getObject(columnIndex, type);
		}

		@Override
		public <T> T getObject(String columnLabel, Class<T> type)
				throws SQLException {
			validateThread();
			return r.getObject(columnLabel, type);
		}
	}

	private class OTMeta extends OTWrapper implements DatabaseMetaData {
		private final DatabaseMetaData m;

		OTMeta(DatabaseMetaData m) {
			super(m);
			this.m = m;
		}

		@Override
		public boolean allProceduresAreCallable() throws SQLException {
			return m.allProceduresAreCallable();
		}

		@Override
		public boolean allTablesAreSelectable() throws SQLException {
			return m.allTablesAreSelectable();
		}

		@Override
		public String getURL() throws SQLException {
			return m.getURL();
		}

		@Override
		public String getUserName() throws SQLException {
			return m.getUserName();
		}

		@Override
		public boolean isReadOnly() throws SQLException {
			validateThread();
			return m.isReadOnly();
		}

		@Override
		public boolean nullsAreSortedHigh() throws SQLException {
			return m.nullsAreSortedHigh();
		}

		@Override
		public boolean nullsAreSortedLow() throws SQLException {
			return m.nullsAreSortedLow();
		}

		@Override
		public boolean nullsAreSortedAtStart() throws SQLException {
			return m.nullsAreSortedAtStart();
		}

		@Override
		public boolean nullsAreSortedAtEnd() throws SQLException {
			return m.nullsAreSortedAtEnd();
		}

		@Override
		public String getDatabaseProductName() throws SQLException {
			return m.getDatabaseProductName();
		}

		@Override
		public String getDatabaseProductVersion() throws SQLException {
			return m.getDatabaseProductVersion();
		}

		@Override
		public String getDriverName() throws SQLException {
			return m.getDriverName();
		}

		@Override
		public String getDriverVersion() throws SQLException {
			return m.getDriverVersion();
		}

		@Override
		public int getDriverMajorVersion() {
			return m.getDriverMajorVersion();
		}

		@Override
		public int getDriverMinorVersion() {
			return m.getDriverMinorVersion();
		}

		@Override
		public boolean usesLocalFiles() throws SQLException {
			return m.usesLocalFiles();
		}

		@Override
		public boolean usesLocalFilePerTable() throws SQLException {
			return m.usesLocalFilePerTable();
		}

		@Override
		public boolean supportsMixedCaseIdentifiers() throws SQLException {
			return m.supportsMixedCaseIdentifiers();
		}

		@Override
		public boolean storesUpperCaseIdentifiers() throws SQLException {
			return m.storesUpperCaseIdentifiers();
		}

		@Override
		public boolean storesLowerCaseIdentifiers() throws SQLException {
			return m.storesLowerCaseIdentifiers();
		}

		@Override
		public boolean storesMixedCaseIdentifiers() throws SQLException {
			return m.storesMixedCaseIdentifiers();
		}

		@Override
		public boolean supportsMixedCaseQuotedIdentifiers()
				throws SQLException {
			return m.supportsMixedCaseQuotedIdentifiers();
		}

		@Override
		public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
			return m.storesUpperCaseQuotedIdentifiers();
		}

		@Override
		public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
			return m.storesLowerCaseQuotedIdentifiers();
		}

		@Override
		public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
			return m.storesMixedCaseQuotedIdentifiers();
		}

		@Override
		public String getIdentifierQuoteString() throws SQLException {
			return m.getIdentifierQuoteString();
		}

		@Override
		public String getSQLKeywords() throws SQLException {
			return m.getSQLKeywords();
		}

		@Override
		public String getNumericFunctions() throws SQLException {
			return m.getNumericFunctions();
		}

		@Override
		public String getStringFunctions() throws SQLException {
			return m.getStringFunctions();
		}

		@Override
		public String getSystemFunctions() throws SQLException {
			return m.getSystemFunctions();
		}

		@Override
		public String getTimeDateFunctions() throws SQLException {
			return m.getTimeDateFunctions();
		}

		@Override
		public String getSearchStringEscape() throws SQLException {
			return m.getSearchStringEscape();
		}

		@Override
		public String getExtraNameCharacters() throws SQLException {
			return m.getExtraNameCharacters();
		}

		@Override
		public boolean supportsAlterTableWithAddColumn() throws SQLException {
			return m.supportsAlterTableWithAddColumn();
		}

		@Override
		public boolean supportsAlterTableWithDropColumn() throws SQLException {
			return m.supportsAlterTableWithDropColumn();
		}

		@Override
		public boolean supportsColumnAliasing() throws SQLException {
			return m.supportsColumnAliasing();
		}

		@Override
		public boolean nullPlusNonNullIsNull() throws SQLException {
			return m.nullPlusNonNullIsNull();
		}

		@Override
		public boolean supportsConvert() throws SQLException {
			return m.supportsConvert();
		}

		@Override
		public boolean supportsConvert(int fromType, int toType)
				throws SQLException {
			return m.supportsConvert(fromType, toType);
		}

		@Override
		public boolean supportsTableCorrelationNames() throws SQLException {
			return m.supportsTableCorrelationNames();
		}

		@Override
		public boolean supportsDifferentTableCorrelationNames()
				throws SQLException {
			return m.supportsDifferentTableCorrelationNames();
		}

		@Override
		public boolean supportsExpressionsInOrderBy() throws SQLException {
			return m.supportsExpressionsInOrderBy();
		}

		@Override
		public boolean supportsOrderByUnrelated() throws SQLException {
			return m.supportsOrderByUnrelated();
		}

		@Override
		public boolean supportsGroupBy() throws SQLException {
			return m.supportsGroupBy();
		}

		@Override
		public boolean supportsGroupByUnrelated() throws SQLException {
			return m.supportsGroupByUnrelated();
		}

		@Override
		public boolean supportsGroupByBeyondSelect() throws SQLException {
			return m.supportsGroupByBeyondSelect();
		}

		@Override
		public boolean supportsLikeEscapeClause() throws SQLException {
			return m.supportsLikeEscapeClause();
		}

		@Override
		public boolean supportsMultipleResultSets() throws SQLException {
			return m.supportsMultipleResultSets();
		}

		@Override
		public boolean supportsMultipleTransactions() throws SQLException {
			return m.supportsMultipleTransactions();
		}

		@Override
		public boolean supportsNonNullableColumns() throws SQLException {
			return m.supportsNonNullableColumns();
		}

		@Override
		public boolean supportsMinimumSQLGrammar() throws SQLException {
			return m.supportsMinimumSQLGrammar();
		}

		@Override
		public boolean supportsCoreSQLGrammar() throws SQLException {
			return m.supportsCoreSQLGrammar();
		}

		@Override
		public boolean supportsExtendedSQLGrammar() throws SQLException {
			return m.supportsExtendedSQLGrammar();
		}

		@Override
		public boolean supportsANSI92EntryLevelSQL() throws SQLException {
			return m.supportsANSI92EntryLevelSQL();
		}

		@Override
		public boolean supportsANSI92IntermediateSQL() throws SQLException {
			return m.supportsANSI92IntermediateSQL();
		}

		@Override
		public boolean supportsANSI92FullSQL() throws SQLException {
			return m.supportsANSI92FullSQL();
		}

		@Override
		public boolean supportsIntegrityEnhancementFacility()
				throws SQLException {
			return m.supportsIntegrityEnhancementFacility();
		}

		@Override
		public boolean supportsOuterJoins() throws SQLException {
			return m.supportsOuterJoins();
		}

		@Override
		public boolean supportsFullOuterJoins() throws SQLException {
			return m.supportsFullOuterJoins();
		}

		@Override
		public boolean supportsLimitedOuterJoins() throws SQLException {
			return m.supportsLimitedOuterJoins();
		}

		@Override
		public String getSchemaTerm() throws SQLException {
			return m.getSchemaTerm();
		}

		@Override
		public String getProcedureTerm() throws SQLException {
			return m.getProcedureTerm();
		}

		@Override
		public String getCatalogTerm() throws SQLException {
			return m.getCatalogTerm();
		}

		@Override
		public boolean isCatalogAtStart() throws SQLException {
			return m.isCatalogAtStart();
		}

		@Override
		public String getCatalogSeparator() throws SQLException {
			return m.getCatalogSeparator();
		}

		@Override
		public boolean supportsSchemasInDataManipulation() throws SQLException {
			return m.supportsSchemasInDataManipulation();
		}

		@Override
		public boolean supportsSchemasInProcedureCalls() throws SQLException {
			return m.supportsSchemasInProcedureCalls();
		}

		@Override
		public boolean supportsSchemasInTableDefinitions() throws SQLException {
			return m.supportsSchemasInTableDefinitions();
		}

		@Override
		public boolean supportsSchemasInIndexDefinitions() throws SQLException {
			return m.supportsSchemasInIndexDefinitions();
		}

		@Override
		public boolean supportsSchemasInPrivilegeDefinitions()
				throws SQLException {
			return m.supportsSchemasInPrivilegeDefinitions();
		}

		@Override
		public boolean supportsCatalogsInDataManipulation()
				throws SQLException {
			return m.supportsCatalogsInDataManipulation();
		}

		@Override
		public boolean supportsCatalogsInProcedureCalls() throws SQLException {
			return m.supportsCatalogsInProcedureCalls();
		}

		@Override
		public boolean supportsCatalogsInTableDefinitions()
				throws SQLException {
			return m.supportsCatalogsInTableDefinitions();
		}

		@Override
		public boolean supportsCatalogsInIndexDefinitions()
				throws SQLException {
			return m.supportsCatalogsInIndexDefinitions();
		}

		@Override
		public boolean supportsCatalogsInPrivilegeDefinitions()
				throws SQLException {
			return m.supportsCatalogsInPrivilegeDefinitions();
		}

		@Override
		public boolean supportsPositionedDelete() throws SQLException {
			return m.supportsPositionedDelete();
		}

		@Override
		public boolean supportsPositionedUpdate() throws SQLException {
			return m.supportsPositionedUpdate();
		}

		@Override
		public boolean supportsSelectForUpdate() throws SQLException {
			return m.supportsSelectForUpdate();
		}

		@Override
		public boolean supportsStoredProcedures() throws SQLException {
			return m.supportsStoredProcedures();
		}

		@Override
		public boolean supportsSubqueriesInComparisons() throws SQLException {
			return m.supportsSubqueriesInComparisons();
		}

		@Override
		public boolean supportsSubqueriesInExists() throws SQLException {
			return m.supportsSubqueriesInExists();
		}

		@Override
		public boolean supportsSubqueriesInIns() throws SQLException {
			return m.supportsSubqueriesInIns();
		}

		@Override
		public boolean supportsSubqueriesInQuantifieds() throws SQLException {
			return m.supportsSubqueriesInQuantifieds();
		}

		@Override
		public boolean supportsCorrelatedSubqueries() throws SQLException {
			return m.supportsCorrelatedSubqueries();
		}

		@Override
		public boolean supportsUnion() throws SQLException {
			return m.supportsUnion();
		}

		@Override
		public boolean supportsUnionAll() throws SQLException {
			return m.supportsUnionAll();
		}

		@Override
		public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
			return m.supportsOpenCursorsAcrossCommit();
		}

		@Override
		public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
			return m.supportsOpenCursorsAcrossRollback();
		}

		@Override
		public boolean supportsOpenStatementsAcrossCommit()
				throws SQLException {
			return m.supportsOpenStatementsAcrossCommit();
		}

		@Override
		public boolean supportsOpenStatementsAcrossRollback()
				throws SQLException {
			return m.supportsOpenStatementsAcrossRollback();
		}

		@Override
		public int getMaxBinaryLiteralLength() throws SQLException {
			return m.getMaxBinaryLiteralLength();
		}

		@Override
		public int getMaxCharLiteralLength() throws SQLException {
			return m.getMaxCharLiteralLength();
		}

		@Override
		public int getMaxColumnNameLength() throws SQLException {
			return m.getMaxColumnNameLength();
		}

		@Override
		public int getMaxColumnsInGroupBy() throws SQLException {
			return m.getMaxColumnsInGroupBy();
		}

		@Override
		public int getMaxColumnsInIndex() throws SQLException {
			return m.getMaxColumnsInIndex();
		}

		@Override
		public int getMaxColumnsInOrderBy() throws SQLException {
			return m.getMaxColumnsInOrderBy();
		}

		@Override
		public int getMaxColumnsInSelect() throws SQLException {
			return m.getMaxColumnsInSelect();
		}

		@Override
		public int getMaxColumnsInTable() throws SQLException {
			return m.getMaxColumnsInTable();
		}

		@Override
		public int getMaxConnections() throws SQLException {
			return m.getMaxConnections();
		}

		@Override
		public int getMaxCursorNameLength() throws SQLException {
			return m.getMaxCursorNameLength();
		}

		@Override
		public int getMaxIndexLength() throws SQLException {
			return m.getMaxIndexLength();
		}

		@Override
		public int getMaxSchemaNameLength() throws SQLException {
			return m.getMaxSchemaNameLength();
		}

		@Override
		public int getMaxProcedureNameLength() throws SQLException {
			return m.getMaxProcedureNameLength();
		}

		@Override
		public int getMaxCatalogNameLength() throws SQLException {
			return m.getMaxCatalogNameLength();
		}

		@Override
		public int getMaxRowSize() throws SQLException {
			return m.getMaxRowSize();
		}

		@Override
		public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
			return m.doesMaxRowSizeIncludeBlobs();
		}

		@Override
		public int getMaxStatementLength() throws SQLException {
			return m.getMaxStatementLength();
		}

		@Override
		public int getMaxStatements() throws SQLException {
			return m.getMaxStatements();
		}

		@Override
		public int getMaxTableNameLength() throws SQLException {
			return m.getMaxTableNameLength();
		}

		@Override
		public int getMaxTablesInSelect() throws SQLException {
			return m.getMaxTablesInSelect();
		}

		@Override
		public int getMaxUserNameLength() throws SQLException {
			return m.getMaxUserNameLength();
		}

		@Override
		public int getDefaultTransactionIsolation() throws SQLException {
			return m.getDefaultTransactionIsolation();
		}

		@Override
		public boolean supportsTransactions() throws SQLException {
			return m.supportsTransactions();
		}

		@Override
		public boolean supportsTransactionIsolationLevel(int level)
				throws SQLException {
			return m.supportsTransactionIsolationLevel(level);
		}

		@Override
		public boolean supportsDataDefinitionAndDataManipulationTransactions()
				throws SQLException {
			return m.supportsDataDefinitionAndDataManipulationTransactions();
		}

		@Override
		public boolean supportsDataManipulationTransactionsOnly()
				throws SQLException {
			return m.supportsDataManipulationTransactionsOnly();
		}

		@Override
		public boolean dataDefinitionCausesTransactionCommit()
				throws SQLException {
			return m.dataDefinitionCausesTransactionCommit();
		}

		@Override
		public boolean dataDefinitionIgnoredInTransactions()
				throws SQLException {
			return m.dataDefinitionIgnoredInTransactions();
		}

		@Override
		public ResultSet getProcedures(String catalog, String schemaPattern,
				String procedureNamePattern) throws SQLException {
			validateThread();
			return new OTResults(m.getProcedures(catalog, schemaPattern,
					procedureNamePattern));
		}

		@Override
		public ResultSet getProcedureColumns(String catalog,
				String schemaPattern, String procedureNamePattern,
				String columnNamePattern) throws SQLException {
			validateThread();
			return new OTResults(m.getProcedureColumns(catalog, schemaPattern,
					procedureNamePattern, columnNamePattern));
		}

		@Override
		public ResultSet getTables(String catalog, String schemaPattern,
				String tableNamePattern, String[] types) throws SQLException {
			validateThread();
			return new OTResults(m.getTables(catalog, schemaPattern,
					tableNamePattern, types));
		}

		@Override
		public ResultSet getSchemas() throws SQLException {
			validateThread();
			return new OTResults(m.getSchemas());
		}

		@Override
		public ResultSet getCatalogs() throws SQLException {
			validateThread();
			return new OTResults(m.getCatalogs());
		}

		@Override
		public ResultSet getTableTypes() throws SQLException {
			validateThread();
			return new OTResults(m.getTableTypes());
		}

		@Override
		public ResultSet getColumns(String catalog, String schemaPattern,
				String tableNamePattern, String columnNamePattern)
				throws SQLException {
			validateThread();
			return new OTResults(m.getColumns(catalog, schemaPattern,
					tableNamePattern, columnNamePattern));
		}

		@Override
		public ResultSet getColumnPrivileges(String catalog, String schema,
				String table, String columnNamePattern) throws SQLException {
			validateThread();
			return new OTResults(m.getColumnPrivileges(catalog, schema, table,
					columnNamePattern));
		}

		@Override
		public ResultSet getTablePrivileges(String catalog,
				String schemaPattern, String tableNamePattern)
				throws SQLException {
			validateThread();
			return new OTResults(m.getTablePrivileges(catalog, schemaPattern,
					tableNamePattern));
		}

		@Override
		public ResultSet getBestRowIdentifier(String catalog, String schema,
				String table, int scope, boolean nullable) throws SQLException {
			validateThread();
			return new OTResults(m.getBestRowIdentifier(catalog, schema, table,
					scope, nullable));
		}

		@Override
		public ResultSet getVersionColumns(String catalog, String schema,
				String table) throws SQLException {
			validateThread();
			return new OTResults(m.getVersionColumns(catalog, schema, table));
		}

		@Override
		public ResultSet getPrimaryKeys(String catalog, String schema,
				String table) throws SQLException {
			validateThread();
			return new OTResults(m.getPrimaryKeys(catalog, schema, table));
		}

		@Override
		public ResultSet getImportedKeys(String catalog, String schema,
				String table) throws SQLException {
			validateThread();
			return new OTResults(m.getImportedKeys(catalog, schema, table));
		}

		@Override
		public ResultSet getExportedKeys(String catalog, String schema,
				String table) throws SQLException {
			validateThread();
			return new OTResults(m.getExportedKeys(catalog, schema, table));
		}

		@Override
		public ResultSet getCrossReference(String parentCatalog,
				String parentSchema, String parentTable, String foreignCatalog,
				String foreignSchema, String foreignTable) throws SQLException {
			validateThread();
			return new OTResults(m.getCrossReference(parentCatalog,
					parentSchema, parentTable, foreignCatalog, foreignSchema,
					foreignTable));
		}

		@Override
		public ResultSet getTypeInfo() throws SQLException {
			validateThread();
			return new OTResults(m.getTypeInfo());
		}

		@Override
		public ResultSet getIndexInfo(String catalog, String schema,
				String table, boolean unique, boolean approximate)
				throws SQLException {
			validateThread();
			return new OTResults(m.getIndexInfo(catalog, schema, table, unique,
					approximate));
		}

		@Override
		public boolean supportsResultSetType(int type) throws SQLException {
			return m.supportsResultSetType(type);
		}

		@Override
		public boolean supportsResultSetConcurrency(int type, int concurrency)
				throws SQLException {
			return m.supportsResultSetConcurrency(type, concurrency);
		}

		@Override
		public boolean ownUpdatesAreVisible(int type) throws SQLException {
			return m.ownUpdatesAreVisible(type);
		}

		@Override
		public boolean ownDeletesAreVisible(int type) throws SQLException {
			return m.ownDeletesAreVisible(type);
		}

		@Override
		public boolean ownInsertsAreVisible(int type) throws SQLException {
			return m.ownInsertsAreVisible(type);
		}

		@Override
		public boolean othersUpdatesAreVisible(int type) throws SQLException {
			return m.othersUpdatesAreVisible(type);
		}

		@Override
		public boolean othersDeletesAreVisible(int type) throws SQLException {
			return m.othersDeletesAreVisible(type);
		}

		@Override
		public boolean othersInsertsAreVisible(int type) throws SQLException {
			return m.othersInsertsAreVisible(type);
		}

		@Override
		public boolean updatesAreDetected(int type) throws SQLException {
			return m.updatesAreDetected(type);
		}

		@Override
		public boolean deletesAreDetected(int type) throws SQLException {
			return m.deletesAreDetected(type);
		}

		@Override
		public boolean insertsAreDetected(int type) throws SQLException {
			return m.insertsAreDetected(type);
		}

		@Override
		public boolean supportsBatchUpdates() throws SQLException {
			return m.supportsBatchUpdates();
		}

		@Override
		public ResultSet getUDTs(String catalog, String schemaPattern,
				String typeNamePattern, int[] types) throws SQLException {
			validateThread();
			return new OTResults(
					m.getUDTs(catalog, schemaPattern, typeNamePattern, types));
		}

		@Override
		public Connection getConnection() throws SQLException {
			return new OTConnection(m.getConnection());
		}

		@Override
		public boolean supportsSavepoints() throws SQLException {
			return m.supportsSavepoints();
		}

		@Override
		public boolean supportsNamedParameters() throws SQLException {
			return m.supportsNamedParameters();
		}

		@Override
		public boolean supportsMultipleOpenResults() throws SQLException {
			return m.supportsMultipleOpenResults();
		}

		@Override
		public boolean supportsGetGeneratedKeys() throws SQLException {
			return m.supportsGetGeneratedKeys();
		}

		@Override
		public ResultSet getSuperTypes(String catalog, String schemaPattern,
				String typeNamePattern) throws SQLException {
			validateThread();
			return new OTResults(
					m.getSuperTypes(catalog, schemaPattern, typeNamePattern));
		}

		@Override
		public ResultSet getSuperTables(String catalog, String schemaPattern,
				String tableNamePattern) throws SQLException {
			validateThread();
			return new OTResults(
					m.getSuperTables(catalog, schemaPattern, tableNamePattern));
		}

		@Override
		public ResultSet getAttributes(String catalog, String schemaPattern,
				String typeNamePattern, String attributeNamePattern)
				throws SQLException {
			validateThread();
			return new OTResults(m.getAttributes(catalog, schemaPattern,
					typeNamePattern, attributeNamePattern));
		}

		@Override
		public boolean supportsResultSetHoldability(int holdability)
				throws SQLException {
			return m.supportsResultSetHoldability(holdability);
		}

		@Override
		public int getResultSetHoldability() throws SQLException {
			return m.getResultSetHoldability();
		}

		@Override
		public int getDatabaseMajorVersion() throws SQLException {
			return m.getDatabaseMajorVersion();
		}

		@Override
		public int getDatabaseMinorVersion() throws SQLException {
			return m.getDatabaseMinorVersion();
		}

		@Override
		public int getJDBCMajorVersion() throws SQLException {
			return m.getJDBCMajorVersion();
		}

		@Override
		public int getJDBCMinorVersion() throws SQLException {
			return m.getJDBCMinorVersion();
		}

		@Override
		public int getSQLStateType() throws SQLException {
			return m.getSQLStateType();
		}

		@Override
		public boolean locatorsUpdateCopy() throws SQLException {
			return m.locatorsUpdateCopy();
		}

		@Override
		public boolean supportsStatementPooling() throws SQLException {
			return m.supportsStatementPooling();
		}

		@Override
		public RowIdLifetime getRowIdLifetime() throws SQLException {
			return m.getRowIdLifetime();
		}

		@Override
		public ResultSet getSchemas(String catalog, String schemaPattern)
				throws SQLException {
			validateThread();
			return new OTResults(m.getSchemas(catalog, schemaPattern));
		}

		@Override
		public boolean supportsStoredFunctionsUsingCallSyntax()
				throws SQLException {
			return m.supportsStoredFunctionsUsingCallSyntax();
		}

		@Override
		public boolean autoCommitFailureClosesAllResultSets()
				throws SQLException {
			return m.autoCommitFailureClosesAllResultSets();
		}

		@Override
		public ResultSet getClientInfoProperties() throws SQLException {
			validateThread();
			return new OTResults(m.getClientInfoProperties());
		}

		@Override
		public ResultSet getFunctions(String catalog, String schemaPattern,
				String functionNamePattern) throws SQLException {
			validateThread();
			return new OTResults(m.getFunctions(catalog, schemaPattern,
					functionNamePattern));
		}

		@Override
		public ResultSet getFunctionColumns(String catalog,
				String schemaPattern, String functionNamePattern,
				String columnNamePattern) throws SQLException {
			validateThread();
			return new OTResults(m.getFunctionColumns(catalog, schemaPattern,
					functionNamePattern, columnNamePattern));
		}

		@Override
		public ResultSet getPseudoColumns(String catalog, String schemaPattern,
				String tableNamePattern, String columnNamePattern)
				throws SQLException {
			validateThread();
			return new OTResults(m.getPseudoColumns(catalog, schemaPattern,
					tableNamePattern, columnNamePattern));
		}

		@Override
		public boolean generatedKeyAlwaysReturned() throws SQLException {
			return m.generatedKeyAlwaysReturned();
		}
	}

	private class OTRSMeta extends OTWrapper implements ResultSetMetaData {
		private final ResultSetMetaData m;

		OTRSMeta(ResultSetMetaData metaData) {
			super(metaData);
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
}
