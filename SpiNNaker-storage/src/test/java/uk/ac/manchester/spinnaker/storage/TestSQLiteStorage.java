package uk.ac.manchester.spinnaker.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
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

	@Test
	void testBasicOps() throws StorageException {
		ConnectionProvider engine = new DatabaseEngine(db);
		Storage storage = new SQLiteStorage(engine);
		HasCoreLocation core = new CoreLocation(0, 0, 0);

		assertEquals(Collections.emptyList(), storage.getCoresWithStorage());

		storage.storeRegionContents(core, 0, "abc".getBytes(UTF_8));
		storage.appendRegionContents(core, 0, "def".getBytes(UTF_8));

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
		storage.storeRegionContents(core, 0, "abc".getBytes(UTF_8));
		storage.storeRegionContents(core, 0, "def".getBytes(UTF_8));
		assertEquals("def",
				new String(storage.getRegionContents(core, 0), UTF_8));

		// append creates
		storage.appendRegionContents(core, 1, "abc".getBytes(UTF_8));
		storage.appendRegionContents(core, 1, "def".getBytes(UTF_8));
		assertEquals("abcdef",
				new String(storage.getRegionContents(core, 1), UTF_8));
	}

}
