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
import static org.junit.jupiter.api.Assumptions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.exec;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import org.sqlite.SQLiteException;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.BMPCoords;
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.Machine;
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.TriadCoords;
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.BoardPhysicalCoords;

/**
 * Test that the database engine interface works and that the queries are
 * synchronised with the schema. Deliberately does not do meaningful testing of
 * the data in the database.
 *
 * @author Donal Fellows
 */
@SpringBootTest
@TestInstance(PER_CLASS)
class MDefLoaderTest {
	// Not equal to any machine_id
	private static final int NO_MACHINE = -1;

	// Not equal to any job_id
	private static final int NO_JOB = -1;

	// Not equal to any board_id
	private static final int NO_BOARD = -1;

	// Not equal to any change_id
	private static final int NO_CHANGE = -1;

	@Autowired
	private MachineDefinitionLoader loader;

	@Autowired
	private DatabaseEngine mainDBEngine;

	@Value("classpath:single-board-example.json")
	private Resource singleBoard;

	private DatabaseEngine memdb;

	private Connection c;

	private static <T extends Comparable<T>> void assertSetEquals(
			Set<T> expected, Set<T> actual) {
		List<T> e = new ArrayList<>(expected);
		Collections.sort(e);
		List<T> a = new ArrayList<>(actual);
		Collections.sort(a);
		assertEquals(e, a);
	}

	@BeforeAll
	void makeMemoryDatabase() {
		assumeTrue(mainDBEngine != null, "spring-configured DB engine absent");
		memdb = new DatabaseEngine(mainDBEngine);
	}

	@BeforeEach
	void getConnection() throws SQLException {
		c = memdb.getConnection();
		assumeTrue(c != null, "connection not generated");
	}

	@AfterEach
	void closeConnection() throws SQLException {
		c.close();
	}

	@SuppressWarnings("unchecked")
	private static <T> Set<T> set(T... members) {
		return unmodifiableSet(new HashSet<>(asList(members)));
	}

	@Test
	void loadSingleBoardExample() throws IOException {
		List<Machine> machines =
				loader.loadMachineDefinitions(singleBoard.getFile());

		assertNotNull(machines);
		assertEquals(1, machines.size());
		Machine m = machines.get(0);
		assertEquals("my-board", m.getName());
		assertSetEquals(set(new TriadCoords(0, 0, 0)),
				m.getBoardLocations().keySet());
		assertSetEquals(set(new TriadCoords(0, 0, 0)),
				m.getSpinnakerIPs().keySet());
		assertSetEquals(set(new BMPCoords(0, 0)), m.getBmpIPs().keySet());
		assertSetEquals(
				set(new BoardPhysicalCoords(0, 0, 0)),
				new HashSet<>(m.getBoardLocations().values()));
	}
}
