/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import uk.ac.manchester.spinnaker.alloc.db.SimpleDBTestBase;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.storage.ResultColumn;
import uk.ac.manchester.spinnaker.storage.SingleRowResult;

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
class MDefLoaderTest extends SimpleDBTestBase {
	@ResultColumn("c")
	@SingleRowResult
	private static final String COUNT_LIVE_BOARDS =
			"SELECT COUNT(*) AS c FROM boards WHERE board_num IS NOT NULL";

	@ResultColumn("c")
	@SingleRowResult
	private static final String COUNT_LIVE_LINKS =
			"SELECT COUNT(*) AS c FROM links WHERE live";

	@Autowired
	private MachineDefinitionLoader loader;

	@Value("classpath:single-board-example.json")
	private Resource singleBoard;

	@Value("classpath:three-board-example.json")
	private Resource threeBoard;

	@Value("classpath:bad-board-example.json")
	private Resource badBoard;

	@Value("classpath:bad-board-example2.json")
	private Resource badBoard2;

	@Test
	void readSingleBoardExample() throws IOException {
		var machines = loader.readMachineDefinitions(singleBoard.getFile());

		assertNotNull(machines);
		assertEquals(1, machines.size());
		var m = machines.get(0);
		assertEquals("my-board", m.getName());
		assertEquals(Set.of(new TriadCoords(0, 0, 0)),
				m.getBoardLocations().keySet());
		assertEquals(Set.of(new TriadCoords(0, 0, 0)),
				m.getSpinnakerIPs().keySet());
		assertEquals(Set.of(new BMPCoords(0, 0)), m.getBmpIPs().keySet());
		assertEquals(Set.of(new PhysicalCoords(0, 0, 0)),
				Set.copyOf(m.getBoardLocations().values()));
	}

	@Test
	void readBadBoardExample() {
		var e = assertThrows(IOException.class,
				() -> loader.readMachineDefinitions(badBoard.getFile()));
		assertEquals(
				"failed to validate configuration: "
						+ "'1.2.3.4.5.not-an-ip' is a bad IPv4 address",
				e.getMessage());
	}

	@Test
	void readBadBoardExample2() {
		var e = assertThrows(IOException.class,
				() -> loader.readMachineDefinitions(badBoard2.getFile()));
		assertEquals(
				"failed to validate configuration: "
						+ "'1.2.3.4.5.6.not-an-ip' is a bad IPv4 address",
				e.getMessage());
	}

	@Test
	@SuppressWarnings("deprecation")
	void loadSingleBoardExample() throws IOException {
		var machines = loader.readMachineDefinitions(singleBoard.getFile());
		assumeTrue(machines.size() == 1);
		var machine = machines.get(0);
		assumeTrue(machine != null);

		c.transaction(() -> {
			loader.getTestAPI(c).loadMachineDefinition(machine);
		});

		c.transaction(() -> {
			try (var q = c.query("SELECT machine_name FROM machines")) {
				int rows = 0;
				for (var row : q.call(string("machine_name"))) {
					assertEquals("my-board", row);
					rows++;
				}
				assertEquals(1, rows);
			}

			// Should be just one BMP
			try (var q = c.query("SELECT COUNT(*) AS c FROM bmp")) {
				assertEquals(1, q.call1(integer("c")).orElseThrow());
			}

			// Should be just one board
			try (var q = c.query(COUNT_LIVE_BOARDS)) {
				assertEquals(1, q.call1(integer("c")).orElseThrow());
			}

			// Single-board setups have no inter-board links
			try (var q = c.query(COUNT_LIVE_LINKS)) {
				assertEquals(0, q.call1(integer("c")).orElseThrow());
			}
		});
	}

	@Test
	@SuppressWarnings("deprecation")
	void loadThreeBoardExample() throws IOException {
		var machines = loader.readMachineDefinitions(threeBoard.getFile());
		assumeTrue(machines.size() == 1);
		var machine = machines.get(0);
		assumeTrue(machine != null);

		c.transaction(() -> {
			loader.getTestAPI(c).loadMachineDefinition(machine);
		});

		c.transaction(() -> {
			try (var q = c.query("SELECT machine_name FROM machines")) {
				int rows = 0;
				for (var row : q.call(string("machine_name"))) {
					assertEquals("SpiNNaker3board", row);
					rows++;
				}
				assertEquals(1, rows);
			}

			// Should be just one BMP
			try (var q = c.query("SELECT COUNT(*) AS c FROM bmp")) {
				assertEquals(1, q.call1(integer("c")).orElseThrow());
			}

			// Should be just one board
			try (var q = c.query(COUNT_LIVE_BOARDS)) {
				assertEquals(3, q.call1(integer("c")).orElseThrow());
			}

			// Single-board setups have no inter-board links
			try (var q = c.query(COUNT_LIVE_LINKS)) {
				assertEquals(9, q.call1(integer("c")).orElseThrow());
			}
		});
	}
}
