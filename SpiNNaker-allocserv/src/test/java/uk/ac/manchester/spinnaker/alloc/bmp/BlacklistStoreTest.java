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

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.BOARD;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.setupDB1;
import static uk.ac.manchester.spinnaker.alloc.db.Row.enumerate;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.machine.Direction.EAST;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTH;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTHEAST;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTH;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTHWEST;
import static uk.ac.manchester.spinnaker.machine.Direction.WEST;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connected;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;

@SpringBootTest
@SpringJUnitWebConfig(BlacklistStoreTest.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + BlacklistStoreTest.DB,
	"spalloc.historical-data.path=" + BlacklistStoreTest.HIST_DB
})
class BlacklistStoreTest extends SQLQueries {
	/** The DB file. */
	static final String DB = "target/blio_test.sqlite3";

	/** The DB file. */
	static final String HIST_DB = "target/blio_test_hist.sqlite3";

	private static final Logger log = getLogger(BlacklistStoreTest.class);

	@Configuration
	@ComponentScan(basePackageClasses = SpallocProperties.class)
	static class Config {
	}

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private BlacklistStore blio;

	private BlacklistStore.TestAPI testAPI;

	@BeforeAll
	static void clearDB() throws IOException {
		Path dbp = Paths.get(DB);
		if (exists(dbp)) {
			log.info("deleting old database: {}", dbp);
			delete(dbp);
		}
	}

	@SafeVarargs
	private static <T> Set<T> set(T... args) {
		// TODO replace with Set.of() in Java 11 onwards
		return new HashSet<>(Arrays.asList(args));
	}

	@SuppressWarnings("unchecked")
	private static <T> Map<ChipLocation, Set<T>> map(Object... args) {
		// TODO replace with Map.of() in Java 11 onwards
		Map<ChipLocation, Set<T>> map = new HashMap<>();
		for (int i = 0; i < args.length; i += 2) {
			ChipLocation key = (ChipLocation) args[i];
			Set<T> value = (Set<T>) args[i + 1];
			map.put(key, value);
		}
		return map;
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
	void checkSetup() {
		assumeTrue(db != null, "spring-configured DB engine absent");
		try (Connection c = db.getConnection()) {
			c.transaction(() -> setupDB1(c));
		}
		// Get at the internal API so we can control transaction boundaries
		testAPI = blio.getTestAPI();
	}

	private void checkAndRollback(Connected act) {
		db.executeVoid(c -> {
			try {
				act.act(c);
			} finally {
				c.rollback();
			}
		});
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
			try (Update u = c.update(ADD_BLACKLISTED_CHIP)) {
				assertEquals(1, u.call(BOARD, 1, 1));
			}

			Blacklist bl = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(new Blacklist(set(C11), emptyMap(), emptyMap()), bl);
		});
	}

	@Test
	void readDBWithBlacklistedChipsPresent() {
		checkAndRollback(c -> {
			try (Update u = c.update(ADD_BLACKLISTED_CHIP)) {
				assertEquals(1, u.call(BOARD, 1, 0));
				assertEquals(1, u.call(BOARD, 0, 1));
			}

			Blacklist bl = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(new Blacklist(set(C01, C10), emptyMap(), emptyMap()),
					bl);
		});
	}

	@Test
	void readDBWithBlacklistedCorePresent() {
		checkAndRollback(c -> {
			try (Update u = c.update(ADD_BLACKLISTED_CORE)) {
				assertEquals(1, u.call(BOARD, 1, 1, 15));
			}

			Blacklist bl = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(new Blacklist(set(), map(C11, set(15)), emptyMap()),
					bl);
		});
	}

	@Test
	void readDBWithBlacklistedCoresPresent() {
		checkAndRollback(c -> {
			try (Update u = c.update(ADD_BLACKLISTED_CORE)) {
				assertEquals(1, u.call(BOARD, 0, 1, 16));
				assertEquals(1, u.call(BOARD, 1, 0, 14));
			}

			Blacklist bl = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(new Blacklist(set(), map(C01, set(16), C10, set(14)),
					emptyMap()), bl);
		});
	}

	@Test
	void readDBWithBlacklistedLinkPresent() {
		checkAndRollback(c -> {
			try (Update u = c.update(ADD_BLACKLISTED_LINK)) {
				assertEquals(1, u.call(BOARD, 1, 1, WEST));
			}

			Blacklist bl = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(new Blacklist(set(), emptyMap(), map(C11, set(WEST))),
					bl);
		});
	}

	@Test
	void readDBWithBlacklistedLinksPresent() {
		checkAndRollback(c -> {
			try (Update u = c.update(ADD_BLACKLISTED_LINK)) {
				assertEquals(1, u.call(BOARD, 0, 1, NORTH));
				assertEquals(1, u.call(BOARD, 1, 0, SOUTH));
			}

			Blacklist bl = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(new Blacklist(set(), emptyMap(),
					map(C01, set(NORTH), C10, set(SOUTH))), bl);
		});
	}

	@Test
	void readDBWithMultipleBlacklistStuffPresent() {
		checkAndRollback(c -> {
			try (Update chip = c.update(ADD_BLACKLISTED_CHIP);
					Update core = c.update(ADD_BLACKLISTED_CORE);
					Update link = c.update(ADD_BLACKLISTED_LINK)) {
				assertEquals(1, chip.call(BOARD, 1, 0));
				assertEquals(1, chip.call(BOARD, 0, 1));
				assertEquals(1, core.call(BOARD, 0, 2, 16));
				assertEquals(1, core.call(BOARD, 2, 0, 14));
				assertEquals(1, link.call(BOARD, 0, 3, NORTH));
				assertEquals(1, link.call(BOARD, 3, 0, SOUTH));
			}

			Blacklist bl = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(new Blacklist(set(C10, C01),
					map(new ChipLocation(0, 2), set(16), new ChipLocation(2, 0),
							set(14)),
					map(new ChipLocation(0, 3), set(NORTH),
							new ChipLocation(3, 0), set(SOUTH))),
					bl);
		});
	}

	@Test
	void writeDB() {
		checkAndRollback(c -> {
			Blacklist bl = new Blacklist(set(C01, C11), map(C10, set(1, 2, 3)),
					map(C77, set(NORTH, SOUTH, EAST, WEST)));

			testAPI.writeBlacklist(c, BOARD, bl);

			// Check the results by looking in the DB ourselves
			try (Query chips = c.query(GET_BLACKLISTED_CHIPS);
					Query cores = c.query(GET_BLACKLISTED_CORES);
					Query links = c.query(GET_BLACKLISTED_LINKS)) {
				assertEquals(set(C11, C01), chips.call(BOARD)
						.map(BlacklistStoreTest::coords).toSet());
				assertEquals(map(C10, set(3, 2, 1)),
						cores.call(BOARD).toCollectingMap(HashMap::new,
								HashSet::new, BlacklistStoreTest::coords,
								integer("p")));
				assertEquals(map(C77, set(NORTH, SOUTH, EAST, WEST)),
						links.call(BOARD).toCollectingMap(HashMap::new,
								HashSet::new, BlacklistStoreTest::coords,
								enumerate("direction", Direction.class)));
			}
		});
	}

	@Test
	void rewriteDB() {
		checkAndRollback(c -> {
			Blacklist bl1 = new Blacklist(set(C01, C11), map(C10, set(1, 2, 3)),
					map(C77, set(NORTH, SOUTH, EAST, WEST)));
			Blacklist bl2 = new Blacklist(set(C10, C77), map(C01, set(4, 5, 6)),
					map(C11, set(NORTHEAST, SOUTHWEST)));

			testAPI.writeBlacklist(c, BOARD, bl1);
			testAPI.writeBlacklist(c, BOARD, bl2);

			// Check the results by looking in the DB ourselves
			try (Query chips = c.query(GET_BLACKLISTED_CHIPS);
					Query cores = c.query(GET_BLACKLISTED_CORES);
					Query links = c.query(GET_BLACKLISTED_LINKS)) {
				assertEquals(set(C77, C10), chips.call(BOARD)
						.map(BlacklistStoreTest::coords).toSet());
				assertEquals(map(C01, set(6, 4, 5)),
						cores.call(BOARD).toCollectingMap(HashMap::new,
								HashSet::new, BlacklistStoreTest::coords,
								integer("p")));
				assertEquals(map(C11, set(SOUTHWEST, NORTHEAST)),
						links.call(BOARD).toCollectingMap(HashMap::new,
								HashSet::new, BlacklistStoreTest::coords,
								enumerate("direction", Direction.class)));
			}
		});
	}

	@Test
	void rewriteToEmptyDB() {
		checkAndRollback(c -> {
			Blacklist bl1 = new Blacklist(set(C01, C11), map(C10, set(1, 2, 3)),
					map(C77, set(NORTH, SOUTH, EAST, WEST)));
			Blacklist bl2 = new Blacklist(emptySet(), emptyMap(), emptyMap());

			testAPI.writeBlacklist(c, BOARD, bl1);
			testAPI.writeBlacklist(c, BOARD, bl2);

			// Check the results by looking in the DB ourselves
			try (Query chips = c.query(GET_BLACKLISTED_CHIPS);
					Query cores = c.query(GET_BLACKLISTED_CORES);
					Query links = c.query(GET_BLACKLISTED_LINKS)) {
				assertEquals(emptySet(), chips.call(BOARD)
						.map(BlacklistStoreTest::coords).toSet());
				assertEquals(emptyMap(),
						cores.call(BOARD).toCollectingMap(HashMap::new,
								HashSet::new, BlacklistStoreTest::coords,
								integer("p")));
				assertEquals(emptyMap(),
						links.call(BOARD).toCollectingMap(HashMap::new,
								HashSet::new, BlacklistStoreTest::coords,
								enumerate("direction", Direction.class)));
			}
		});
	}

	@Test
	void writeAndReadBack() {
		checkAndRollback(c -> {
			Blacklist blIn =
					new Blacklist(set(C01, C11), map(C10, set(1, 2, 3)),
							map(C77, set(NORTH, SOUTH, EAST, WEST)));

			testAPI.writeBlacklist(c, BOARD, blIn);
			Blacklist blOut = testAPI.readBlacklist(c, BOARD).get();

			assertEquals(blIn, blOut);
		});
	}
}
