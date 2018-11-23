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

	@Test
	void testBasicOps() throws StorageException {
		ConnectionProvider engine = new DatabaseEngine(db);
		Storage storage = new SQLiteStorage(engine);
		HasCoreLocation core = new CoreLocation(0, 0, 0);

		assertEquals(Collections.emptyList(), storage.getCoresWithStorage());

		storage.storeRegionContents(core, 0, bytes("abc"));
		storage.appendRegionContents(core, 0, bytes("def"));

		assertArrayEquals("abcdef".getBytes(UTF_8),
				storage.getRegionContents(core, 0));
		assertEquals(Arrays.asList(core), storage.getCoresWithStorage());
		assertEquals(Arrays.asList(0), storage.getRegionsWithStorage(core));

		storage.deleteRegionContents(core, 0);

		assertEquals(Collections.emptyList(), storage.getCoresWithStorage());
	}

	@Test
	void testWithExisting() throws StorageException {
		ConnectionProvider engine = new DatabaseEngine(db);
		Storage storage = new SQLiteStorage(engine);
		HasCoreLocation core = new CoreLocation(0, 0, 0);

		// store overwrites
		storage.storeRegionContents(core, 0, bytes("abc"));
		storage.storeRegionContents(core, 0, bytes("def"));
		assertEquals("def",
				new String(storage.getRegionContents(core, 0), UTF_8));

		// append creates
		storage.appendRegionContents(core, 1, bytes("abc"));
		storage.appendRegionContents(core, 1, bytes("def"));
		assertEquals("abcdef",
				new String(storage.getRegionContents(core, 1), UTF_8));
	}

}
