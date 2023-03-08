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
package uk.ac.manchester.spinnaker.storage;

import static java.nio.ByteBuffer.wrap;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.manchester.spinnaker.machine.MemoryLocation.NULL;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.CoreLocation;

class TestSQLiteStorage {
	private File db;

	@BeforeEach
	void makeDB() {
		db = new File("target/test.db");
	}

	@AfterEach
	void killDB() {
		db.delete();
	}

	private static ByteBuffer bytes(String str) {
		return wrap(str.getBytes(UTF_8));
	}

	private static String str(byte[] bytes) {
		return new String(bytes, UTF_8);
	}

	@Test
	void testBasicOps() throws StorageException {
		var storage = new BufferManagerDatabaseEngine(db).getStorageInterface();
		var core = new CoreLocation(0, 0, 0);

		assertEquals(List.of(), storage.getCoresWithStorage());

		var rr = new BufferManagerStorage.Region(core, 0, NULL, 100);
		storage.appendRecordingContents(rr, bytes("def"));
		assertArrayEquals("def".getBytes(UTF_8),
				storage.getRecordingRegionContents(rr));

		assertEquals(List.of(core), storage.getCoresWithStorage());
		assertEquals(List.of(0), storage.getRegionsWithStorage(core));
	}

	@Test
	void testWithExisting() throws StorageException {
		var storage = new BufferManagerDatabaseEngine(db).getStorageInterface();
		var core = new CoreLocation(0, 0, 0);

		// append creates
		var rr = new BufferManagerStorage.Region(core, 1, NULL, 100);
		storage.appendRecordingContents(rr, bytes("ab"));
		storage.appendRecordingContents(rr, bytes("cd"));
		storage.appendRecordingContents(rr, bytes("ef"));
		assertEquals("abcdef", str(storage.getRecordingRegionContents(rr)));
	}

}
