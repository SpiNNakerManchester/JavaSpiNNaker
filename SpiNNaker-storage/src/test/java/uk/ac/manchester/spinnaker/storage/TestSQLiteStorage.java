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
		ConnectionProvider engine = new DSEDatabaseEngine(db);
		Storage storage = new SQLiteStorage(engine);
		HasCoreLocation core = new CoreLocation(0, 0, 0);

		assertEquals(Collections.emptyList(), storage.getCoresWithStorage());

		Storage.Region r = new Storage.Region(core, 0, 0, 100);
		storage.storeRegionContents(r, bytes("abc"));
		assertArrayEquals("abc".getBytes(UTF_8),
				storage.getRegionContents(r));

		Storage.Region rr = new Storage.Region(core, 1, 0, 100);
		storage.noteRecordingVertex(rr, "foo");
		storage.appendRecordingContents(rr, 0, bytes("def"));
		assertArrayEquals("def".getBytes(UTF_8),
				storage.getRecordingRegionContents(rr, 0));

		assertEquals(Arrays.asList(core), storage.getCoresWithStorage());
		assertEquals(Arrays.asList(0), storage.getRegionsWithStorage(core));

		storage.deleteRegionContents(core, 0);

		assertEquals(Collections.emptyList(), storage.getCoresWithStorage());
	}

	@Test
	void testWithExisting() throws StorageException {
		ConnectionProvider engine = new DSEDatabaseEngine(db);
		Storage storage = new SQLiteStorage(engine);
		HasCoreLocation core = new CoreLocation(0, 0, 0);

		// store overwrites
		Storage.Region r = new Storage.Region(core, 0, 0, 100);
		storage.storeRegionContents(r, bytes("abc"));
		storage.storeRegionContents(r, bytes("def"));
		assertEquals("def", str(storage.getRegionContents(r)));

		// append creates
		Storage.Region rr = new Storage.Region(core, 1, 0, 100);
		storage.noteRecordingVertex(rr, "foo");
		storage.appendRecordingContents(rr, 0, bytes("abc"));
		storage.appendRecordingContents(rr, 0, bytes("def"));
		assertEquals("abcdef", str(storage.getRecordingRegionContents(rr, 0)));
	}

}
