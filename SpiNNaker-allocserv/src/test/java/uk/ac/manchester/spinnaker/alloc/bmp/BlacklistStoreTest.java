/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.bmp;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static uk.ac.manchester.spinnaker.alloc.db.Row.enumerate;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.machine.Direction.EAST;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTH;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTHEAST;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTH;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTHWEST;
import static uk.ac.manchester.spinnaker.machine.Direction.WEST;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.TestSupport;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;

@SpringBootTest
@SpringJUnitWebConfig(TestSupport.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + BlacklistStoreTest.DB,
	"spalloc.historical-data.path=" + BlacklistStoreTest.HIST_DB
})
class BlacklistStoreTest extends TestSupport {
	/** The DB file. */
	static final String DB = "target/blio_test.sqlite3";

	/** The DB file. */
	static final String HIST_DB = "target/blio_test_hist.sqlite3";

	private BlacklistStore.TestAPI testAPI;

	@BeforeAll
	static void clearDB() throws IOException {
		killDB(DB);
	}

	private static final ChipLocation C01 = new ChipLocation(0, 1);

	private static final ChipLocation C10 = new ChipLocation(1, 0);

	private static final ChipLocation C11 = new ChipLocation(1, 1);

	private static final ChipLocation C77 = new ChipLocation(7, 7);

	private static ChipLocation coords(Row r) {
		return new ChipLocation(r.getInt("x"), r.getInt("y"));
	}

	@BeforeEach
	@SuppressWarnings("deprecation") // Calling internal API
	void checkSetup(@Autowired BlacklistStore blio) {
		assumeTrue(db != null, "spring-configured DB engine absent");
		setupDB1();
		// Get at the internal API so we can control transaction boundaries
		testAPI = blio.getTestAPI();
	}

	@Test
	void readDBNoBlacklistPresent() {
		checkAndRollback(c -> {
			assertFalse(testAPI.readBlacklist(c, BOARD).isPresent());
		});
	}

	@Test
	void readDBWithBlacklistedChipPresent() {
		checkAndRollback(c -> {
			try (var u = c.update(ADD_BLACKLISTED_CHIP)) {
				assertEquals(1, u.call(BOARD, 1, 1));
			}

			var bl = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(new Blacklist(Set.of(C11), Map.of(), Map.of()), bl);
		});
	}

	@Test
	void readDBWithBlacklistedChipsPresent() {
		checkAndRollback(c -> {
			try (var u = c.update(ADD_BLACKLISTED_CHIP)) {
				assertEquals(1, u.call(BOARD, 1, 0));
				assertEquals(1, u.call(BOARD, 0, 1));
			}

			var bl = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(new Blacklist(Set.of(C01, C10), Map.of(), Map.of()),
					bl);
		});
	}

	@Test
	void readDBWithBlacklistedCorePresent() {
		checkAndRollback(c -> {
			try (var u = c.update(ADD_BLACKLISTED_CORE)) {
				assertEquals(1, u.call(BOARD, 1, 1, 15));
			}

			var bl = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(
					new Blacklist(Set.of(), Map.of(C11, Set.of(15)), Map.of()),
					bl);
		});
	}

	@Test
	void readDBWithBlacklistedCoresPresent() {
		checkAndRollback(c -> {
			try (var u = c.update(ADD_BLACKLISTED_CORE)) {
				assertEquals(1, u.call(BOARD, 0, 1, 16));
				assertEquals(1, u.call(BOARD, 1, 0, 14));
			}

			var bl = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(
					new Blacklist(Set.of(),
							Map.of(C01, Set.of(16), C10, Set.of(14)), Map.of()),
					bl);
		});
	}

	@Test
	void readDBWithBlacklistedLinkPresent() {
		checkAndRollback(c -> {
			try (var u = c.update(ADD_BLACKLISTED_LINK)) {
				assertEquals(1, u.call(BOARD, 1, 1, WEST));
			}

			var bl = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(new Blacklist(Set.of(), Map.of(),
					Map.of(C11, Set.of(WEST))), bl);
		});
	}

	@Test
	void readDBWithBlacklistedLinksPresent() {
		checkAndRollback(c -> {
			try (var u = c.update(ADD_BLACKLISTED_LINK)) {
				assertEquals(1, u.call(BOARD, 0, 1, NORTH));
				assertEquals(1, u.call(BOARD, 1, 0, SOUTH));
			}

			var bl = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(new Blacklist(Set.of(), Map.of(),
					Map.of(C01, Set.of(NORTH), C10, Set.of(SOUTH))), bl);
		});
	}

	@Test
	void readDBWithMultipleBlacklistStuffPresent() {
		checkAndRollback(c -> {
			try (var chip = c.update(ADD_BLACKLISTED_CHIP);
					var core = c.update(ADD_BLACKLISTED_CORE);
					var link = c.update(ADD_BLACKLISTED_LINK)) {
				assertEquals(1, chip.call(BOARD, 1, 0));
				assertEquals(1, chip.call(BOARD, 0, 1));
				assertEquals(1, core.call(BOARD, 0, 2, 16));
				assertEquals(1, core.call(BOARD, 2, 0, 14));
				assertEquals(1, link.call(BOARD, 0, 3, NORTH));
				assertEquals(1, link.call(BOARD, 3, 0, SOUTH));
			}

			var bl = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(
					new Blacklist(Set.of(C10, C01),
							Map.of(new ChipLocation(0, 2), Set.of(16),
									new ChipLocation(2, 0), Set.of(14)),
							Map.of(new ChipLocation(0, 3), Set.of(NORTH),
									new ChipLocation(3, 0), Set.of(SOUTH))),
					bl);
		});
	}

	@Test
	void writeDB() {
		checkAndRollback(c -> {
			var bl = new Blacklist(Set.of(C01, C11),
					Map.of(C10, Set.of(1, 2, 3)),
					Map.of(C77, Set.of(NORTH, SOUTH, EAST, WEST)));

			testAPI.writeBlacklist(c, BOARD, bl);

			// Check the results by looking in the DB ourselves
			try (var chips = c.query(GET_BLACKLISTED_CHIPS);
					var cores = c.query(GET_BLACKLISTED_CORES);
					var links = c.query(GET_BLACKLISTED_LINKS)) {
				assertEquals(Set.of(C11, C01), chips.call(BOARD)
						.map(BlacklistStoreTest::coords).toSet());
				assertEquals(Map.of(C10, Set.of(3, 2, 1)),
						cores.call(BOARD).toCollectingMap(HashMap::new,
								HashSet::new, BlacklistStoreTest::coords,
								integer("p")));
				assertEquals(Map.of(C77, Set.of(NORTH, SOUTH, EAST, WEST)),
						links.call(BOARD).toCollectingMap(HashMap::new,
								HashSet::new, BlacklistStoreTest::coords,
								enumerate("direction", Direction.class)));
			}
		});
	}

	@Test
	void rewriteDB() {
		checkAndRollback(c -> {
			var bl1 = new Blacklist(Set.of(C01, C11),
					Map.of(C10, Set.of(1, 2, 3)),
					Map.of(C77, Set.of(NORTH, SOUTH, EAST, WEST)));
			var bl2 = new Blacklist(Set.of(C10, C77),
					Map.of(C01, Set.of(4, 5, 6)),
					Map.of(C11, Set.of(NORTHEAST, SOUTHWEST)));

			testAPI.writeBlacklist(c, BOARD, bl1);
			testAPI.writeBlacklist(c, BOARD, bl2);

			// Check the results by looking in the DB ourselves
			try (var chips = c.query(GET_BLACKLISTED_CHIPS);
					var cores = c.query(GET_BLACKLISTED_CORES);
					var links = c.query(GET_BLACKLISTED_LINKS)) {
				assertEquals(Set.of(C77, C10), chips.call(BOARD)
						.map(BlacklistStoreTest::coords).toSet());
				assertEquals(Map.of(C01, Set.of(6, 4, 5)),
						cores.call(BOARD).toCollectingMap(HashMap::new,
								HashSet::new, BlacklistStoreTest::coords,
								integer("p")));
				assertEquals(Map.of(C11, Set.of(SOUTHWEST, NORTHEAST)),
						links.call(BOARD).toCollectingMap(HashMap::new,
								HashSet::new, BlacklistStoreTest::coords,
								enumerate("direction", Direction.class)));
			}
		});
	}

	@Test
	void rewriteToEmptyDB() {
		checkAndRollback(c -> {
			var bl1 = new Blacklist(Set.of(C01, C11),
					Map.of(C10, Set.of(1, 2, 3)),
					Map.of(C77, Set.of(NORTH, SOUTH, EAST, WEST)));
			var bl2 = new Blacklist(Set.of(), Map.of(), Map.of());

			testAPI.writeBlacklist(c, BOARD, bl1);
			testAPI.writeBlacklist(c, BOARD, bl2);

			// Check the results by looking in the DB ourselves
			try (var chips = c.query(GET_BLACKLISTED_CHIPS);
					var cores = c.query(GET_BLACKLISTED_CORES);
					var links = c.query(GET_BLACKLISTED_LINKS)) {
				assertEquals(Set.of(), chips.call(BOARD)
						.map(BlacklistStoreTest::coords).toSet());
				assertEquals(Map.of(),
						cores.call(BOARD).toCollectingMap(HashMap::new,
								HashSet::new, BlacklistStoreTest::coords,
								integer("p")));
				assertEquals(Map.of(),
						links.call(BOARD).toCollectingMap(HashMap::new,
								HashSet::new, BlacklistStoreTest::coords,
								enumerate("direction", Direction.class)));
			}
		});
	}

	@Test
	void writeAndReadBack() {
		checkAndRollback(c -> {
			var blIn = new Blacklist(Set.of(C01, C11),
					Map.of(C10, Set.of(1, 2, 3)),
					Map.of(C77, Set.of(NORTH, SOUTH, EAST, WEST)));

			testAPI.writeBlacklist(c, BOARD, blIn);
			var blOut = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(blIn, blOut);
		});
	}
}
