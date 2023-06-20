/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage.sqlite;

import static java.lang.System.arraycopy;
import static java.lang.System.currentTimeMillis;
import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.ADD_CONTENT;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.ADD_EXTRA_CONTENT;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.FETCH_EXTRA_RECORDING;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.FETCH_RECORDING;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_CORES_WITH_STORAGE;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_LOCATION;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_MAIN_CONTENT_AVAILABLE;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_REGION;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_REGIONS_WITH_STORAGE;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.INSERT_LOCATION;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.INSERT_REGION;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.PREP_EXTRA_CONTENT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.storage.BufferManagerDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;

/**
 * How to actually talk to an SQLite database.
 *
 * @author Donal Fellows
 */
public final class SQLiteBufferStorage extends
		SQLiteStorage<BufferManagerStorage> implements BufferManagerStorage {
	private static final Logger log = getLogger(SQLiteBufferStorage.class);

	/**
	 * Create an instance.
	 *
	 * @param db
	 *            The database engine that will be asked for how to talk SQL to
	 *            the database.
	 */
	public SQLiteBufferStorage(BufferManagerDatabaseEngine db) {
		super(db);
	}

	private static int getRecordingCore(Connection conn, CoreLocation core)
			throws SQLException {
		try (var s = conn.prepareStatement(GET_LOCATION)) {
			// x, y, processor
			setArguments(s, core.getX(), core.getY(), core.getP());
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt("core_id");
				}
			}
		}
		try (var s = conn.prepareStatement(INSERT_LOCATION,
				RETURN_GENERATED_KEYS)) {
			// x, y, processor
			setArguments(s, core.getX(), core.getY(), core.getP());
			s.executeUpdate();
			return getLastKey(s).orElseThrow(() -> new IllegalStateException(
					"could not make or find recording region core record"));
		}
	}

	private static int getRecordingRegion(Connection conn, int coreID,
			Region region) throws SQLException {
		try (var s = conn.prepareStatement(GET_REGION)) {
			// core_id, local_region_index
			setArguments(s, coreID, region.regionIndex);
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt("region_id");
				}
			}
		}
		try (var s = conn.prepareStatement(INSERT_REGION,
				RETURN_GENERATED_KEYS)) {
			// core_id, local_region_index, address
			setArguments(s, coreID, region.regionIndex,
					region.startAddress.address());
			s.executeUpdate();
			return getLastKey(s).orElseThrow(() -> new IllegalStateException(
					"could not make or find recording region record"));
		}
	}

	private void appendRecordingContents(Connection conn, int regionID,
			byte[] content) throws SQLException {
		int chunkLen = content.length;
		var chunk = new ByteArrayInputStream(content);
		long timestamp = currentTimeMillis();
		if (useMainTable(conn, regionID)) {
			log.debug("adding chunk of {} bytes to region table for region {}",
					chunkLen, regionID);
			addContentToMainRow(conn, regionID, chunkLen, chunk, timestamp);
		} else {
			log.debug("adding chunk of {} bytes to extra table for region {}",
					chunkLen, regionID);
			prepareExtraContent(conn, regionID, timestamp);
			addExtraContentRow(conn, regionID, chunkLen, chunk);
		}
	}

	private static byte[] read(ByteArrayInputStream chunk, int chunkLen)
			throws SQLException {
		byte[] buffer = new byte[chunkLen];
		int len;
		try {
			len = IOUtils.read(chunk, buffer, 0, chunkLen);
		} catch (IOException e) {
			throw new SQLException("failed to read buffer", e);
		}
		if (len == chunkLen) {
			return buffer;
		} else if (len <= 0) {
			return new byte[0];
		}
		byte[] nb = new byte[len];
		arraycopy(buffer, 0, nb, 0, len);
		return nb;
	}

	private void addContentToMainRow(Connection conn, int regionID,
			int chunkLen, ByteArrayInputStream chunk, long timestamp)
			throws SQLException {
		try (var s = conn.prepareStatement(ADD_CONTENT)) {
			// content, append_time, region_id
			setArguments(s, read(chunk, chunkLen), chunkLen, timestamp,
					regionID);
			s.executeUpdate();
		}
	}

	private void prepareExtraContent(Connection conn, int regionID,
			long timestamp) throws SQLException {
		try (var s = conn.prepareStatement(PREP_EXTRA_CONTENT)) {
			// append_time, region_id
			setArguments(s, timestamp, regionID);
			s.executeUpdate();
		}
	}

	private void addExtraContentRow(Connection conn, int regionID, int chunkLen,
			ByteArrayInputStream chunk) throws SQLException {
		try (var s = conn.prepareStatement(ADD_EXTRA_CONTENT)) {
			// region_id, content
			setArguments(s, regionID, read(chunk, chunkLen), chunkLen);
			s.executeUpdate();
		}
	}

	private boolean useMainTable(Connection conn, int regionID)
			throws SQLException {
		try (var s = conn.prepareStatement(GET_MAIN_CONTENT_AVAILABLE)) {
			setArguments(s, regionID);
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					int existing = rs.getInt("existing");
					return existing == 1;
				}
			}
		}
		return false;
	}

	@Override
	public void appendRecordingContents(Region region, byte[] contents)
			throws StorageException {
		// Strip off any prefix and suffix added to make the read aligned
		byte[] tmp;
		if (region.isAligned()) {
			tmp = contents;
		} else {
			tmp = new byte[region.realSize];
			arraycopy(contents, region.initialIgnore, tmp, 0, region.realSize);
		}
		callV(conn -> appendRecordContents(conn, region, tmp),
				"creating or adding to a recorded region");
	}

	/**
	 * The core of how to append content to a recording.
	 *
	 * @param conn
	 *            The connection, with a transaction open.
	 * @param region
	 *            The recording region owning the recording.
	 * @param contents
	 *            The bytes to append.
	 * @throws SQLException
	 *             If anything goes wrong.
	 * @see #appendRecordingContents(Region,int,byte[])
	 */
	private void appendRecordContents(Connection conn, Region region,
			byte[] contents) throws SQLException {
		int coreID = getRecordingCore(conn, region.core);
		int regionID = getRecordingRegion(conn, coreID, region);
		appendRecordingContents(conn, regionID, contents);
	}

	@Override
	public byte[] getRecordingRegionContents(Region region)
			throws StorageException {
		return callR(conn -> getRecordingRegionContents(conn, region),
				"retrieving a recording region");
	}

	private static byte[] getRecordingRegionContents(Connection conn,
			Region region) throws SQLException {
		var accum = new ByteArrayOutputStream();
		try {
			int regionID = -1;
			try (var s = conn.prepareStatement(FETCH_RECORDING)) {
				// x, y, processor, local_region_index
				setArguments(s, region.core.getX(), region.core.getY(),
						region.core.getP(), region.regionIndex);
				try (var rs = s.executeQuery()) {
					while (rs.next()) {
						accum.write(rs.getBytes("content"));
						regionID = rs.getInt("region_id");
					}
				}
			}
			if (regionID < 0) {
				throw new IllegalArgumentException("core " + region.core
						+ " has no data for region " + region.regionIndex);
			}
			try (var s = conn.prepareStatement(FETCH_EXTRA_RECORDING)) {
				// region_id
				setArguments(s, regionID);
				try (var rs = s.executeQuery()) {
					while (rs.next()) {
						accum.write(rs.getBytes("content"));
					}
				}
			}
		} catch (IOException | OutOfMemoryError e) {
			throw new RuntimeException("BLOB sequence too large for Java", e);
		}
		return accum.toByteArray();
	}

	@Override
	public List<CoreLocation> getCoresWithStorage() throws StorageException {
		return callR(conn -> {
			try (var s = conn.prepareStatement(GET_CORES_WITH_STORAGE);
					var rs = s.executeQuery()) {
				var result = new ArrayList<CoreLocation>();
				while (rs.next()) {
					int x = rs.getInt("x");
					int y = rs.getInt("y");
					int p = rs.getInt("processor");
					result.add(new CoreLocation(x, y, p));
				}
				return result;
			}
		}, "listing cores");
	}

	@Override
	public List<Integer> getRegionsWithStorage(HasCoreLocation core)
			throws StorageException {
		return callR(conn -> {
			try (var s = conn.prepareStatement(GET_REGIONS_WITH_STORAGE)) {
				setArguments(s, core.getX(), core.getY(), core.getP());
				try (var rs = s.executeQuery()) {
					var result = new ArrayList<Integer>();
					while (rs.next()) {
						result.add(rs.getInt("local_region_index"));
					}
					return result;
				}
			}
		}, "listing regions for a core");
	}
}
