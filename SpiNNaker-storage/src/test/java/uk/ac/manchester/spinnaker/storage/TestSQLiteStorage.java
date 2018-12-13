/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

class TestSQLiteStorage {
	File db;

	@BeforeEach
	void makeDB() {
		db = new File("target/test.db");
	}

	@AfterEach
	void killDB() {
		db.delete();
	}

	private static ByteBuffer bytes(String str) {
		return ByteBuffer.wrap(str.getBytes(UTF_8));
	}

	private static String str(byte[] bytes) {
		return new String(bytes, UTF_8);
	}

	@Test
	void testBasicOps() throws StorageException {
		ConnectionProvider engine = new BufferManagerDatabaseEngine(db);
		BufferManagerStorage storage = engine.getBufferManagerStorage();
		HasCoreLocation core = new CoreLocation(0, 0, 0);

		assertEquals(Collections.emptyList(), storage.getCoresWithStorage());

		BufferManagerStorage.Region rr = new BufferManagerStorage.Region(core, 0, 0, 100);
		storage.appendRecordingContents(rr, bytes("def"));
		assertArrayEquals("def".getBytes(UTF_8),
				storage.getRecordingRegionContents(rr));

		assertEquals(Arrays.asList(core), storage.getCoresWithStorage());
		assertEquals(Arrays.asList(0), storage.getRegionsWithStorage(core));
	}

	@Test
	void testWithExisting() throws StorageException {
		ConnectionProvider engine = new BufferManagerDatabaseEngine(db);
		BufferManagerStorage storage = engine.getBufferManagerStorage();
		HasCoreLocation core = new CoreLocation(0, 0, 0);

		// append creates
		BufferManagerStorage.Region rr = new BufferManagerStorage.Region(core, 1, 0, 100);
		storage.appendRecordingContents(rr, bytes("abc"));
		storage.appendRecordingContents(rr, bytes("def"));
		assertEquals("abcdef", str(storage.getRecordingRegionContents(rr)));
	}

}
