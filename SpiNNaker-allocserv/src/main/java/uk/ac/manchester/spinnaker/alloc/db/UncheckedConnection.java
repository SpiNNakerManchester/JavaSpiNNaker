/*
 * Copyright (c) 2021-2023 The University of Manchester
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

import static uk.ac.manchester.spinnaker.alloc.db.Utils.mapException;

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

import org.springframework.dao.DataAccessException;
import org.sqlite.SQLiteConfig.TransactionMode;
import org.sqlite.SQLiteConnection;
import org.sqlite.core.DB;

import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * A connection that guarantees to not throw {@link SQLException} from many of
 * its methods. Such exceptions are wrapped as {@link DataAccessException}s
 * instead.
 *
 * @author Donal Fellows
 */
@UsedInJavadocOnly(DataAccessException.class)
public class UncheckedConnection implements Connection {
	private final Connection c;

	private final DB realDB;

	UncheckedConnection(Connection c) {
		this.c = c;
		if (isWrapperFor(SQLiteConnection.class)) {
			realDB = unwrap(SQLiteConnection.class).getDatabase();
		} else {
			realDB = null;
		}
	}

	/**
	 * Produce a value or throw. Only used in {@link UncheckedConnection}.
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
			throw mapException(e, null);
		}
	}

	/**
	 * Handles exception mapping if an exception is thrown.
	 *
	 * @param <T>
	 *            The type of the result.
	 * @param sql
	 *            The SQL being handled. Not {@code null}.
	 * @param getter
	 *            How to get the result. May throw.
	 * @return The result.
	 * @throws DataAccessException
	 *             If the interior code throws an {@link SQLException}.
	 */
	private <T> T get(String sql, Getter<T> getter) {
		try {
			return getter.get();
		} catch (SQLException e) {
			throw mapException(e, sql);
		}
	}

	/**
	 * Take an action or throw. Only used in {@link UncheckedConnection}.
	 */
	private interface Acter {
		/**
		 * Take an action or throw.
		 *
		 * @throws SQLException
		 *             If things fail.
		 */
		void act() throws SQLException;
	}

	/**
	 * Handles exception mapping if an exception is thrown.
	 *
	 * @param acter
	 *            How to take the action. May throw.
	 * @throws DataAccessException
	 *             If the interior code throws an {@link SQLException}.
	 */
	private void act(Acter acter) {
		try {
			acter.act();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	/**
	 * A real database {@code BEGIN} in SQLite. Can't use
	 * {@link #setAutoCommit(boolean)} as that maintains transactions when it
	 * doesn't need to, eventually causing database locking problems.
	 *
	 * @param mode
	 *            Which transaction mode to use
	 * @throws DataAccessException
	 *             If the {@code BEGIN} fails.
	 */
	final void realBegin(TransactionMode mode) {
		act(() -> realDB.exec("begin " + mode.name() + ";", false));
	}

	/**
	 * A real database {@code COMMIT} in SQLite. Can't use {@link #commit()} as
	 * that maintains transactions when it doesn't need to, eventually causing
	 * database locking problems.
	 *
	 * @throws DataAccessException
	 *             If the {@code COMMIT} fails.
	 */
	final void realCommit() {
		act(() -> realDB.exec("commit;", false));
	}

	/**
	 * A real database {@code ROLLBACK} in SQLite. Can't use {@link #rollback()}
	 * as that maintains transactions when it doesn't need to, eventually
	 * causing database locking problems.
	 *
	 * @throws DataAccessException
	 *             If the {@code ROLLBACK} fails.
	 */
	final void realRollback() {
		act(() -> realDB.exec("rollback;", false));
	}

	@Override
	public final <T> T unwrap(Class<T> iface) {
		return get(() -> c.unwrap(iface));
	}

	@Override
	public final boolean isWrapperFor(Class<?> iface) {
		return get(() -> c.isWrapperFor(iface));
	}

	@Override
	public final Statement createStatement() {
		return get(() -> c.createStatement());
	}

	@Override
	public final PreparedStatement prepareStatement(String sql) {
		return get(sql, () -> c.prepareStatement(sql));
	}

	@Override
	public final CallableStatement prepareCall(String sql) {
		return get(sql, () -> c.prepareCall(sql));
	}

	@Override
	public final String nativeSQL(String sql) {
		return get(sql, () -> c.nativeSQL(sql));
	}

	@Override
	public final void setAutoCommit(boolean autoCommit) {
		act(() -> c.setAutoCommit(autoCommit));
	}

	@Override
	public final boolean getAutoCommit() {
		return get(() -> c.getAutoCommit());
	}

	@Override
	public final void commit() {
		act(() -> c.commit());
	}

	@Override
	public final void rollback() {
		act(() -> c.rollback());
	}

	@Override
	public final void close() {
		act(() -> c.close());
	}

	@Override
	public final boolean isClosed() {
		return get(() -> c.isClosed());
	}

	@Override
	public final DatabaseMetaData getMetaData() {
		return get(() -> c.getMetaData());
	}

	@Override
	public final void setReadOnly(boolean readOnly) {
		act(() -> c.setReadOnly(readOnly));
	}

	@Override
	public final boolean isReadOnly() {
		return get(() -> c.isReadOnly());
	}

	@Override
	public final void setCatalog(String catalog) {
		act(() -> c.setCatalog(catalog));
	}

	@Override
	public final String getCatalog() {
		return get(() -> c.getCatalog());
	}

	@Override
	public final void setTransactionIsolation(int level) {
		act(() -> c.setTransactionIsolation(level));
	}

	@Override
	public final int getTransactionIsolation() {
		return get(() -> c.getTransactionIsolation());
	}

	@Override
	public final SQLWarning getWarnings() {
		return get(() -> c.getWarnings());
	}

	@Override
	public final void clearWarnings() {
		act(() -> c.clearWarnings());
	}

	@Override
	public final Statement createStatement(int resultSetType,
			int resultSetConcurrency) {
		return get(
				() -> c.createStatement(resultSetType, resultSetConcurrency));
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			int resultSetType, int resultSetConcurrency) {
		return get(sql, () -> c.prepareStatement(sql, resultSetType,
				resultSetConcurrency));
	}

	@Override
	public final CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency) {
		return get(sql,
				() -> c.prepareCall(sql, resultSetType, resultSetConcurrency));
	}

	@Override
	public final Map<String, Class<?>> getTypeMap() {
		return get(() -> c.getTypeMap());
	}

	@Override
	public final void setTypeMap(Map<String, Class<?>> map) {
		act(() -> c.setTypeMap(map));
	}

	@Override
	public final void setHoldability(int holdability) {
		act(() -> c.setHoldability(holdability));
	}

	@Override
	public final int getHoldability() {
		return get(() -> c.getHoldability());
	}

	@Override
	public final Savepoint setSavepoint() {
		return get(() -> c.setSavepoint());
	}

	@Override
	public final Savepoint setSavepoint(String name) {
		return get(() -> c.setSavepoint(name));
	}

	@Override
	public final void rollback(Savepoint savepoint) {
		act(() -> c.rollback(savepoint));
	}

	@Override
	public final void releaseSavepoint(Savepoint savepoint) {
		act(() -> c.releaseSavepoint(savepoint));
	}

	@Override
	public final Statement createStatement(int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) {
		return get(() -> c.createStatement(resultSetType, resultSetConcurrency,
				resultSetHoldability));
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) {
		return get(sql, () -> c.prepareStatement(sql, resultSetType,
				resultSetConcurrency, resultSetHoldability));
	}

	@Override
	public final CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) {
		return get(sql, () -> c.prepareCall(sql, resultSetType,
				resultSetConcurrency, resultSetHoldability));
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			int autoGeneratedKeys) {
		return get(sql, () -> c.prepareStatement(sql, autoGeneratedKeys));
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			int[] columnIndexes) {
		return get(sql, () -> c.prepareStatement(sql, columnIndexes));
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			String[] columnNames) {
		return get(sql, () -> c.prepareStatement(sql, columnNames));
	}

	@Override
	public final Clob createClob() {
		return get(() -> c.createClob());
	}

	@Override
	public final Blob createBlob() {
		return get(() -> c.createBlob());
	}

	@Override
	public final NClob createNClob() {
		return get(() -> c.createNClob());
	}

	@Override
	public final SQLXML createSQLXML() {
		return get(() -> c.createSQLXML());
	}

	@Override
	public final boolean isValid(int timeout) {
		return get(() -> c.isValid(timeout));
	}

	@Override
	public final void setClientInfo(String name, String value)
			throws SQLClientInfoException {
		c.setClientInfo(name, value);
	}

	@Override
	public final void setClientInfo(Properties properties)
			throws SQLClientInfoException {
		c.setClientInfo(properties);
	}

	@Override
	public final String getClientInfo(String name) {
		return get(() -> c.getClientInfo(name));
	}

	@Override
	public final Properties getClientInfo() {
		return get(() -> c.getClientInfo());
	}

	@Override
	public final Array createArrayOf(String typeName, Object[] elements) {
		return get(() -> c.createArrayOf(typeName, elements));
	}

	@Override
	public final Struct createStruct(String typeName, Object[] attributes) {
		return get(() -> c.createStruct(typeName, attributes));
	}

	@Override
	public final void setSchema(String schema) {
		act(() -> c.setSchema(schema));
	}

	@Override
	public final String getSchema() {
		return get(() -> c.getSchema());
	}

	@Override
	public final void abort(Executor executor) {
		act(() -> c.abort(executor));
	}

	@Override
	public final void setNetworkTimeout(Executor executor, int milliseconds) {
		act(() -> c.setNetworkTimeout(executor, milliseconds));
	}

	@Override
	public final int getNetworkTimeout() {
		return get(() -> c.getNetworkTimeout());
	}
}
