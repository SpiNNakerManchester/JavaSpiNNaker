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

/**
 * A connection that guarantees to not throw {@link SQLException} from many of
 * its methods. Such exceptions are wrapped as {@link DataAccessException}s
 * instead.
 *
 * @author Donal Fellows
 */
class UncheckedConnection implements Connection {
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
		try {
			realDB.exec("begin " + mode.name() + ";", false);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
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
		try {
			realDB.exec("commit;", false);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
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
		try {
			realDB.exec("rollback;", false);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final <T> T unwrap(Class<T> iface) {
		try {
			return c.unwrap(iface);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final boolean isWrapperFor(Class<?> iface) {
		try {
			return c.isWrapperFor(iface);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final Statement createStatement() {
		try {
			return c.createStatement();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final PreparedStatement prepareStatement(String sql) {
		try {
			return c.prepareStatement(sql);
		} catch (SQLException e) {
			throw mapException(e, sql);
		}
	}

	@Override
	public final CallableStatement prepareCall(String sql) {
		try {
			return c.prepareCall(sql);
		} catch (SQLException e) {
			throw mapException(e, sql);
		}
	}

	@Override
	public final String nativeSQL(String sql) {
		try {
			return c.nativeSQL(sql);
		} catch (SQLException e) {
			throw mapException(e, sql);
		}
	}

	@Override
	public final void setAutoCommit(boolean autoCommit) {
		try {
			c.setAutoCommit(autoCommit);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final boolean getAutoCommit() {
		try {
			return c.getAutoCommit();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final void commit() {
		try {
			c.commit();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final void rollback() {
		try {
			c.rollback();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final void close() {
		try {
			c.close();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final boolean isClosed() {
		try {
			return c.isClosed();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final DatabaseMetaData getMetaData() {
		try {
			return c.getMetaData();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final void setReadOnly(boolean readOnly) {
		try {
			c.setReadOnly(readOnly);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final boolean isReadOnly() {
		try {
			return c.isReadOnly();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final void setCatalog(String catalog) {
		try {
			c.setCatalog(catalog);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final String getCatalog() {
		try {
			return c.getCatalog();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final void setTransactionIsolation(int level) {
		try {
			c.setTransactionIsolation(level);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final int getTransactionIsolation() {
		try {
			return c.getTransactionIsolation();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final SQLWarning getWarnings() {
		try {
			return c.getWarnings();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final void clearWarnings() {
		try {
			c.clearWarnings();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final Statement createStatement(int resultSetType,
			int resultSetConcurrency) {
		try {
			return c.createStatement(resultSetType, resultSetConcurrency);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			int resultSetType, int resultSetConcurrency) {
		try {
			return c.prepareStatement(sql, resultSetType, resultSetConcurrency);
		} catch (SQLException e) {
			throw mapException(e, sql);
		}
	}

	@Override
	public final CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency) {
		try {
			return c.prepareCall(sql, resultSetType, resultSetConcurrency);
		} catch (SQLException e) {
			throw mapException(e, sql);
		}
	}

	@Override
	public final Map<String, Class<?>> getTypeMap() throws SQLException {
		return c.getTypeMap();
	}

	@Override
	public final void setTypeMap(Map<String, Class<?>> map) {
		try {
			c.setTypeMap(map);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final void setHoldability(int holdability) {
		try {
			c.setHoldability(holdability);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final int getHoldability() {
		try {
			return c.getHoldability();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final Savepoint setSavepoint() {
		try {
			return c.setSavepoint();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final Savepoint setSavepoint(String name) {
		try {
			return c.setSavepoint(name);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final void rollback(Savepoint savepoint) {
		try {
			c.rollback(savepoint);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final void releaseSavepoint(Savepoint savepoint) {
		try {
			c.releaseSavepoint(savepoint);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final Statement createStatement(int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) {
		try {
			return c.createStatement(resultSetType, resultSetConcurrency,
					resultSetHoldability);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) {
		try {
			return c.prepareStatement(sql, resultSetType, resultSetConcurrency,
					resultSetHoldability);
		} catch (SQLException e) {
			throw mapException(e, sql);
		}
	}

	@Override
	public final CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) {
		try {
			return c.prepareCall(sql, resultSetType, resultSetConcurrency,
					resultSetHoldability);
		} catch (SQLException e) {
			throw mapException(e, sql);
		}
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			int autoGeneratedKeys) {
		try {
			return c.prepareStatement(sql, autoGeneratedKeys);
		} catch (SQLException e) {
			throw mapException(e, sql);
		}
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			int[] columnIndexes) {
		try {
			return c.prepareStatement(sql, columnIndexes);
		} catch (SQLException e) {
			throw mapException(e, sql);
		}
	}

	@Override
	public final PreparedStatement prepareStatement(String sql,
			String[] columnNames) {
		try {
			return c.prepareStatement(sql, columnNames);
		} catch (SQLException e) {
			throw mapException(e, sql);
		}
	}

	@Override
	public final Clob createClob() {
		try {
			return c.createClob();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final Blob createBlob() {
		try {
			return c.createBlob();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final NClob createNClob() {
		try {
			return c.createNClob();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final SQLXML createSQLXML() {
		try {
			return c.createSQLXML();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final boolean isValid(int timeout) {
		try {
			return c.isValid(timeout);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
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
		try {
			return c.getClientInfo(name);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final Properties getClientInfo() {
		try {
			return c.getClientInfo();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final Array createArrayOf(String typeName, Object[] elements) {
		try {
			return c.createArrayOf(typeName, elements);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final Struct createStruct(String typeName, Object[] attributes) {
		try {
			return c.createStruct(typeName, attributes);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final void setSchema(String schema) {
		try {
			c.setSchema(schema);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final String getSchema() {
		try {
			return c.getSchema();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final void abort(Executor executor) {
		try {
			c.abort(executor);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final void setNetworkTimeout(Executor executor, int milliseconds) {
		try {
			c.setNetworkTimeout(executor, milliseconds);
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}

	@Override
	public final int getNetworkTimeout() {
		try {
			return c.getNetworkTimeout();
		} catch (SQLException e) {
			throw mapException(e, null);
		}
	}
}
