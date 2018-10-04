package uk.ac.manchester.spinnaker.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

class TestSQLiteStorage {

	@Test
	void testBasicOps() throws SQLException {
		DatabaseEngine engine = new DatabaseEngine(new File("target/test.db"));
		SQLiteStorage storage = new SQLiteStorage(engine);

		assertEquals(Collections.emptyList(), storage.getCores());

		HasCoreLocation core = new CoreLocation(0, 0, 0);
		storage.storeRegionContents(core, 0, "abc".getBytes(UTF_8));
		storage.appendRegionContents(core, 0, "def".getBytes(UTF_8));

		assertArrayEquals("abcdef".getBytes(UTF_8),
				storage.getRegionContents(core, 0));
		assertEquals(Arrays.asList(core), storage.getCores());
		assertEquals(Arrays.asList(0), storage.getRegions(core));

		storage.deleteRegionContents(core, 0);

		assertEquals(Collections.emptyList(), storage.getCores());
	}

}
