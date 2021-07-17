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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * A single-threaded database prepared statement wrapper.
 *
 * @author Donal Fellows
 */
class OTPrepared extends OTStatement implements PreparedStatement {
	private final PreparedStatement s;

	OTPrepared(OneThread ot, PreparedStatement s) {
		super(ot, s);
		this.s = s;
	}

	@Override
	public final ResultSet executeQuery() throws SQLException {
		validateThread();
		return wrap(this, s.executeQuery());
	}

	@Override
	public final int executeUpdate() throws SQLException {
		validateThread();
		return s.executeUpdate();
	}

	@Override
	public final void setNull(int parameterIndex, int sqlType)
			throws SQLException {
		validateThread();
		s.setNull(parameterIndex, sqlType);
	}

	@Override
	public final void setBoolean(int parameterIndex, boolean x)
			throws SQLException {
		validateThread();
		s.setBoolean(parameterIndex, x);
	}

	@Override
	public final void setByte(int parameterIndex, byte x) throws SQLException {
		validateThread();
		s.setByte(parameterIndex, x);
	}

	@Override
	public final void setShort(int parameterIndex, short x)
			throws SQLException {
		validateThread();
		s.setShort(parameterIndex, x);
	}

	@Override
	public final void setInt(int parameterIndex, int x) throws SQLException {
		validateThread();
		s.setInt(parameterIndex, x);
	}

	@Override
	public final void setLong(int parameterIndex, long x) throws SQLException {
		validateThread();
		s.setLong(parameterIndex, x);
	}

	@Override
	public final void setFloat(int parameterIndex, float x)
			throws SQLException {
		validateThread();
		s.setFloat(parameterIndex, x);
	}

	@Override
	public final void setDouble(int parameterIndex, double x)
			throws SQLException {
		validateThread();
		s.setDouble(parameterIndex, x);
	}

	@Override
	public final void setBigDecimal(int parameterIndex, BigDecimal x)
			throws SQLException {
		validateThread();
		s.setBigDecimal(parameterIndex, x);
	}

	@Override
	public final void setString(int parameterIndex, String x)
			throws SQLException {
		validateThread();
		s.setString(parameterIndex, x);
	}

	@Override
	public final void setBytes(int parameterIndex, byte[] x)
			throws SQLException {
		validateThread();
		s.setBytes(parameterIndex, x);
	}

	@Override
	public final void setDate(int parameterIndex, Date x) throws SQLException {
		validateThread();
		s.setDate(parameterIndex, x);
	}

	@Override
	public final void setTime(int parameterIndex, Time x) throws SQLException {
		validateThread();
		s.setTime(parameterIndex, x);
	}

	@Override
	public final void setTimestamp(int parameterIndex, Timestamp x)
			throws SQLException {
		validateThread();
		s.setTimestamp(parameterIndex, x);
	}

	@Override
	public final void setAsciiStream(int parameterIndex, InputStream x,
			int length) throws SQLException {
		validateThread();
		s.setAsciiStream(parameterIndex, x, length);
	}

	@Override
	@Deprecated
	public final void setUnicodeStream(int parameterIndex, InputStream x,
			int length) throws SQLException {
		validateThread();
		s.setUnicodeStream(parameterIndex, x, length);
	}

	@Override
	public final void setBinaryStream(int parameterIndex, InputStream x,
			int length) throws SQLException {
		validateThread();
		s.setBinaryStream(parameterIndex, x, length);
	}

	@Override
	public final void clearParameters() throws SQLException {
		validateThread();
		s.clearParameters();
	}

	@Override
	public final void setObject(int parameterIndex, Object x, int targetSqlType)
			throws SQLException {
		validateThread();
		s.setObject(parameterIndex, x, targetSqlType);
	}

	@Override
	public final void setObject(int parameterIndex, Object x)
			throws SQLException {
		validateThread();
		s.setObject(parameterIndex, x);
	}

	@Override
	public final boolean execute() throws SQLException {
		validateThread();
		return s.execute();
	}

	@Override
	public final void addBatch() throws SQLException {
		validateThread();
		s.addBatch();
	}

	@Override
	public final void setCharacterStream(int parameterIndex, Reader reader,
			int length) throws SQLException {
		validateThread();
		s.setCharacterStream(parameterIndex, reader, length);
	}

	@Override
	public final void setRef(int parameterIndex, Ref x) throws SQLException {
		validateThread();
		s.setRef(parameterIndex, x);
	}

	@Override
	public final void setBlob(int parameterIndex, Blob x) throws SQLException {
		validateThread();
		s.setBlob(parameterIndex, x);
	}

	@Override
	public final void setClob(int parameterIndex, Clob x) throws SQLException {
		validateThread();
		s.setClob(parameterIndex, x);
	}

	@Override
	public final void setArray(int parameterIndex, Array x)
			throws SQLException {
		validateThread();
		s.setArray(parameterIndex, x);
	}

	@Override
	public final ResultSetMetaData getMetaData() throws SQLException {
		validateThread();
		return s.getMetaData();
	}

	@Override
	public final void setDate(int parameterIndex, Date x, Calendar cal)
			throws SQLException {
		validateThread();
		s.setDate(parameterIndex, x, cal);
	}

	@Override
	public final void setTime(int parameterIndex, Time x, Calendar cal)
			throws SQLException {
		validateThread();
		s.setTime(parameterIndex, x, cal);
	}

	@Override
	public final void setTimestamp(int parameterIndex, Timestamp x,
			Calendar cal) throws SQLException {
		validateThread();
		s.setTimestamp(parameterIndex, x, cal);
	}

	@Override
	public final void setNull(int parameterIndex, int sqlType, String typeName)
			throws SQLException {
		validateThread();
		s.setNull(parameterIndex, sqlType, typeName);
	}

	@Override
	public final void setURL(int parameterIndex, URL x) throws SQLException {
		validateThread();
		s.setURL(parameterIndex, x);
	}

	@Override
	public final ParameterMetaData getParameterMetaData() throws SQLException {
		validateThread();
		return s.getParameterMetaData();
	}

	@Override
	public final void setRowId(int parameterIndex, RowId x)
			throws SQLException {
		validateThread();
		s.setRowId(parameterIndex, x);
	}

	@Override
	public final void setNString(int parameterIndex, String value)
			throws SQLException {
		validateThread();
		s.setNString(parameterIndex, value);
	}

	@Override
	public final void setNCharacterStream(int parameterIndex, Reader value,
			long length) throws SQLException {
		validateThread();
		s.setNCharacterStream(parameterIndex, value, length);
	}

	@Override
	public final void setNClob(int parameterIndex, NClob value)
			throws SQLException {
		validateThread();
		s.setNClob(parameterIndex, value);
	}

	@Override
	public final void setClob(int parameterIndex, Reader reader, long length)
			throws SQLException {
		validateThread();
		s.setClob(parameterIndex, reader, length);
	}

	@Override
	public final void setBlob(int parameterIndex, InputStream inputStream,
			long length) throws SQLException {
		validateThread();
		s.setBlob(parameterIndex, inputStream, length);
	}

	@Override
	public final void setNClob(int parameterIndex, Reader reader, long length)
			throws SQLException {
		validateThread();
		s.setNClob(parameterIndex, reader, length);
	}

	@Override
	public final void setSQLXML(int parameterIndex, SQLXML xmlObject)
			throws SQLException {
		validateThread();
		s.setSQLXML(parameterIndex, xmlObject);
	}

	@Override
	public final void setObject(int parameterIndex, Object x, int targetSqlType,
			int scaleOrLength) throws SQLException {
		validateThread();
		s.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
	}

	@Override
	public final void setAsciiStream(int parameterIndex, InputStream x,
			long length) throws SQLException {
		validateThread();
		s.setAsciiStream(parameterIndex, x, length);
	}

	@Override
	public final void setBinaryStream(int parameterIndex, InputStream x,
			long length) throws SQLException {
		validateThread();
		s.setBinaryStream(parameterIndex, x, length);
	}

	@Override
	public final void setCharacterStream(int parameterIndex, Reader reader,
			long length) throws SQLException {
		validateThread();
		s.setCharacterStream(parameterIndex, reader, length);
	}

	@Override
	public final void setAsciiStream(int parameterIndex, InputStream x)
			throws SQLException {
		validateThread();
		s.setAsciiStream(parameterIndex, x);
	}

	@Override
	public final void setBinaryStream(int parameterIndex, InputStream x)
			throws SQLException {
		validateThread();
		s.setBinaryStream(parameterIndex, x);
	}

	@Override
	public final void setCharacterStream(int parameterIndex, Reader reader)
			throws SQLException {
		validateThread();
		s.setCharacterStream(parameterIndex, reader);
	}

	@Override
	public final void setNCharacterStream(int parameterIndex, Reader value)
			throws SQLException {
		validateThread();
		s.setNCharacterStream(parameterIndex, value);
	}

	@Override
	public final void setClob(int parameterIndex, Reader reader)
			throws SQLException {
		validateThread();
		s.setClob(parameterIndex, reader);
	}

	@Override
	public final void setBlob(int parameterIndex, InputStream inputStream)
			throws SQLException {
		validateThread();
		s.setBlob(parameterIndex, inputStream);
	}

	@Override
	public final void setNClob(int parameterIndex, Reader reader)
			throws SQLException {
		validateThread();
		s.setNClob(parameterIndex, reader);
	}
}
