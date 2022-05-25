/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.admin;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.BMPCoords;
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.BoardPhysicalCoords;
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.TriadCoords;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;

/**
 * Test that the database engine interface works and that the queries are
 * synchronised with the schema. Deliberately does not do meaningful testing of
 * the data in the database.
 *
 * @author Donal Fellows
 */
@SpringBootTest
@TestInstance(PER_CLASS)
@ActiveProfiles("unittest")
class MDefLoaderTest {
	private static final String COUNT_LIVE_BOARDS =
			"SELECT COUNT(*) AS c FROM boards WHERE board_num IS NOT NULL";

	private static final String COUNT_LIVE_LINKS =
			"SELECT COUNT(*) AS c FROM links WHERE live";

	@Autowired
	private MachineDefinitionLoader loader;

	@Autowired
	private DatabaseEngine mainDBEngine;

	@Value("classpath:single-board-example.json")
	private Resource singleBoard;

	@Value("classpath:three-board-example.json")
	private Resource threeBoard;

	private DatabaseEngine memdb;

	private Connection c;

	private static <T extends Comparable<T>> void
			assertSetEquals(Set<T> expected, Set<T> actual) {
		var e = new ArrayList<>(expected);
		Collections.sort(e);
		var a = new ArrayList<>(actual);
		Collections.sort(a);
		assertEquals(e, a);
	}

	@BeforeAll
	void makeMemoryDatabase() {
		assumeTrue(mainDBEngine != null, "spring-configured DB engine absent");
		memdb = mainDBEngine.getInMemoryDB();
	}

	@BeforeEach
	void getConnection() {
		c = memdb.getConnection();
		assumeTrue(c != null, "connection not generated");
	}

	@AfterEach
	void closeConnection() {
		c.close();
	}

	@SuppressWarnings("unchecked")
	private static <T> Set<T> set(T... members) {
		return unmodifiableSet(new HashSet<>(asList(members)));
	}

	@Test
	void readSingleBoardExample() throws IOException {
		var machines = loader.readMachineDefinitions(singleBoard.getFile());

		assertNotNull(machines);
		assertEquals(1, machines.size());
		var m = machines.get(0);
		assertEquals("my-board", m.getName());
		assertSetEquals(set(new TriadCoords(0, 0, 0)),
				m.getBoardLocations().keySet());
		assertSetEquals(set(new TriadCoords(0, 0, 0)),
				m.getSpinnakerIPs().keySet());
		assertSetEquals(set(new BMPCoords(0, 0)), m.getBmpIPs().keySet());
		assertSetEquals(set(new BoardPhysicalCoords(0, 0, 0)),
				new HashSet<>(m.getBoardLocations().values()));
	}

	@Test
	void loadSingleBoardExample() throws IOException {
		var machines = loader.readMachineDefinitions(singleBoard.getFile());
		assumeTrue(machines != null && machines.size() == 1);
		@SuppressWarnings("null")
		var machine = machines.get(0);
		assumeTrue(machine != null);

		c.transaction(() -> {
			try (var q = loader.new Updates(c)) {
				loader.loadMachineDefinition(q, machine);
			}
		});

		c.transaction(() -> {
			try (var q = c.query("SELECT machine_name FROM machines")) {
				int rows = 0;
				for (var row : q.call()) {
					assertEquals("my-board", row.getString("machine_name"));
					rows++;
				}
				assertEquals(1, rows);
			}

			// Should be just one BMP
			try (var q = c.query("SELECT COUNT(*) AS c FROM bmp")) {
				assertEquals(1, q.call1().get().getInt("c"));
			}

			// Should be just one board
			try (var q = c.query(COUNT_LIVE_BOARDS)) {
				assertEquals(1, q.call1().get().getInt("c"));
			}

			// Single-board setups have no inter-board links
			try (var q = c.query(COUNT_LIVE_LINKS)) {
				assertEquals(0, q.call1().get().getInt("c"));
			}
		});
	}

	@Test
	void loadThreeBoardExample() throws IOException {
		var machines = loader.readMachineDefinitions(threeBoard.getFile());
		assumeTrue(machines != null && machines.size() == 1);
		@SuppressWarnings("null")
		var machine = machines.get(0);
		assumeTrue(machine != null);

		c.transaction(() -> {
			try (var q = loader.new Updates(c)) {
				loader.loadMachineDefinition(q, machine);
			}
		});

		c.transaction(() -> {
			try (var q = c.query("SELECT machine_name FROM machines")) {
				int rows = 0;
				for (var row : q.call()) {
					assertEquals("SpiNNaker3board",
							row.getString("machine_name"));
					rows++;
				}
				assertEquals(1, rows);
			}

			// Should be just one BMP
			try (var q = c.query("SELECT COUNT(*) AS c FROM bmp")) {
				assertEquals(1, q.call1().get().getInt("c"));
			}

			// Should be just one board
			try (var q = c.query(COUNT_LIVE_BOARDS)) {
				assertEquals(3, q.call1().get().getInt("c"));
			}

			// Single-board setups have no inter-board links
			try (var q = c.query(COUNT_LIVE_LINKS)) {
				assertEquals(9, q.call1().get().getInt("c"));
			}
		});
	}
}
