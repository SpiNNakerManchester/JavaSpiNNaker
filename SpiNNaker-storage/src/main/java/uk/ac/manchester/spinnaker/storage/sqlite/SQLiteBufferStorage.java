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
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.ADD_DOWNLOAD_DATA;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.ADD_RECORDING_DATA;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_CORES_WITH_STORAGE;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_DOWNLOAD;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_DOWNLOAD_REGION;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_LAST_EXTRACTION_ID;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_LOCATION;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_RECORDING_REGION;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_RECORDING;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.GET_REGIONS_WITH_STORAGE;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.INSERT_LOCATION;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.INSERT_DOWNLOAD_REGION;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.INSERT_RECORDING_REGION;
import static uk.ac.manchester.spinnaker.storage.sqlite.SQL.INSERT_MOCK_EXTRACTION;

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
public class SQLiteBufferStorage
		extends SQLiteProxyStorage<BufferManagerStorage>
		implements BufferManagerStorage {
	private static final Logger log = getLogger(SQLiteBufferStorage.class);

	/**
	 * Create an instance.
	 *
	 * @param connectionProvider
	 *            The connection provider that will be asked for how to talk SQL
	 *            to the database.
	 */
	public SQLiteBufferStorage(BufferManagerDatabaseEngine connectionProvider) {
		super(connectionProvider);
	}

	private static int getLastExtractionId(
		Connection conn) throws SQLException {
		try (var s = conn.prepareStatement(GET_LAST_EXTRACTION_ID)) {
			// core_id, local_region_index
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt("max_id");
				}
			}
		}
		throw new IllegalStateException(
			"could not find last extraction id");
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
		try (var s = conn.prepareStatement(INSERT_LOCATION)) {
			// x, y, processor
			setArguments(s, core.getX(), core.getY(), core.getP());
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt("core_id");
				}
			}
		}
		throw new IllegalStateException(
				"could not make or find recording region core record");
	}

	private static int getRecordingRegion(Connection conn, int coreID,
			Region region) throws SQLException {
		try (var s = conn.prepareStatement(GET_RECORDING_REGION)) {
			// core_id, local_region_index
			setArguments(s, coreID, region.regionIndex);
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt("recording_region_id");
				}
			}
		}
		try (var s = conn.prepareStatement(INSERT_RECORDING_REGION)) {
			// core_id, local_region_index, address
			setArguments(s, coreID, region.regionIndex);
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt("recording_region_id");
				}
			}
		}
		throw new IllegalStateException(
				"could not make or find recording region record");
	}

	private static int getDownloadRegion(Connection conn, int coreID,
			Region region) throws SQLException {
		try (var s = conn.prepareStatement(GET_DOWNLOAD_REGION)) {
			// core_id, local_region_index
			setArguments(s, coreID, region.regionIndex);
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt("download_region_id");
				}
			}
		}
		try (var s = conn.prepareStatement(INSERT_DOWNLOAD_REGION)) {
			// core_id, local_region_index, address
			setArguments(s, coreID, region.regionIndex);
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt("download_region_id");
				}
			}
		}
		throw new IllegalStateException(
			"could not make or find recording region record");
	}

	private static int getExistingRecordingRegion(
		Connection conn, int coreID, Region region) throws SQLException {
		try (var s = conn.prepareStatement(GET_RECORDING_REGION)) {
			// core_id, local_region_index
			setArguments(s, coreID, region.regionIndex);
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt("recording_region_id");
				}
			}
		}
		throw new IllegalStateException(
			"could not find recording region record");
	}

	private static int getExistingDownloadRegion(
		Connection conn, int coreID, Region region) throws SQLException {
		try (var s = conn.prepareStatement(GET_DOWNLOAD_REGION)) {
			// core_id, local_region_index
			setArguments(s, coreID, region.regionIndex);
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt("download_region_id");
				}
			}
		}
		throw new IllegalStateException(
			"could not find recording region record");
	}

	private static int insertMockExtraction(Connection conn)
			throws SQLException {
		try (var s = conn.prepareStatement(INSERT_MOCK_EXTRACTION)) {
			// core_id, local_region_index, address
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt("extraction_id");
				}
			}
		}
		throw new IllegalStateException(
			"could not mock an extraction record");
	}

	public void insertMockExtraction() throws StorageException {
		callV(conn -> insertMockExtraction(conn),
			"Mocking Extraction");
	}

	private static byte[] read(ByteArrayInputStream chunk, int chunkLen)
			throws SQLException {
		var buffer = new byte[chunkLen];
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
		var nb = new byte[len];
		arraycopy(buffer, 0, nb, 0, len);
		return nb;
	}

	private int addRecordingData(Connection conn, int regionID,
			int extractionId, int chunkLen,
			ByteArrayInputStream chunk) throws SQLException {
		try (var s = conn.prepareStatement(ADD_RECORDING_DATA)) {
			// region_id, extraction_id, content, content_len,
			setArguments(
				s, regionID,  extractionId, read(chunk, chunkLen), chunkLen);
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt("recording_data_id");
				}
			}
		}
		throw new IllegalStateException("no row inserted");
	}

	private int addDownloadData(Connection conn, int regionID, int extractionId,
			int chunkLen, ByteArrayInputStream chunk) throws SQLException {
		try (var s = conn.prepareStatement(ADD_DOWNLOAD_DATA)) {
			// region_id, extraction_id, content, content_len,
			setArguments(
				s, regionID,  extractionId, read(chunk, chunkLen), chunkLen);
			try (var rs = s.executeQuery()) {
				while (rs.next()) {
					return rs.getInt("download_data_id");
				}
			}
		}
		throw new IllegalStateException("no row inserted");
	}

	@Override
	public void addRecordingContents(Region region, byte[] contents)
			throws StorageException {
		// Strip off any prefix and suffix added to make the read aligned
		byte[] tmp;
		if (region.isAligned()) {
			tmp = contents;
		} else {
			tmp = new byte[region.realSize];
			arraycopy(contents, region.initialIgnore, tmp, 0, region.realSize);
		}
		if (region.isRecording) {
			callV(conn -> addRecordingContents(conn, region, tmp),
				"creating or adding to a recorded region");
		} else {
			callV(conn -> addDownloadContents(conn, region, tmp),
				"creating or adding to a recorded region");
		}
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
	 */
	private void addRecordingContents(Connection conn, Region region,
			byte[] contents) throws SQLException {
		int coreID = getRecordingCore(conn, region.core);
		int regionID = getRecordingRegion(conn, coreID, region);
		int lastExtractionId = getLastExtractionId(conn);
		int chunkLen = contents.length;
		var chunk = new ByteArrayInputStream(contents);
		log.debug("adding chunk of {} bytes to region data table for region {}",
			chunkLen, regionID);
		addRecordingData(conn, regionID,  lastExtractionId, chunkLen, chunk);
	}

	private void addDownloadContents(Connection conn, Region region,
			byte[] contents) throws SQLException {
		int coreID = getRecordingCore(conn, region.core);
		int regionID = getDownloadRegion(conn, coreID, region);
		int lastExtractionId = getLastExtractionId(conn);
		int chunkLen = contents.length;
		var chunk = new ByteArrayInputStream(contents);
		log.debug("adding chunk of {} bytes to region data table for region {}",
			chunkLen, regionID);
		addDownloadData(conn, regionID,  lastExtractionId, chunkLen, chunk);
	}

	@Override
	public byte[] getContents(Region region) throws StorageException {
		if (region.isRecording) {
			return callR(conn -> getRecordingContents(conn, region),
				"retrieving a recording region");
		} else {
			return callR(conn -> getDownloadContents(conn, region),
				"retrieving a recording region");
		}
	}

	private static byte[] getRecordingContents(Connection conn,
			Region region) throws SQLException {
		var accum = new ByteArrayOutputStream();
		try {
			int coreID = getRecordingCore(conn, region.core);
			int regionID = getExistingRecordingRegion(conn, coreID, region);
			try (var s = conn.prepareStatement(GET_RECORDING)) {
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

	private static byte[] getDownloadContents(
			Connection conn, Region region) throws SQLException {
		var accum = new ByteArrayOutputStream();
		try {
			int coreID = getRecordingCore(conn, region.core);
			int regionID = getExistingDownloadRegion(conn, coreID, region);
			try (var s = conn.prepareStatement(GET_DOWNLOAD)) {
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
