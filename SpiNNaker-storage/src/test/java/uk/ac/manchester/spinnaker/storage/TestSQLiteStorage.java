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

		Storage.Region r = new Storage.Region(core, 0, 0, 100);
		storage.storeRegionContents(r, bytes("abc"));
		storage.appendRecordingContents(r, 0, bytes("def"));

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
		Storage.Region r = new Storage.Region(core, 0, 0, 100);
		storage.storeRegionContents(r, bytes("abc"));
		storage.storeRegionContents(r, bytes("def"));
		assertEquals("def",
				new String(storage.getRegionContents(core, 0), UTF_8));

		// append creates
		storage.appendRecordingContents(r, 1, bytes("abc"));
		storage.appendRecordingContents(r, 1, bytes("def"));
		assertEquals("abcdef",
				new String(storage.getRegionContents(core, 1), UTF_8));
	}

}
