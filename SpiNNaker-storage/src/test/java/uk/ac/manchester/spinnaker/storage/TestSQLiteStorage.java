package uk.ac.manchester.spinnaker.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

class TestSQLiteStorage {

	@Test
	void testBasicOps() throws StorageException {
		DatabaseEngine engine = new DatabaseEngine(new File("target/test.db"));
		Storage storage = new SQLiteStorage(engine);

		assertEquals(Collections.emptyList(), storage.getCoresWithStorage());

		HasCoreLocation core = new CoreLocation(0, 0, 0);
		storage.storeRegionContents(core, 0, "abc".getBytes(UTF_8));
		storage.appendRegionContents(core, 0, "def".getBytes(UTF_8));

		assertArrayEquals("abcdef".getBytes(UTF_8),
				storage.getRegionContents(core, 0));
		assertEquals(Arrays.asList(core), storage.getCoresWithStorage());
		assertEquals(Arrays.asList(0), storage.getRegionsWithStorage(core));

		storage.deleteRegionContents(core, 0);

		assertEquals(Collections.emptyList(), storage.getCoresWithStorage());
	}

}
