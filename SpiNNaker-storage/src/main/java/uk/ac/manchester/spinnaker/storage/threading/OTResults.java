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

	/** {@inheritDoc} */
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

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		validateThread();
		return r.getUnicodeStream(columnIndex);
	}

	@Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
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

	/** {@inheritDoc} */
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
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		validateThread();
		return r.getAsciiStream(columnLabel);
	}

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public InputStream getUnicodeStream(String columnLabel)
			throws SQLException {
		validateThread();
		return r.getUnicodeStream(columnLabel);
	}

	@Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
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
		return wrap(r.getMetaData());
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
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		validateThread();
		return r.getCharacterStream(columnLabel);
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		validateThread();
		return r.getBigDecimal(columnIndex);
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
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
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
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
	public void updateDouble(int columnIndex, double x) throws SQLException {
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
	public void updateString(int columnIndex, String x) throws SQLException {
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
	public void updateAsciiStream(int columnIndex, InputStream x, int length)
			throws SQLException {
		validateThread();
		r.updateAsciiStream(columnIndex, x, length);
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length)
			throws SQLException {
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
	public void updateObject(int columnIndex, Object x) throws SQLException {
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
	public void updateShort(String columnLabel, short x) throws SQLException {
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
	public void updateFloat(String columnLabel, float x) throws SQLException {
		validateThread();
		r.updateFloat(columnLabel, x);
	}

	@Override
	public void updateDouble(String columnLabel, double x) throws SQLException {
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
	public void updateString(String columnLabel, String x) throws SQLException {
		validateThread();
		r.updateString(columnLabel, x);
	}

	@Override
	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
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
	public void updateAsciiStream(String columnLabel, InputStream x, int length)
			throws SQLException {
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
	public void updateObject(String columnLabel, Object x, int scaleOrLength)
			throws SQLException {
		validateThread();
		r.updateObject(columnLabel, x, scaleOrLength);
	}

	@Override
	public void updateObject(String columnLabel, Object x) throws SQLException {
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
		var st = r.getStatement();
		if (st instanceof CallableStatement) {
			return wrap((CallableStatement) st);
		} else if (st instanceof PreparedStatement) {
			return wrap((PreparedStatement) st);
		} else {
			return wrap(st);
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
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		validateThread();
		return r.getDate(columnLabel, cal);
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		validateThread();
		return r.getTime(columnIndex, cal);
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
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
	public void updateArray(String columnLabel, Array x) throws SQLException {
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
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
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
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
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
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		validateThread();
		return r.getNCharacterStream(columnLabel);
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x, long length)
			throws SQLException {
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
	public void updateAsciiStream(int columnIndex, InputStream x, long length)
			throws SQLException {
		validateThread();
		r.updateAsciiStream(columnIndex, x, length);
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length)
			throws SQLException {
		validateThread();
		r.updateBinaryStream(columnIndex, x, length);
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length)
			throws SQLException {
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
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
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
	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
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
