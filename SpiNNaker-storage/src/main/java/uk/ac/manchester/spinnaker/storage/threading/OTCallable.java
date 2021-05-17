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
 * GNU General public final License for more details.
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
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * A single-threaded database callable statement wrapper.
 *
 * @author Donal Fellows
 */
class OTCallable extends OTPrepared implements CallableStatement {
	private final CallableStatement s;

	OTCallable(OneThread ot, CallableStatement s) {
		super(ot, s);
		this.s = s;
	}

	@Override
	public final void registerOutParameter(int parameterIndex, int sqlType)
			throws SQLException {
		validateThread();
		s.registerOutParameter(parameterIndex, sqlType);
	}

	@Override
	public final void registerOutParameter(int parameterIndex, int sqlType,
			int scale) throws SQLException {
		validateThread();
		s.registerOutParameter(parameterIndex, sqlType, scale);
	}

	@Override
	public final boolean wasNull() throws SQLException {
		validateThread();
		return s.wasNull();
	}

	@Override
	public final String getString(int parameterIndex) throws SQLException {
		validateThread();
		return s.getString(parameterIndex);
	}

	@Override
	public final boolean getBoolean(int parameterIndex) throws SQLException {
		validateThread();
		return s.getBoolean(parameterIndex);
	}

	@Override
	public final byte getByte(int parameterIndex) throws SQLException {
		validateThread();
		return s.getByte(parameterIndex);
	}

	@Override
	public final short getShort(int parameterIndex) throws SQLException {
		validateThread();
		return s.getShort(parameterIndex);
	}

	@Override
	public final int getInt(int parameterIndex) throws SQLException {
		validateThread();
		return s.getInt(parameterIndex);
	}

	@Override
	public final long getLong(int parameterIndex) throws SQLException {
		validateThread();
		return s.getLong(parameterIndex);
	}

	@Override
	public final float getFloat(int parameterIndex) throws SQLException {
		validateThread();
		return s.getFloat(parameterIndex);
	}

	@Override
	public final double getDouble(int parameterIndex) throws SQLException {
		validateThread();
		return s.getDouble(parameterIndex);
	}

	@Override
	@Deprecated
	public final BigDecimal getBigDecimal(int parameterIndex, int scale)
			throws SQLException {
		validateThread();
		return s.getBigDecimal(parameterIndex, scale);
	}

	@Override
	public final byte[] getBytes(int parameterIndex) throws SQLException {
		validateThread();
		return s.getBytes(parameterIndex);
	}

	@Override
	public final Date getDate(int parameterIndex) throws SQLException {
		validateThread();
		return s.getDate(parameterIndex);
	}

	@Override
	public final Time getTime(int parameterIndex) throws SQLException {
		validateThread();
		return s.getTime(parameterIndex);
	}

	@Override
	public final Timestamp getTimestamp(int parameterIndex)
			throws SQLException {
		validateThread();
		return s.getTimestamp(parameterIndex);
	}

	@Override
	public final Object getObject(int parameterIndex) throws SQLException {
		validateThread();
		return s.getObject(parameterIndex);
	}

	@Override
	public final BigDecimal getBigDecimal(int parameterIndex)
			throws SQLException {
		validateThread();
		return s.getBigDecimal(parameterIndex);
	}

	@Override
	public final Object getObject(int parameterIndex, Map<String, Class<?>> map)
			throws SQLException {
		validateThread();
		return s.getObject(parameterIndex, map);
	}

	@Override
	public final Ref getRef(int parameterIndex) throws SQLException {
		validateThread();
		return s.getRef(parameterIndex);
	}

	@Override
	public final Blob getBlob(int parameterIndex) throws SQLException {
		validateThread();
		return s.getBlob(parameterIndex);
	}

	@Override
	public final Clob getClob(int parameterIndex) throws SQLException {
		validateThread();
		return s.getClob(parameterIndex);
	}

	@Override
	public final Array getArray(int parameterIndex) throws SQLException {
		validateThread();
		return s.getArray(parameterIndex);
	}

	@Override
	public final Date getDate(int parameterIndex, Calendar cal)
			throws SQLException {
		validateThread();
		return s.getDate(parameterIndex, cal);
	}

	@Override
	public final Time getTime(int parameterIndex, Calendar cal)
			throws SQLException {
		validateThread();
		return s.getTime(parameterIndex, cal);
	}

	@Override
	public final Timestamp getTimestamp(int parameterIndex, Calendar cal)
			throws SQLException {
		validateThread();
		return s.getTimestamp(parameterIndex, cal);
	}

	@Override
	public final void registerOutParameter(int parameterIndex, int sqlType,
			String typeName) throws SQLException {
		validateThread();
		s.registerOutParameter(parameterIndex, sqlType, typeName);
	}

	@Override
	public final void registerOutParameter(String parameterName, int sqlType)
			throws SQLException {
		validateThread();
		s.registerOutParameter(parameterName, sqlType);
	}

	@Override
	public final void registerOutParameter(String parameterName, int sqlType,
			int scale) throws SQLException {
		validateThread();
		s.registerOutParameter(parameterName, sqlType, scale);
	}

	@Override
	public final void registerOutParameter(String parameterName, int sqlType,
			String typeName) throws SQLException {
		validateThread();
		s.registerOutParameter(parameterName, sqlType, typeName);
	}

	@Override
	public final URL getURL(int parameterIndex) throws SQLException {
		validateThread();
		return s.getURL(parameterIndex);
	}

	@Override
	public final void setURL(String parameterName, URL val)
			throws SQLException {
		validateThread();
		s.setURL(parameterName, val);
	}

	@Override
	public final void setNull(String parameterName, int sqlType)
			throws SQLException {
		validateThread();
		s.setNull(parameterName, sqlType);
	}

	@Override
	public final void setBoolean(String parameterName, boolean x)
			throws SQLException {
		validateThread();
		s.setBoolean(parameterName, x);
	}

	@Override
	public final void setByte(String parameterName, byte x)
			throws SQLException {
		validateThread();
		s.setByte(parameterName, x);
	}

	@Override
	public final void setShort(String parameterName, short x)
			throws SQLException {
		validateThread();
		s.setShort(parameterName, x);
	}

	@Override
	public final void setInt(String parameterName, int x) throws SQLException {
		validateThread();
		s.setInt(parameterName, x);
	}

	@Override
	public final void setLong(String parameterName, long x)
			throws SQLException {
		validateThread();
		s.setLong(parameterName, x);
	}

	@Override
	public final void setFloat(String parameterName, float x)
			throws SQLException {
		validateThread();
		s.setFloat(parameterName, x);
	}

	@Override
	public final void setDouble(String parameterName, double x)
			throws SQLException {
		validateThread();
		s.setDouble(parameterName, x);
	}

	@Override
	public final void setBigDecimal(String parameterName, BigDecimal x)
			throws SQLException {
		validateThread();
		s.setBigDecimal(parameterName, x);
	}

	@Override
	public final void setString(String parameterName, String x)
			throws SQLException {
		validateThread();
		s.setString(parameterName, x);
	}

	@Override
	public final void setBytes(String parameterName, byte[] x)
			throws SQLException {
		validateThread();
		s.setBytes(parameterName, x);
	}

	@Override
	public final void setDate(String parameterName, Date x)
			throws SQLException {
		validateThread();
		s.setDate(parameterName, x);
	}

	@Override
	public final void setTime(String parameterName, Time x)
			throws SQLException {
		validateThread();
		s.setTime(parameterName, x);
	}

	@Override
	public final void setTimestamp(String parameterName, Timestamp x)
			throws SQLException {
		validateThread();
		s.setTimestamp(parameterName, x);
	}

	@Override
	public final void setAsciiStream(String parameterName, InputStream x,
			int length) throws SQLException {
		validateThread();
		s.setAsciiStream(parameterName, x, length);
	}

	@Override
	public final void setBinaryStream(String parameterName, InputStream x,
			int length) throws SQLException {
		validateThread();
		s.setBinaryStream(parameterName, x, length);
	}

	@Override
	public final void setObject(String parameterName, Object x,
			int targetSqlType, int scale) throws SQLException {
		validateThread();
		s.setObject(parameterName, x, targetSqlType, scale);
	}

	@Override
	public final void setObject(String parameterName, Object x,
			int targetSqlType) throws SQLException {
		validateThread();
		s.setObject(parameterName, x, targetSqlType);
	}

	@Override
	public final void setObject(String parameterName, Object x)
			throws SQLException {
		validateThread();
		s.setObject(parameterName, x);
	}

	@Override
	public final void setCharacterStream(String parameterName, Reader reader,
			int length) throws SQLException {
		validateThread();
		s.setCharacterStream(parameterName, reader, length);
	}

	@Override
	public final void setDate(String parameterName, Date x, Calendar cal)
			throws SQLException {
		validateThread();
		s.setDate(parameterName, x, cal);
	}

	@Override
	public final void setTime(String parameterName, Time x, Calendar cal)
			throws SQLException {
		validateThread();
		s.setTime(parameterName, x, cal);
	}

	@Override
	public final void setTimestamp(String parameterName, Timestamp x,
			Calendar cal) throws SQLException {
		validateThread();
		s.setTimestamp(parameterName, x, cal);
	}

	@Override
	public final void setNull(String parameterName, int sqlType,
			String typeName) throws SQLException {
		validateThread();
		s.setNull(parameterName, sqlType, typeName);
	}

	@Override
	public final String getString(String parameterName) throws SQLException {
		validateThread();
		return s.getString(parameterName);
	}

	@Override
	public final boolean getBoolean(String parameterName) throws SQLException {
		validateThread();
		return s.getBoolean(parameterName);
	}

	@Override
	public final byte getByte(String parameterName) throws SQLException {
		validateThread();
		return s.getByte(parameterName);
	}

	@Override
	public final short getShort(String parameterName) throws SQLException {
		validateThread();
		return s.getShort(parameterName);
	}

	@Override
	public final int getInt(String parameterName) throws SQLException {
		validateThread();
		return s.getInt(parameterName);
	}

	@Override
	public final long getLong(String parameterName) throws SQLException {
		validateThread();
		return s.getLong(parameterName);
	}

	@Override
	public final float getFloat(String parameterName) throws SQLException {
		validateThread();
		return s.getFloat(parameterName);
	}

	@Override
	public final double getDouble(String parameterName) throws SQLException {
		validateThread();
		return s.getDouble(parameterName);
	}

	@Override
	public final byte[] getBytes(String parameterName) throws SQLException {
		validateThread();
		return s.getBytes(parameterName);
	}

	@Override
	public final Date getDate(String parameterName) throws SQLException {
		validateThread();
		return s.getDate(parameterName);
	}

	@Override
	public final Time getTime(String parameterName) throws SQLException {
		validateThread();
		return s.getTime(parameterName);
	}

	@Override
	public final Timestamp getTimestamp(String parameterName)
			throws SQLException {
		validateThread();
		return s.getTimestamp(parameterName);
	}

	@Override
	public final Object getObject(String parameterName) throws SQLException {
		validateThread();
		return s.getObject(parameterName);
	}

	@Override
	public final BigDecimal getBigDecimal(String parameterName)
			throws SQLException {
		validateThread();
		return s.getBigDecimal(parameterName);
	}

	@Override
	public final Object getObject(String parameterName,
			Map<String, Class<?>> map) throws SQLException {
		validateThread();
		return s.getObject(parameterName, map);
	}

	@Override
	public final Ref getRef(String parameterName) throws SQLException {
		validateThread();
		return s.getRef(parameterName);
	}

	@Override
	public final Blob getBlob(String parameterName) throws SQLException {
		validateThread();
		return s.getBlob(parameterName);
	}

	@Override
	public final Clob getClob(String parameterName) throws SQLException {
		validateThread();
		return s.getClob(parameterName);
	}

	@Override
	public final Array getArray(String parameterName) throws SQLException {
		validateThread();
		return s.getArray(parameterName);
	}

	@Override
	public final Date getDate(String parameterName, Calendar cal)
			throws SQLException {
		validateThread();
		return s.getDate(parameterName, cal);
	}

	@Override
	public final Time getTime(String parameterName, Calendar cal)
			throws SQLException {
		validateThread();
		return s.getTime(parameterName, cal);
	}

	@Override
	public final Timestamp getTimestamp(String parameterName, Calendar cal)
			throws SQLException {
		validateThread();
		return s.getTimestamp(parameterName, cal);
	}

	@Override
	public final URL getURL(String parameterName) throws SQLException {
		validateThread();
		return s.getURL(parameterName);
	}

	@Override
	public final RowId getRowId(int parameterIndex) throws SQLException {
		validateThread();
		return s.getRowId(parameterIndex);
	}

	@Override
	public final RowId getRowId(String parameterName) throws SQLException {
		validateThread();
		return s.getRowId(parameterName);
	}

	@Override
	public final void setRowId(String parameterName, RowId x)
			throws SQLException {
		validateThread();
		s.setRowId(parameterName, x);
	}

	@Override
	public final void setNString(String parameterName, String value)
			throws SQLException {
		validateThread();
		s.setNString(parameterName, value);
	}

	@Override
	public final void setNCharacterStream(String parameterName, Reader value,
			long length) throws SQLException {
		validateThread();
		s.setNCharacterStream(parameterName, value, length);
	}

	@Override
	public final void setNClob(String parameterName, NClob value)
			throws SQLException {
		validateThread();
		s.setNClob(parameterName, value);
	}

	@Override
	public final void setClob(String parameterName, Reader reader, long length)
			throws SQLException {
		validateThread();
		s.setNClob(parameterName, reader, length);
	}

	@Override
	public final void setBlob(String parameterName, InputStream inputStream,
			long length) throws SQLException {
		validateThread();
		s.setBlob(parameterName, inputStream, length);
	}

	@Override
	public final void setNClob(String parameterName, Reader reader, long length)
			throws SQLException {
		validateThread();
		s.setNClob(parameterName, reader, length);
	}

	@Override
	public final NClob getNClob(int parameterIndex) throws SQLException {
		validateThread();
		return s.getNClob(parameterIndex);
	}

	@Override
	public final NClob getNClob(String parameterName) throws SQLException {
		validateThread();
		return s.getNClob(parameterName);
	}

	@Override
	public final void setSQLXML(String parameterName, SQLXML xmlObject)
			throws SQLException {
		validateThread();
		s.setSQLXML(parameterName, xmlObject);
	}

	@Override
	public final SQLXML getSQLXML(int parameterIndex) throws SQLException {
		validateThread();
		return s.getSQLXML(parameterIndex);
	}

	@Override
	public final SQLXML getSQLXML(String parameterName) throws SQLException {
		validateThread();
		return s.getSQLXML(parameterName);
	}

	@Override
	public final String getNString(int parameterIndex) throws SQLException {
		validateThread();
		return s.getNString(parameterIndex);
	}

	@Override
	public final String getNString(String parameterName) throws SQLException {
		validateThread();
		return s.getNString(parameterName);
	}

	@Override
	public final Reader getNCharacterStream(int parameterIndex)
			throws SQLException {
		validateThread();
		return s.getNCharacterStream(parameterIndex);
	}

	@Override
	public final Reader getNCharacterStream(String parameterName)
			throws SQLException {
		validateThread();
		return s.getNCharacterStream(parameterName);
	}

	@Override
	public final Reader getCharacterStream(int parameterIndex)
			throws SQLException {
		validateThread();
		return s.getCharacterStream(parameterIndex);
	}

	@Override
	public final Reader getCharacterStream(String parameterName)
			throws SQLException {
		validateThread();
		return s.getCharacterStream(parameterName);
	}

	@Override
	public final void setBlob(String parameterName, Blob x)
			throws SQLException {
		validateThread();
		s.setBlob(parameterName, x);
	}

	@Override
	public final void setClob(String parameterName, Clob x)
			throws SQLException {
		validateThread();
		s.setClob(parameterName, x);
	}

	@Override
	public final void setAsciiStream(String parameterName, InputStream x,
			long length) throws SQLException {
		validateThread();
		s.setAsciiStream(parameterName, x, length);
	}

	@Override
	public final void setBinaryStream(String parameterName, InputStream x,
			long length) throws SQLException {
		validateThread();
		s.setBinaryStream(parameterName, x, length);
	}

	@Override
	public final void setCharacterStream(String parameterName, Reader reader,
			long length) throws SQLException {
		validateThread();
		s.setCharacterStream(parameterName, reader, length);
	}

	@Override
	public final void setAsciiStream(String parameterName, InputStream x)
			throws SQLException {
		validateThread();
		s.setAsciiStream(parameterName, x);
	}

	@Override
	public final void setBinaryStream(String parameterName, InputStream x)
			throws SQLException {
		validateThread();
		s.setBinaryStream(parameterName, x);
	}

	@Override
	public final void setCharacterStream(String parameterName, Reader reader)
			throws SQLException {
		validateThread();
		s.setCharacterStream(parameterName, reader);
	}

	@Override
	public final void setNCharacterStream(String parameterName, Reader value)
			throws SQLException {
		validateThread();
		s.setNCharacterStream(parameterName, value);
	}

	@Override
	public final void setClob(String parameterName, Reader reader)
			throws SQLException {
		validateThread();
		s.setClob(parameterName, reader);
	}

	@Override
	public final void setBlob(String parameterName, InputStream inputStream)
			throws SQLException {
		validateThread();
		s.setBlob(parameterName, inputStream);
	}

	@Override
	public final void setNClob(String parameterName, Reader reader)
			throws SQLException {
		validateThread();
		s.setNClob(parameterName, reader);
	}

	@Override
	public final <T> T getObject(int parameterIndex, Class<T> type)
			throws SQLException {
		validateThread();
		return s.getObject(parameterIndex, type);
	}

	@Override
	public final <T> T getObject(String parameterName, Class<T> type)
			throws SQLException {
		validateThread();
		return s.getObject(parameterName, type);
	}
}
