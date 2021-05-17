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
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * A single-threaded database result set wrapper.
 *
 * @author Donal Fellows
 */
final class OTResults extends OTWrapper implements ResultSet {
	private final ResultSet r;
	private final Statement s;

	OTResults(OneThread ot, Statement s, ResultSet r) {
		super(ot, r);
		this.s = s;
		this.r = r;
	}

	OTResults(OneThread ot, ResultSet r) {
		super(ot, r);
		this.s = null;
		this.r = r;
	}

	@Override
	public final boolean next() throws SQLException {
		validateThread();
		return r.next();
	}

	@Override
	public final void close() throws SQLException {
		validateThread();
		r.close();
	}

	@Override
	public final boolean wasNull() throws SQLException {
		validateThread();
		return r.wasNull();
	}

	@Override
	public final String getString(int columnIndex) throws SQLException {
		validateThread();
		return r.getString(columnIndex);
	}

	@Override
	public final boolean getBoolean(int columnIndex) throws SQLException {
		validateThread();
		return r.getBoolean(columnIndex);
	}

	@Override
	public final byte getByte(int columnIndex) throws SQLException {
		validateThread();
		return r.getByte(columnIndex);
	}

	@Override
	public final short getShort(int columnIndex) throws SQLException {
		validateThread();
		return r.getShort(columnIndex);
	}

	@Override
	public final int getInt(int columnIndex) throws SQLException {
		validateThread();
		return r.getInt(columnIndex);
	}

	@Override
	public final long getLong(int columnIndex) throws SQLException {
		validateThread();
		return r.getLong(columnIndex);
	}

	@Override
	public final float getFloat(int columnIndex) throws SQLException {
		validateThread();
		return r.getFloat(columnIndex);
	}

	@Override
	public final double getDouble(int columnIndex) throws SQLException {
		validateThread();
		return r.getDouble(columnIndex);
	}

	@Override
	@Deprecated
	public final BigDecimal getBigDecimal(int columnIndex, int scale)
			throws SQLException {
		validateThread();
		return r.getBigDecimal(columnIndex, scale);
	}

	@Override
	public final byte[] getBytes(int columnIndex) throws SQLException {
		validateThread();
		return r.getBytes(columnIndex);
	}

	@Override
	public final Date getDate(int columnIndex) throws SQLException {
		validateThread();
		return r.getDate(columnIndex);
	}

	@Override
	public final Time getTime(int columnIndex) throws SQLException {
		validateThread();
		return r.getTime(columnIndex);
	}

	@Override
	public final Timestamp getTimestamp(int columnIndex) throws SQLException {
		validateThread();
		return r.getTimestamp(columnIndex);
	}

	@Override
	public final InputStream getAsciiStream(int columnIndex)
			throws SQLException {
		validateThread();
		return r.getAsciiStream(columnIndex);
	}

	@Override
	@Deprecated
	public final InputStream getUnicodeStream(int columnIndex)
			throws SQLException {
		validateThread();
		return r.getUnicodeStream(columnIndex);
	}

	@Override
	public final InputStream getBinaryStream(int columnIndex)
			throws SQLException {
		validateThread();
		return r.getBinaryStream(columnIndex);
	}

	@Override
	public final String getString(String columnLabel) throws SQLException {
		validateThread();
		return r.getString(columnLabel);
	}

	@Override
	public final boolean getBoolean(String columnLabel) throws SQLException {
		validateThread();
		return r.getBoolean(columnLabel);
	}

	@Override
	public final byte getByte(String columnLabel) throws SQLException {
		validateThread();
		return r.getByte(columnLabel);
	}

	@Override
	public final short getShort(String columnLabel) throws SQLException {
		validateThread();
		return r.getShort(columnLabel);
	}

	@Override
	public final int getInt(String columnLabel) throws SQLException {
		validateThread();
		return r.getInt(columnLabel);
	}

	@Override
	public final long getLong(String columnLabel) throws SQLException {
		validateThread();
		return r.getLong(columnLabel);
	}

	@Override
	public final float getFloat(String columnLabel) throws SQLException {
		validateThread();
		return r.getFloat(columnLabel);
	}

	@Override
	public final double getDouble(String columnLabel) throws SQLException {
		validateThread();
		return r.getDouble(columnLabel);
	}

	@Override
	@Deprecated
	public final BigDecimal getBigDecimal(String columnLabel, int scale)
			throws SQLException {
		validateThread();
		return r.getBigDecimal(columnLabel, scale);
	}

	@Override
	public final byte[] getBytes(String columnLabel) throws SQLException {
		validateThread();
		return r.getBytes(columnLabel);
	}

	@Override
	public final Date getDate(String columnLabel) throws SQLException {
		validateThread();
		return r.getDate(columnLabel);
	}

	@Override
	public final Time getTime(String columnLabel) throws SQLException {
		validateThread();
		return r.getTime(columnLabel);
	}

	@Override
	public final Timestamp getTimestamp(String columnLabel)
			throws SQLException {
		validateThread();
		return r.getTimestamp(columnLabel);
	}

	@Override
	public final InputStream getAsciiStream(String columnLabel)
			throws SQLException {
		validateThread();
		return r.getAsciiStream(columnLabel);
	}

	@Override
	@Deprecated
	public final InputStream getUnicodeStream(String columnLabel)
			throws SQLException {
		validateThread();
		return r.getUnicodeStream(columnLabel);
	}

	@Override
	public final InputStream getBinaryStream(String columnLabel)
			throws SQLException {
		validateThread();
		return r.getBinaryStream(columnLabel);
	}

	@Override
	public final SQLWarning getWarnings() throws SQLException {
		validateThread();
		return r.getWarnings();
	}

	@Override
	public final void clearWarnings() throws SQLException {
		validateThread();
		r.clearWarnings();
	}

	@Override
	public final String getCursorName() throws SQLException {
		validateThread();
		return r.getCursorName();
	}

	@Override
	public final ResultSetMetaData getMetaData() throws SQLException {
		validateThread();
		return wrap(r.getMetaData());
	}

	@Override
	public final Object getObject(int columnIndex) throws SQLException {
		validateThread();
		return r.getObject(columnIndex);
	}

	@Override
	public final Object getObject(String columnLabel) throws SQLException {
		validateThread();
		return r.getObject(columnLabel);
	}

	@Override
	public final int findColumn(String columnLabel) throws SQLException {
		validateThread();
		return r.findColumn(columnLabel);
	}

	@Override
	public final Reader getCharacterStream(int columnIndex)
			throws SQLException {
		validateThread();
		return r.getCharacterStream(columnIndex);
	}

	@Override
	public final Reader getCharacterStream(String columnLabel)
			throws SQLException {
		validateThread();
		return r.getCharacterStream(columnLabel);
	}

	@Override
	public final BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		validateThread();
		return r.getBigDecimal(columnIndex);
	}

	@Override
	public final BigDecimal getBigDecimal(String columnLabel)
			throws SQLException {
		validateThread();
		return r.getBigDecimal(columnLabel);
	}

	@Override
	public final boolean isBeforeFirst() throws SQLException {
		validateThread();
		return r.isBeforeFirst();
	}

	@Override
	public final boolean isAfterLast() throws SQLException {
		validateThread();
		return r.isAfterLast();
	}

	@Override
	public final boolean isFirst() throws SQLException {
		validateThread();
		return r.isFirst();
	}

	@Override
	public final boolean isLast() throws SQLException {
		validateThread();
		return r.isLast();
	}

	@Override
	public final void beforeFirst() throws SQLException {
		validateThread();
		r.beforeFirst();
	}

	@Override
	public final void afterLast() throws SQLException {
		validateThread();
		r.afterLast();
	}

	@Override
	public final boolean first() throws SQLException {
		validateThread();
		return r.first();
	}

	@Override
	public final boolean last() throws SQLException {
		validateThread();
		return r.last();
	}

	@Override
	public final int getRow() throws SQLException {
		validateThread();
		return r.getRow();
	}

	@Override
	public final boolean absolute(int row) throws SQLException {
		validateThread();
		return r.absolute(row);
	}

	@Override
	public final boolean relative(int rows) throws SQLException {
		validateThread();
		return r.relative(rows);
	}

	@Override
	public final boolean previous() throws SQLException {
		validateThread();
		return r.previous();
	}

	@Override
	public final void setFetchDirection(int direction) throws SQLException {
		validateThread();
		r.setFetchDirection(direction);
	}

	@Override
	public final int getFetchDirection() throws SQLException {
		validateThread();
		return r.getFetchDirection();
	}

	@Override
	public final void setFetchSize(int rows) throws SQLException {
		validateThread();
		r.setFetchSize(rows);
	}

	@Override
	public final int getFetchSize() throws SQLException {
		validateThread();
		return r.getFetchSize();
	}

	@Override
	public final int getType() throws SQLException {
		validateThread();
		return r.getType();
	}

	@Override
	public final int getConcurrency() throws SQLException {
		validateThread();
		return r.getConcurrency();
	}

	@Override
	public final boolean rowUpdated() throws SQLException {
		validateThread();
		return r.rowUpdated();
	}

	@Override
	public final boolean rowInserted() throws SQLException {
		validateThread();
		return r.rowInserted();
	}

	@Override
	public final boolean rowDeleted() throws SQLException {
		validateThread();
		return r.rowDeleted();
	}

	@Override
	public final void updateNull(int columnIndex) throws SQLException {
		validateThread();
		r.updateNull(columnIndex);
	}

	@Override
	public final void updateBoolean(int columnIndex, boolean x)
			throws SQLException {
		validateThread();
		r.updateBoolean(columnIndex, x);
	}

	@Override
	public final void updateByte(int columnIndex, byte x) throws SQLException {
		validateThread();
		r.updateByte(columnIndex, x);
	}

	@Override
	public final void updateShort(int columnIndex, short x)
			throws SQLException {
		validateThread();
		r.updateShort(columnIndex, x);
	}

	@Override
	public final void updateInt(int columnIndex, int x) throws SQLException {
		validateThread();
		r.updateInt(columnIndex, x);
	}

	@Override
	public final void updateLong(int columnIndex, long x) throws SQLException {
		validateThread();
		r.updateLong(columnIndex, x);
	}

	@Override
	public final void updateFloat(int columnIndex, float x)
			throws SQLException {
		validateThread();
		r.updateFloat(columnIndex, x);
	}

	@Override
	public final void updateDouble(int columnIndex, double x)
			throws SQLException {
		validateThread();
		r.updateDouble(columnIndex, x);
	}

	@Override
	public final void updateBigDecimal(int columnIndex, BigDecimal x)
			throws SQLException {
		validateThread();
		r.updateBigDecimal(columnIndex, x);
	}

	@Override
	public final void updateString(int columnIndex, String x)
			throws SQLException {
		validateThread();
		r.updateString(columnIndex, x);
	}

	@Override
	public final void updateBytes(int columnIndex, byte[] x)
			throws SQLException {
		validateThread();
		r.updateBytes(columnIndex, x);
	}

	@Override
	public final void updateDate(int columnIndex, Date x) throws SQLException {
		validateThread();
		r.updateDate(columnIndex, x);
	}

	@Override
	public final void updateTime(int columnIndex, Time x) throws SQLException {
		validateThread();
		r.updateTime(columnIndex, x);
	}

	@Override
	public final void updateTimestamp(int columnIndex, Timestamp x)
			throws SQLException {
		validateThread();
		r.updateTimestamp(columnIndex, x);
	}

	@Override
	public final void updateAsciiStream(int columnIndex, InputStream x,
			int length) throws SQLException {
		validateThread();
		r.updateAsciiStream(columnIndex, x, length);
	}

	@Override
	public final void updateBinaryStream(int columnIndex, InputStream x,
			int length) throws SQLException {
		validateThread();
		r.updateBinaryStream(columnIndex, x, length);
	}

	@Override
	public final void updateCharacterStream(int columnIndex, Reader x,
			int length) throws SQLException {
		validateThread();
		r.updateCharacterStream(columnIndex, x, length);
	}

	@Override
	public final void updateObject(int columnIndex, Object x, int scaleOrLength)
			throws SQLException {
		validateThread();
		r.updateObject(columnIndex, x, scaleOrLength);
	}

	@Override
	public final void updateObject(int columnIndex, Object x)
			throws SQLException {
		validateThread();
		r.updateObject(columnIndex, x);
	}

	@Override
	public final void updateNull(String columnLabel) throws SQLException {
		validateThread();
		r.updateNull(columnLabel);
	}

	@Override
	public final void updateBoolean(String columnLabel, boolean x)
			throws SQLException {
		validateThread();
		r.updateBoolean(columnLabel, x);
	}

	@Override
	public final void updateByte(String columnLabel, byte x)
			throws SQLException {
		validateThread();
		r.updateByte(columnLabel, x);
	}

	@Override
	public final void updateShort(String columnLabel, short x)
			throws SQLException {
		validateThread();
		r.updateShort(columnLabel, x);
	}

	@Override
	public final void updateInt(String columnLabel, int x) throws SQLException {
		validateThread();
		r.updateInt(columnLabel, x);
	}

	@Override
	public final void updateLong(String columnLabel, long x)
			throws SQLException {
		validateThread();
		r.updateLong(columnLabel, x);
	}

	@Override
	public final void updateFloat(String columnLabel, float x)
			throws SQLException {
		validateThread();
		r.updateFloat(columnLabel, x);
	}

	@Override
	public final void updateDouble(String columnLabel, double x)
			throws SQLException {
		validateThread();
		r.updateDouble(columnLabel, x);
	}

	@Override
	public final void updateBigDecimal(String columnLabel, BigDecimal x)
			throws SQLException {
		validateThread();
		r.updateBigDecimal(columnLabel, x);
	}

	@Override
	public final void updateString(String columnLabel, String x)
			throws SQLException {
		validateThread();
		r.updateString(columnLabel, x);
	}

	@Override
	public final void updateBytes(String columnLabel, byte[] x)
			throws SQLException {
		validateThread();
		r.updateBytes(columnLabel, x);
	}

	@Override
	public final void updateDate(String columnLabel, Date x)
			throws SQLException {
		validateThread();
		r.updateDate(columnLabel, x);
	}

	@Override
	public final void updateTime(String columnLabel, Time x)
			throws SQLException {
		validateThread();
		r.updateTime(columnLabel, x);
	}

	@Override
	public final void updateTimestamp(String columnLabel, Timestamp x)
			throws SQLException {
		validateThread();
		r.updateTimestamp(columnLabel, x);
	}

	@Override
	public final void updateAsciiStream(String columnLabel, InputStream x,
			int length) throws SQLException {
		validateThread();
		r.updateAsciiStream(columnLabel, x, length);
	}

	@Override
	public final void updateBinaryStream(String columnLabel, InputStream x,
			int length) throws SQLException {
		validateThread();
		r.updateBinaryStream(columnLabel, x, length);
	}

	@Override
	public final void updateCharacterStream(String columnLabel, Reader reader,
			int length) throws SQLException {
		validateThread();
		r.updateCharacterStream(columnLabel, reader, length);
	}

	@Override
	public final void updateObject(String columnLabel, Object x,
			int scaleOrLength) throws SQLException {
		validateThread();
		r.updateObject(columnLabel, x, scaleOrLength);
	}

	@Override
	public final void updateObject(String columnLabel, Object x)
			throws SQLException {
		validateThread();
		r.updateObject(columnLabel, x);
	}

	@Override
	public final void insertRow() throws SQLException {
		validateThread();
		r.insertRow();
	}

	@Override
	public final void updateRow() throws SQLException {
		validateThread();
		r.updateRow();
	}

	@Override
	public final void deleteRow() throws SQLException {
		validateThread();
		r.deleteRow();
	}

	@Override
	public final void refreshRow() throws SQLException {
		validateThread();
		r.refreshRow();
	}

	@Override
	public final void cancelRowUpdates() throws SQLException {
		validateThread();
		r.cancelRowUpdates();
	}

	@Override
	public final void moveToInsertRow() throws SQLException {
		validateThread();
		r.moveToInsertRow();
	}

	@Override
	public final void moveToCurrentRow() throws SQLException {
		validateThread();
		r.moveToCurrentRow();
	}

	@Override
	public final Statement getStatement() throws SQLException {
		if (s != null) {
			return s;
		}
		validateThread();
		Statement st = r.getStatement();
		if (st instanceof CallableStatement) {
			return wrap((CallableStatement) st);
		} else if (st instanceof PreparedStatement) {
			return wrap((PreparedStatement) st);
		} else {
			return wrap(st);
		}
	}

	@Override
	public final Object getObject(int columnIndex, Map<String, Class<?>> map)
			throws SQLException {
		validateThread();
		return r.getObject(columnIndex, map);
	}

	@Override
	public final Ref getRef(int columnIndex) throws SQLException {
		validateThread();
		return r.getRef(columnIndex);
	}

	@Override
	public final Blob getBlob(int columnIndex) throws SQLException {
		validateThread();
		return r.getBlob(columnIndex);
	}

	@Override
	public final Clob getClob(int columnIndex) throws SQLException {
		validateThread();
		return r.getClob(columnIndex);
	}

	@Override
	public final Array getArray(int columnIndex) throws SQLException {
		validateThread();
		return r.getArray(columnIndex);
	}

	@Override
	public final Object getObject(String columnLabel, Map<String, Class<?>> map)
			throws SQLException {
		validateThread();
		return r.getObject(columnLabel, map);
	}

	@Override
	public final Ref getRef(String columnLabel) throws SQLException {
		validateThread();
		return r.getRef(columnLabel);
	}

	@Override
	public final Blob getBlob(String columnLabel) throws SQLException {
		validateThread();
		return r.getBlob(columnLabel);
	}

	@Override
	public final Clob getClob(String columnLabel) throws SQLException {
		validateThread();
		return r.getClob(columnLabel);
	}

	@Override
	public final Array getArray(String columnLabel) throws SQLException {
		validateThread();
		return r.getArray(columnLabel);
	}

	@Override
	public final Date getDate(int columnIndex, Calendar cal)
			throws SQLException {
		validateThread();
		return r.getDate(columnIndex, cal);
	}

	@Override
	public final Date getDate(String columnLabel, Calendar cal)
			throws SQLException {
		validateThread();
		return r.getDate(columnLabel, cal);
	}

	@Override
	public final Time getTime(int columnIndex, Calendar cal)
			throws SQLException {
		validateThread();
		return r.getTime(columnIndex, cal);
	}

	@Override
	public final Time getTime(String columnLabel, Calendar cal)
			throws SQLException {
		validateThread();
		return r.getTime(columnLabel, cal);
	}

	@Override
	public final Timestamp getTimestamp(int columnIndex, Calendar cal)
			throws SQLException {
		validateThread();
		return r.getTimestamp(columnIndex, cal);
	}

	@Override
	public final Timestamp getTimestamp(String columnLabel, Calendar cal)
			throws SQLException {
		validateThread();
		return r.getTimestamp(columnLabel, cal);
	}

	@Override
	public final URL getURL(int columnIndex) throws SQLException {
		validateThread();
		return r.getURL(columnIndex);
	}

	@Override
	public final URL getURL(String columnLabel) throws SQLException {
		validateThread();
		return r.getURL(columnLabel);
	}

	@Override
	public final void updateRef(int columnIndex, Ref x) throws SQLException {
		validateThread();
		r.updateRef(columnIndex, x);
	}

	@Override
	public final void updateRef(String columnLabel, Ref x) throws SQLException {
		validateThread();
		r.updateRef(columnLabel, x);
	}

	@Override
	public final void updateBlob(int columnIndex, Blob x) throws SQLException {
		validateThread();
		r.updateBlob(columnIndex, x);
	}

	@Override
	public final void updateBlob(String columnLabel, Blob x)
			throws SQLException {
		validateThread();
		r.updateBlob(columnLabel, x);
	}

	@Override
	public final void updateClob(int columnIndex, Clob x) throws SQLException {
		validateThread();
		r.updateClob(columnIndex, x);
	}

	@Override
	public final void updateClob(String columnLabel, Clob x)
			throws SQLException {
		validateThread();
		r.updateClob(columnLabel, x);
	}

	@Override
	public final void updateArray(int columnIndex, Array x)
			throws SQLException {
		validateThread();
		r.updateArray(columnIndex, x);
	}

	@Override
	public final void updateArray(String columnLabel, Array x)
			throws SQLException {
		validateThread();
		r.updateArray(columnLabel, x);
	}

	@Override
	public final RowId getRowId(int columnIndex) throws SQLException {
		validateThread();
		return r.getRowId(columnIndex);
	}

	@Override
	public final RowId getRowId(String columnLabel) throws SQLException {
		validateThread();
		return r.getRowId(columnLabel);
	}

	@Override
	public final void updateRowId(int columnIndex, RowId x)
			throws SQLException {
		validateThread();
		r.updateRowId(columnIndex, x);
	}

	@Override
	public final void updateRowId(String columnLabel, RowId x)
			throws SQLException {
		validateThread();
		r.updateRowId(columnLabel, x);
	}

	@Override
	public final int getHoldability() throws SQLException {
		validateThread();
		return r.getHoldability();
	}

	@Override
	public final boolean isClosed() throws SQLException {
		validateThread();
		return r.isClosed();
	}

	@Override
	public final void updateNString(int columnIndex, String nString)
			throws SQLException {
		validateThread();
		r.updateNString(columnIndex, nString);
	}

	@Override
	public final void updateNString(String columnLabel, String nString)
			throws SQLException {
		validateThread();
		r.updateNString(columnLabel, nString);
	}

	@Override
	public final void updateNClob(int columnIndex, NClob nClob)
			throws SQLException {
		validateThread();
		r.updateNClob(columnIndex, nClob);
	}

	@Override
	public final void updateNClob(String columnLabel, NClob nClob)
			throws SQLException {
		validateThread();
		r.updateNClob(columnLabel, nClob);
	}

	@Override
	public final NClob getNClob(int columnIndex) throws SQLException {
		validateThread();
		return r.getNClob(columnIndex);
	}

	@Override
	public final NClob getNClob(String columnLabel) throws SQLException {
		validateThread();
		return r.getNClob(columnLabel);
	}

	@Override
	public final SQLXML getSQLXML(int columnIndex) throws SQLException {
		validateThread();
		return r.getSQLXML(columnIndex);
	}

	@Override
	public final SQLXML getSQLXML(String columnLabel) throws SQLException {
		validateThread();
		return r.getSQLXML(columnLabel);
	}

	@Override
	public final void updateSQLXML(int columnIndex, SQLXML xmlObject)
			throws SQLException {
		validateThread();
		r.updateSQLXML(columnIndex, xmlObject);
	}

	@Override
	public final void updateSQLXML(String columnLabel, SQLXML xmlObject)
			throws SQLException {
		validateThread();
		r.updateSQLXML(columnLabel, xmlObject);
	}

	@Override
	public final String getNString(int columnIndex) throws SQLException {
		validateThread();
		return r.getNString(columnIndex);
	}

	@Override
	public final String getNString(String columnLabel) throws SQLException {
		validateThread();
		return r.getNString(columnLabel);
	}

	@Override
	public final Reader getNCharacterStream(int columnIndex)
			throws SQLException {
		validateThread();
		return r.getNCharacterStream(columnIndex);
	}

	@Override
	public final Reader getNCharacterStream(String columnLabel)
			throws SQLException {
		validateThread();
		return r.getNCharacterStream(columnLabel);
	}

	@Override
	public final void updateNCharacterStream(int columnIndex, Reader x,
			long length) throws SQLException {
		validateThread();
		r.updateNCharacterStream(columnIndex, x, length);
	}

	@Override
	public final void updateNCharacterStream(String columnLabel, Reader reader,
			long length) throws SQLException {
		validateThread();
		r.updateNCharacterStream(columnLabel, reader, length);
	}

	@Override
	public final void updateAsciiStream(int columnIndex, InputStream x,
			long length) throws SQLException {
		validateThread();
		r.updateAsciiStream(columnIndex, x, length);
	}

	@Override
	public final void updateBinaryStream(int columnIndex, InputStream x,
			long length) throws SQLException {
		validateThread();
		r.updateBinaryStream(columnIndex, x, length);
	}

	@Override
	public final void updateCharacterStream(int columnIndex, Reader x,
			long length) throws SQLException {
		validateThread();
		r.updateCharacterStream(columnIndex, x, length);
	}

	@Override
	public final void updateAsciiStream(String columnLabel, InputStream x,
			long length) throws SQLException {
		validateThread();
		r.updateAsciiStream(columnLabel, x, length);
	}

	@Override
	public final void updateBinaryStream(String columnLabel, InputStream x,
			long length) throws SQLException {
		validateThread();
		r.updateBinaryStream(columnLabel, x, length);
	}

	@Override
	public final void updateCharacterStream(String columnLabel, Reader reader,
			long length) throws SQLException {
		validateThread();
		r.updateCharacterStream(columnLabel, reader, length);
	}

	@Override
	public final void updateBlob(int columnIndex, InputStream inputStream,
			long length) throws SQLException {
		validateThread();
		r.updateBlob(columnIndex, inputStream, length);
	}

	@Override
	public final void updateBlob(String columnLabel, InputStream inputStream,
			long length) throws SQLException {
		validateThread();
		r.updateBlob(columnLabel, inputStream, length);
	}

	@Override
	public final void updateClob(int columnIndex, Reader reader, long length)
			throws SQLException {
		validateThread();
		r.updateClob(columnIndex, reader, length);
	}

	@Override
	public final void updateClob(String columnLabel, Reader reader, long length)
			throws SQLException {
		validateThread();
		r.updateClob(columnLabel, reader, length);
	}

	@Override
	public final void updateNClob(int columnIndex, Reader reader, long length)
			throws SQLException {
		validateThread();
		r.updateNClob(columnIndex, reader, length);
	}

	@Override
	public final void updateNClob(String columnLabel, Reader reader,
			long length) throws SQLException {
		validateThread();
		r.updateNClob(columnLabel, reader, length);
	}

	@Override
	public final void updateNCharacterStream(int columnIndex, Reader x)
			throws SQLException {
		validateThread();
		r.updateNCharacterStream(columnIndex, x);
	}

	@Override
	public final void updateNCharacterStream(String columnLabel, Reader reader)
			throws SQLException {
		validateThread();
		r.updateNCharacterStream(columnLabel, reader);
	}

	@Override
	public final void updateAsciiStream(int columnIndex, InputStream x)
			throws SQLException {
		validateThread();
		r.updateAsciiStream(columnIndex, x);
	}

	@Override
	public final void updateBinaryStream(int columnIndex, InputStream x)
			throws SQLException {
		validateThread();
		r.updateBinaryStream(columnIndex, x);
	}

	@Override
	public final void updateCharacterStream(int columnIndex, Reader x)
			throws SQLException {
		validateThread();
		r.updateCharacterStream(columnIndex, x);
	}

	@Override
	public final void updateAsciiStream(String columnLabel, InputStream x)
			throws SQLException {
		validateThread();
		r.updateAsciiStream(columnLabel, x);
	}

	@Override
	public final void updateBinaryStream(String columnLabel, InputStream x)
			throws SQLException {
		validateThread();
		r.updateBinaryStream(columnLabel, x);
	}

	@Override
	public final void updateCharacterStream(String columnLabel, Reader reader)
			throws SQLException {
		validateThread();
		r.updateCharacterStream(columnLabel, reader);
	}

	@Override
	public final void updateBlob(int columnIndex, InputStream inputStream)
			throws SQLException {
		validateThread();
		r.updateBlob(columnIndex, inputStream);
	}

	@Override
	public final void updateBlob(String columnLabel, InputStream inputStream)
			throws SQLException {
		validateThread();
		r.updateBlob(columnLabel, inputStream);
	}

	@Override
	public final void updateClob(int columnIndex, Reader reader)
			throws SQLException {
		validateThread();
		r.updateClob(columnIndex, reader);
	}

	@Override
	public final void updateClob(String columnLabel, Reader reader)
			throws SQLException {
		validateThread();
		r.updateClob(columnLabel, reader);
	}

	@Override
	public final void updateNClob(int columnIndex, Reader reader)
			throws SQLException {
		validateThread();
		r.updateNClob(columnIndex, reader);
	}

	@Override
	public final void updateNClob(String columnLabel, Reader reader)
			throws SQLException {
		validateThread();
		r.updateNClob(columnLabel, reader);
	}

	@Override
	public final <T> T getObject(int columnIndex, Class<T> type)
			throws SQLException {
		validateThread();
		return r.getObject(columnIndex, type);
	}

	@Override
	public final <T> T getObject(String columnLabel, Class<T> type)
			throws SQLException {
		validateThread();
		return r.getObject(columnLabel, type);
	}
}
