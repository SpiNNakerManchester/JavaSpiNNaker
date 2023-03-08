/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.bmp;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static uk.ac.manchester.spinnaker.alloc.db.Row.stream;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.TestSupport;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;

@SpringBootTest
@SpringJUnitWebConfig(TestSupport.Config.class)
@ActiveProfiles("unittest")
class BlacklistStoreTest extends TestSupport {

	private BlacklistStore.TestAPI testAPI;

	private static final ChipLocation C01 = new ChipLocation(0, 1);

	private static final ChipLocation C10 = new ChipLocation(1, 0);

	private static final ChipLocation C11 = new ChipLocation(1, 1);

	private static final ChipLocation C77 = new ChipLocation(7, 7);

	private static ChipLocation coords(Row r) {
		return new ChipLocation(r.getInt("x"), r.getInt("y"));
	}

	private static CoreLocation coreCoords(Row r) {
		return new CoreLocation(r.getInt("x"), r.getInt("y"), r.getInt("p"));
	}

	private class LinkLocation {
		ChipLocation location;

		Direction direction;

		LinkLocation(Row r) {
			location = coords(r);
			direction = r.getEnum("direction", Direction.class);
		}
	}

	@BeforeEach
	@SuppressWarnings("deprecation") // Calling internal API
	void checkSetup(@Autowired BlacklistStore blio) throws IOException {
		assumeTrue(db != null, "spring-configured DB engine absent");
		killDB();
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

			var bl = testAPI.readBlacklist(c, BOARD).orElseThrow();

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

			var bl = testAPI.readBlacklist(c, BOARD).orElseThrow();

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

			var bl = testAPI.readBlacklist(c, BOARD).orElseThrow();

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

			var bl = testAPI.readBlacklist(c, BOARD).orElseThrow();

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

			var bl = testAPI.readBlacklist(c, BOARD).orElseThrow();

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

			var bl = testAPI.readBlacklist(c, BOARD).orElseThrow();

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

			var bl = testAPI.readBlacklist(c, BOARD).orElseThrow();

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
				assertEquals(Set.of(C11, C01),
						stream(chips.call(BlacklistStoreTest::coords, BOARD))
						.toSet());
				assertEquals(Map.of(C10, Set.of(3, 2, 1)),
						stream(cores.call(BlacklistStoreTest::coreCoords,
								BOARD))
						.toCollectingMap(
								HashMap::new, HashSet::new,
								coords -> coords.asChipLocation(),
								coords -> coords.getP()));
				assertEquals(Map.of(C77, Set.of(NORTH, SOUTH, EAST, WEST)),
						stream(links.call(LinkLocation::new, BOARD)).
						toCollectingMap(HashMap::new,
								HashSet::new, l -> l.location,
								l -> l.direction));
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
				assertEquals(Set.of(C77, C10),
						stream(chips.call(BlacklistStoreTest::coords, BOARD))
						.toSet());
				assertEquals(Map.of(C01, Set.of(6, 4, 5)),
						stream(cores.call(BlacklistStoreTest::coreCoords,
								BOARD))
						.toCollectingMap(HashMap::new,
								HashSet::new, coords -> coords.asChipLocation(),
								coords -> coords.getP()));
				assertEquals(Map.of(C11, Set.of(SOUTHWEST, NORTHEAST)),
						stream(links.call(LinkLocation::new, BOARD))
						.toCollectingMap(
								HashMap::new, HashSet::new, l -> l.location,
								l -> l.direction));
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
				assertEquals(Set.of(),
						stream(chips.call(BlacklistStoreTest::coords, BOARD))
						.toSet());
				assertEquals(Map.of(),
						stream(cores.call(BlacklistStoreTest::coreCoords,
								BOARD))
						.toCollectingMap(HashMap::new, HashSet::new,
								coords -> coords.asChipLocation(),
								coords -> coords.getP()));
				assertEquals(Map.of(),
						stream(links.call(LinkLocation::new, BOARD))
						.toCollectingMap(HashMap::new,
								HashSet::new, l -> l.location,
								l -> l.direction));
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
			var blOut = testAPI.readBlacklist(c, BOARD).orElseThrow();

			assertEquals(blIn, blOut);
		});
	}
}
