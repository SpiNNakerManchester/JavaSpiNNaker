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
package uk.ac.manchester.spinnaker.alloc;

import static java.lang.String.format;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_CHECK;
import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.exec;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.sqlite.SQLiteException;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl.BoardState;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.alloc.model.JobState;

/**
 * Test that the database engine interface works and that the queries are
 * synchronised with the schema. Deliberately does not do meaningful testing of
 * the data in the database.
 *
 * @author Donal Fellows
 */
@SpringBootTest
@TestInstance(PER_CLASS)
class DbTest {
	// Not equal to any machine_id
	private static final int NO_MACHINE = -1;

	// Not equal to any job_id
	private static final int NO_JOB = -1;

	// Not equal to any board_id
	private static final int NO_BOARD = -1;

	// Not equal to any bmp_id
	private static final int NO_BMP = -1;

	// Not equal to any change_id
	private static final int NO_CHANGE = -1;

	// Not equal to any user_id
	private static final int NO_USER = -1;

	/** The columns the {@link BoardLocation} constructor expects to find. */
	private static final Set<String> BOARD_LOCATION_REQUIRED_COLUMNS =
			set("machine_name", "x", "y", "z", "cabinet", "frame", "board_num",
					"chip_x", "chip_y", "board_chip_x", "board_chip_y",
					"job_id", "job_root_chip_x", "job_root_chip_y");

	/** Columns expected when building {@link BoardState} from a {@link Row}. */
	private static final Set<String> MSC_BOARD_COORDS = set("board_id", "x",
			"y", "z", "cabinet", "frame", "board_num", "address");

	/**
	 * Columns expected when building {@link BoardCoords} from a {@link Row}.
	 */
	private static final Set<String> BOARD_COORDS_REQUIRED_COLUMNS =
			set("x", "y", "z", "cabinet", "frame", "board_num", "address");

	@Autowired
	private DatabaseEngine mainDBEngine;

	private DatabaseEngine memdb;

	private Connection c;

	/**
	 * Easy set builder.
	 *
	 * @param strings
	 *            The values in the set.
	 * @return An unmodifiable set.
	 */
	private static Set<String> set(String... strings) {
		return unmodifiableSet(new HashSet<>(Arrays.asList(strings)));
	}

	/**
	 * Compares two sets by converting them into sorted lists. This produces the
	 * most comprehensible results.
	 *
	 * @param <T>
	 *            The type of elements in the sets.
	 * @param expected
	 *            The set of expected elements.
	 * @param actual
	 *            The actual results of the operation.
	 */
	private static <T extends Comparable<T>> void
			assertSetEquals(Set<T> expected, Set<T> actual) {
		List<T> e = new ArrayList<>(expected);
		sort(e);
		List<T> a = new ArrayList<>(actual);
		sort(a);
		assertEquals(e, a);
	}

	/**
	 * Asserts that the result columns of the query are adequate for making a
	 * {@link BoardLocation} instance.
	 *
	 * @param q
	 *            The query that feeds the creation.
	 */
	private static void assertCanMakeBoardLocation(Query q) {
		assertTrue(
				q.getRowColumnNames()
						.containsAll(BOARD_LOCATION_REQUIRED_COLUMNS),
				() -> "board location creation using " + q
						+ " will fail; required columns missing");
	}

	/**
	 * <em>Assert</em> that execution of the supplied executable throws an
	 * exception due to a foreign key constraint failure.
	 *
	 * @param op
	 *            The executable operation being tested.
	 */
	private static void assertThrowsFK(Executable op) {
		DataAccessException e = assertThrows(DataAccessException.class, op);
		assertEquals(SQLiteException.class, e.getMostSpecificCause());
		SQLiteException exn = (SQLiteException) e.getMostSpecificCause();
		assertEquals(SQLITE_CONSTRAINT_FOREIGNKEY, exn.getResultCode());
	}

	/**
	 * <em>Assert</em> that execution of the supplied executable throws an
	 * exception due to a CHECK constraint failure.
	 *
	 * @param op
	 *            The executable operation being tested.
	 */
	private static void assertThrowsCheck(Executable op) {
		DataAccessException e = assertThrows(DataAccessException.class, op);
		assertEquals(SQLiteException.class, e.getMostSpecificCause());
		SQLiteException exn = (SQLiteException) e.getMostSpecificCause();
		assertEquals(SQLITE_CONSTRAINT_CHECK, exn.getResultCode());
	}

	@BeforeAll
	void getMemoryDatabase() {
		assumeTrue(mainDBEngine != null, "spring-configured DB engine absent");
		memdb = new DatabaseEngine(mainDBEngine);
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

	@Test
	void testDbConn() {
		int rows = 0;
		try (Query q =
				query(c, "SELECT COUNT(*) AS c FROM board_model_coords")) {
			for (Row row : q.call()) {
				// For v2, v3, v4 and v5 board configs (
				assertEquals(104, row.getInt("c"));
				rows++;
			}
		}
		assertEquals(1, rows, "should be only one row in query result");
	}

	private static void assumeWritable(Connection c) {
		try {
			assumeFalse(c.isReadOnly(), "connection is read-only");
		} catch (SQLException e0) {
			throw new RuntimeException("unexpected exception", e0);
		}
	}

	@Test
	void testDbChanges() {
		assumeWritable(c);
		int rows;
		exec(c, "CREATE TEMPORARY TABLE foo(x)");
		try (Update u = update(c, "INSERT INTO foo(x) VALUES(?)");
				Query q = query(c, "SELECT x FROM foo WHERE ? = ?");
				Query q2 = query(c, "SELECT x FROM foo")) {
			rows = 0;
			for (Row row : q.call(1, 1)) {
				assertNotNull(row.getObject("x"));
				rows++;
			}
			assertEquals(0, rows);

			int keyCount = 0;
			for (Integer key : u.keys(123)) {
				// Tricky: assumes how SQLite generates keys
				assertEquals(Integer.valueOf(1), key);
				keyCount++;
			}
			assertEquals(1, keyCount);

			rows = 0;
			for (Row row : q.call(1, 1)) {
				assertEquals(123, row.getInt("x"));
				rows++;
			}
			assertEquals(1, rows);

			// Test what happens when we give too many arguments
			DataAccessException e1 = assertThrows(DataAccessException.class,
					() -> q.call(1, 2, 3));
			assertEquals("prepared statement takes 2 arguments, not 3",
					e1.getMostSpecificCause().getMessage());

			DataAccessException e2 =
					assertThrows(DataAccessException.class, () -> q2.call(1));
			assertEquals("prepared statement takes no arguments",
					e2.getMostSpecificCause().getMessage());

			// Test what happens when we give too few arguments
			DataAccessException e3 =
					assertThrows(DataAccessException.class, () -> q.call(1));
			assertEquals("prepared statement takes 2 arguments, not 1",
					e3.getMostSpecificCause().getMessage());
		}
	}

	/**
	 * Tests of queries. Ensures that the SQL and the schema remain
	 * synchronized.
	 */
	@Nested
	class DQLBasicChecks extends SQLQueries {
		@Test
		void getAllMachines() {
			try (Query q = query(c, GET_ALL_MACHINES)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(
						set("machine_id", "machine_name", "width", "height"),
						q.getRowColumnNames());
				assertFalse(q.call1().isPresent());
			}
		}

		@Test
		void listMachineNames() {
			try (Query q = query(c, LIST_MACHINE_NAMES)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(set("machine_name"), q.getRowColumnNames());
				assertFalse(q.call1().isPresent());
			}
		}

		@Test
		void getMachineById() {
			try (Query q = query(c, GET_MACHINE_BY_ID)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(
						set("machine_id", "machine_name", "width", "height"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getNamedMachine() {
			try (Query q = query(c, GET_NAMED_MACHINE)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(
						set("machine_id", "machine_name", "width", "height"),
						q.getRowColumnNames());
				assertFalse(q.call1("gorp").isPresent());
			}
		}

		@Test
		void getMachineJobs() {
			try (Query q = query(c, GET_MACHINE_JOBS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("job_id", "owner_name"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getJobIds() {
			try (Query q = query(c, GET_JOB_IDS)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("job_id", "machine_id", "job_state",
						"keepalive_timestamp"), q.getRowColumnNames());
				assertFalse(q.call1(0, 0).isPresent());
			}
		}

		@Test
		void getLiveJobIds() {
			try (Query q = query(c, GET_LIVE_JOB_IDS)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("job_id", "machine_id", "job_state",
						"keepalive_timestamp"), q.getRowColumnNames());
				q.call(0, 0);
				// Must not throw
			}
		}

		@Test
		void getJob() {
			try (Query q = query(c, GET_JOB)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(
						set("job_id", "machine_id", "machine_name", "width",
								"height", "depth", "root_id", "job_state",
								"keepalive_timestamp", "keepalive_host",
								"keepalive_interval", "create_timestamp",
								"death_reason", "death_timestamp",
								"original_request", "owner"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_JOB).isPresent());
			}
		}

		@Test
		void getJobBoards() {
			try (Query q = query(c, GET_JOB_BOARDS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("board_id"), q.getRowColumnNames());
				assertFalse(q.call1(NO_JOB).isPresent());
			}
		}

		@Test
		void getJobBoardCoords() {
			try (Query q = query(c, GET_JOB_BOARD_COORDS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(BOARD_COORDS_REQUIRED_COLUMNS,
						q.getRowColumnNames());
				assertFalse(q.call1(NO_JOB).isPresent());
			}
		}

		@Test
		void getJobChipDimensions() {
			try (Query q = query(c, GET_JOB_CHIP_DIMENSIONS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("width", "height"), q.getRowColumnNames());
				Row row = q.call1(NO_JOB).get();
				// These two are actually NULL when there's no job
				assertEquals(0, row.getInt("width"));
				assertEquals(0, row.getInt("height"));
			}
		}

		@Test
		void getRootOfBoard() {
			try (Query q = query(c, GET_ROOT_OF_BOARD)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("root_x", "root_y"), q.getRowColumnNames());
				assertFalse(q.call1(NO_BOARD).isPresent());
			}
		}

		@Test
		void getRootBMPAddress() {
			try (Query q = query(c, GET_ROOT_BMP_ADDRESS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("address"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getBoardNumbers() {
			try (Query q = query(c, GET_BOARD_NUMBERS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("board_num"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getLiveBoards() {
			try (Query q = query(c, GET_LIVE_BOARDS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(BOARD_COORDS_REQUIRED_COLUMNS,
						q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getDeadBoards() {
			try (Query q = query(c, GET_DEAD_BOARDS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(BOARD_COORDS_REQUIRED_COLUMNS,
						q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getDeadLinks() {
			try (Query q = query(c, getDeadLinks)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(
						set("board_1_x", "board_1_y", "board_1_z", "board_1_c",
								"board_1_f", "board_1_b", "board_1_addr",
								"dir_1", "board_2_x", "board_2_y", "board_2_z",
								"board_2_c", "board_2_f", "board_2_b",
								"board_2_addr", "dir_2"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getAvailableBoardNumbers() {
			try (Query q = query(c, GET_AVAILABLE_BOARD_NUMBERS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("board_num"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getTags() {
			try (Query q = query(c, GET_TAGS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("tag"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getBoardPower() {
			// This query always produces one row
			try (Query q = query(c, GET_BOARD_POWER)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("total_on"), q.getRowColumnNames());
				Row row = q.call1(NO_JOB).get();
				assertEquals(0, row.getInt("total_on"));
			}
		}

		@Test
		void getBoardConnectInfo() {
			try (Query q = query(c, GET_BOARD_CONNECT_INFO)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("board_id", "address", "x", "y", "z",
						"root_x", "root_y"), q.getRowColumnNames());
				assertFalse(q.call1(NO_JOB).isPresent());
			}
		}

		@Test
		void getRootCoords() {
			try (Query q = query(c, GET_ROOT_COORDS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("x", "y", "z", "root_x", "root_y"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_BOARD).isPresent());
			}
		}

		@Test
		void getAllocationTasks() {
			try (Query q = query(c, getAllocationTasks)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(set("req_id", "job_id", "num_boards", "width",
						"height", "board_id", "machine_id", "max_dead_boards",
						"max_height", "max_width", "job_state", "importance"),
						q.getRowColumnNames());
				assertFalse(q.call1().isPresent());
			}
		}

		@Test
		void findFreeBoard() {
			try (Query q = query(c, FIND_FREE_BOARD)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("x", "y", "z"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getBoardByCoords() {
			try (Query q = query(c, GET_BOARD_BY_COORDS)) {
				assertEquals(4, q.getNumArguments());
				assertSetEquals(set("board_id"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE, -1, -1, -1).isPresent());
			}
		}

		@Test
		void findExpiredJobs() {
			try (Query q = query(c, FIND_EXPIRED_JOBS)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(set("job_id"), q.getRowColumnNames());
				assertFalse(q.call1().isPresent());
			}
		}

		@Test
		void loadDirInfo() {
			try (Query q = query(c, LOAD_DIR_INFO)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(set("z", "direction", "dx", "dy", "dz"),
						q.getRowColumnNames());
				q.call();
			}
		}

		@Test
		void getChanges() {
			try (Query q = query(c, GET_CHANGES)) {
				assertEquals(1, q.getNumArguments());
				Set<String> colNames = q.getRowColumnNames();
				assertSetEquals(set("change_id", "job_id", "board_id", "power",
						"fpga_n", "fpga_s", "fpga_e", "fpga_w", "fpga_nw",
						"fpga_se", "in_progress", "from_state", "to_state"),
						colNames);
				// Ensure that this link is maintained
				for (Direction d : Direction.values()) {
					assertTrue(colNames.contains(d.columnName),
							() -> format("%s must contain %s", colNames,
									d.columnName));
				}
				assertFalse(q.call1(NO_JOB).isPresent());
			}
		}

		@Test
		void findRectangle() {
			try (Query q = query(c, findRectangle)) {
				assertEquals(4, q.getNumArguments());
				assertSetEquals(set("id", "x", "y", "z", "available"),
						q.getRowColumnNames());
				assertFalse(q.call1(-1, -1, NO_MACHINE, 0).isPresent());
			}
		}

		@Test
		void findLocation() {
			try (Query q = query(c, findLocation)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("x", "y", "z"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE, NO_BOARD).isPresent());
			}
		}

		@Test
		void countConnected() {
			try (Query q = query(c, countConnected)) {
				assertEquals(5, q.getNumArguments());
				assertSetEquals(set("connected_size"), q.getRowColumnNames());
				Row row = q.call1(NO_MACHINE, -1, -1, -1, -1).get();
				assertEquals(0, row.getInt("connected_size"));
			}
		}

		@Test
		void countPendingChanges() {
			try (Query q = query(c, COUNT_PENDING_CHANGES)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(set("c"), q.getRowColumnNames());
				Row row = q.call1().get();
				assertEquals(0, row.getInt("c"));
			}
		}

		@Test
		void getPerimeterLinks() {
			try (Query q = query(c, getPerimeterLinks)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("board_id", "direction"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_JOB).isPresent());
			}
		}

		@Test
		void findBoardByGlobalChip() {
			try (Query q = query(c, findBoardByGlobalChip)) {
				assertEquals(3, q.getNumArguments());
				assertSetEquals(set("board_id", "address", "bmp_id", "x", "y",
						"z", "job_id", "machine_name", "cabinet", "frame",
						"board_num", "chip_x", "chip_y", "board_chip_x",
						"board_chip_y", "job_root_chip_x", "job_root_chip_y"),
						q.getRowColumnNames());
				assertCanMakeBoardLocation(q);
				assertFalse(q.call1(NO_MACHINE, -1, -1).isPresent());
			}
		}

		@Test
		void findBoardByJobChip() {
			try (Query q = query(c, findBoardByJobChip)) {
				assertEquals(4, q.getNumArguments());
				assertSetEquals(set("board_id", "address", "x", "y", "z",
						"job_id", "machine_name", "cabinet", "frame",
						"board_num", "chip_x", "chip_y", "board_chip_x",
						"board_chip_y", "job_root_chip_x", "job_root_chip_y"),
						q.getRowColumnNames());
				assertCanMakeBoardLocation(q);
				assertFalse(q.call1(NO_JOB, NO_BOARD, -1, -1).isPresent());
			}
		}

		@Test
		void findBoardByLogicalCoords() {
			try (Query q = query(c, findBoardByLogicalCoords)) {
				assertEquals(4, q.getNumArguments());
				assertSetEquals(set("board_id", "address", "bmp_id", "x", "y",
						"z", "job_id", "machine_name", "cabinet", "frame",
						"board_num", "chip_x", "chip_y", "board_chip_x",
						"board_chip_y", "job_root_chip_x", "job_root_chip_y"),
						q.getRowColumnNames());
				assertCanMakeBoardLocation(q);
				assertFalse(q.call1(NO_MACHINE, -1, -1, -1).isPresent());
			}
		}

		@Test
		void findBoardByPhysicalCoords() {
			try (Query q = query(c, findBoardByPhysicalCoords)) {
				assertEquals(4, q.getNumArguments());
				assertSetEquals(set("board_id", "address", "bmp_id", "x", "y",
						"z", "job_id", "machine_name", "cabinet", "frame",
						"board_num", "chip_x", "chip_y", "board_chip_x",
						"board_chip_y", "job_root_chip_x", "job_root_chip_y"),
						q.getRowColumnNames());
				assertCanMakeBoardLocation(q);
				assertFalse(q.call1(NO_MACHINE, -1, -1, -1).isPresent());
			}
		}

		@Test
		void findBoardByIPAddress() {
			try (Query q = query(c, findBoardByIPAddress)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("board_id", "address", "bmp_id", "x", "y",
						"z", "job_id", "machine_name", "cabinet", "frame",
						"board_num", "chip_x", "chip_y", "board_chip_x",
						"board_chip_y", "job_root_chip_x", "job_root_chip_y"),
						q.getRowColumnNames());
				assertCanMakeBoardLocation(q);
				assertFalse(q.call1(NO_MACHINE, "127.0.0.1").isPresent());
			}
		}

		@Test
		void getJobsWithChanges() {
			try (Query q = query(c, getJobsWithChanges)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("job_id"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getConnectedBoards() {
			try (Query q = query(c, getConnectedBoards)) {
				assertEquals(7, q.getNumArguments());
				assertSetEquals(set("board_id"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE, -1, -1, -1, -1, -1, -1)
						.isPresent());
			}
		}

		@Test
		void findBoardByNameAndXYZ() {
			try (Query q = query(c, FIND_BOARD_BY_NAME_AND_XYZ)) {
				assertEquals(4, q.getNumArguments());
				assertSetEquals(MSC_BOARD_COORDS, q.getRowColumnNames());
				assertFalse(q.call1("gorp", -1, -1, -1).isPresent());
			}
		}

		@Test
		void findBoardByNameAndCFB() {
			try (Query q = query(c, FIND_BOARD_BY_NAME_AND_CFB)) {
				assertEquals(4, q.getNumArguments());
				assertSetEquals(MSC_BOARD_COORDS, q.getRowColumnNames());
				assertFalse(q.call1("gorp", -1, -1, -1).isPresent());
			}
		}

		@Test
		void findBoardByNameAndIPAddress() {
			try (Query q = query(c, FIND_BOARD_BY_NAME_AND_IP_ADDRESS)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(MSC_BOARD_COORDS, q.getRowColumnNames());
				assertFalse(q.call1("gorp", "256.256.256.256").isPresent());
			}
		}

		@Test
		void getFunctioningField() {
			try (Query q = query(c, GET_FUNCTIONING_FIELD)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("functioning"), q.getRowColumnNames());
				assertFalse(q.call1(NO_BOARD).isPresent());
			}
		}

		@Test
		void getUserQuota() {
			try (Query q = query(c, GET_USER_QUOTA)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("quota", "user_id"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE, "gorp").isPresent());
			}
		}

		@Test
		void getCurrentUsage() {
			try (Query q = query(c, GET_CURRENT_USAGE)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("current_usage"), q.getRowColumnNames());
				assertNull(q.call1(NO_MACHINE, "gorp").get()
						.getObject("current_usage"));
			}
		}

		@Test
		void getJobUsageAndQuota() {
			try (Query q = query(c, GET_JOB_USAGE_AND_QUOTA)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("quota", "usage"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE, NO_JOB).isPresent());
			}
		}

		@Test
		void getConsolidationTargets() {
			try (Query q = query(c, GET_CONSOLIDATION_TARGETS)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(set("job_id", "quota_id", "usage"),
						q.getRowColumnNames());
				// Empty DB has no consolidation targets
				assertFalse(q.call1().isPresent());
			}
		}

		@Test
		void isUserLocked() {
			try (Query q = query(c, IS_USER_LOCKED)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("disabled", "locked", "user_id"),
						q.getRowColumnNames());
				// Empty DB has no consolidation targets
				assertFalse(q.call1("").isPresent());
			}
		}

		@Test
		void getUserAuthorities() {
			try (Query q = query(c, GET_USER_AUTHORITIES)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("trust_level", "has_password"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_USER).isPresent());
			}
		}

		@Test
		void listAllUsers() {
			try (Query q = query(c, LIST_ALL_USERS)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(set("user_id", "user_name"),
						q.getRowColumnNames());
				// Testing DB has no users by default
				assertFalse(q.call1().isPresent());
			}
		}

		@Test
		void getUserId() {
			try (Query q = query(c, GET_USER_ID)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("user_id"), q.getRowColumnNames());
				// Testing DB has no users by default
				assertFalse(q.call1("gorp").isPresent());
			}
		}

		@Test
		void getUserDetails() {
			try (Query q = query(c, GET_USER_DETAILS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(
						set("disabled", "has_password", "last_fail_timestamp",
								"last_successful_login_timestamp", "locked",
								"trust_level", "user_id", "user_name"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_USER).isPresent());
			}
		}

		@Test
		void getQuotaDetails() {
			try (Query q = query(c, GET_QUOTA_DETAILS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("machine_name", "quota"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_USER).isPresent());
			}
		}

		@Test
		void countMachineThings() {
			try (Query q = query(c, COUNT_MACHINE_THINGS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("board_count", "in_use", "num_jobs"),
						q.getRowColumnNames());
				Row r = q.call1(NO_MACHINE).get();
				assertEquals(0, r.getInt("board_count"));
				assertEquals(0, r.getInt("in_use"));
				assertEquals(0, r.getInt("num_jobs"));
			}
		}

		@Test
		void countPoweredBoards() {
			try (Query q = query(c, COUNT_POWERED_BOARDS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("c"), q.getRowColumnNames());
				assertEquals(0, q.call1(NO_JOB).get().getInt("c"));
			}
		}

		@Test
		void listLiveJobs() {
			try (Query q = query(c, LIST_LIVE_JOBS)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(set("allocation_size", "create_timestamp",
						"job_id", "job_state", "keepalive_host",
						"keepalive_interval", "machine_id", "user_name"),
						q.getRowColumnNames());
				// No jobs right now
				assertFalse(q.call1().isPresent());
			}
		}

		@Test
		void getLocalUserDetails() {
			try (Query q = query(c, GET_LOCAL_USER_DETAILS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("user_id", "user_name"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_USER).isPresent());
			}
		}

		@Test
		void isUserPassMatched() {
			try (Query q = query(c, IS_USER_PASS_MATCHED)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("matches"), q.getRowColumnNames());
				assertFalse(q.call1("*", NO_USER).isPresent());
			}
		}

		@Test
		void getReportedBoards() {
			try (Query q = query(c, getReportedBoards)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("board_id", "num_reports", "x", "y", "z",
						"address"), q.getRowColumnNames());
				assertFalse(q.call1(0).isPresent());
			}
		}
	}

	/**
	 * Tests of inserts, updates and deletes. Ensures that the SQL and the
	 * schema remain synchronized.
	 */
	@Nested
	class DMLBasicChecks extends SQLQueries {
		@Test
		void insertJob() {
			assumeWritable(c);
			Duration d = Duration.ofSeconds(100);
			try (Update u = update(c, INSERT_JOB)) {
				assertEquals(4, u.getNumArguments());
				// No such machine
				assertThrowsFK(
						() -> u.keys(NO_MACHINE, NO_USER, d, new byte[0]));
			}
		}

		@Test
		void insertReqNBoards() {
			assumeWritable(c);
			try (Update u = update(c, INSERT_REQ_N_BOARDS)) {
				assertEquals(4, u.getNumArguments());
				// No such job
				assertThrowsFK(() -> u.keys(NO_JOB, 1, 0, 0));
				assertThrowsCheck(() -> u.keys(NO_JOB, -1, 0, 0));
			}
		}

		@Test
		void insertReqSize() {
			assumeWritable(c);
			try (Update u = update(c, INSERT_REQ_SIZE)) {
				assertEquals(5, u.getNumArguments());
				// No such job
				assertThrowsFK(() -> u.keys(NO_JOB, 1, 1, 0, 0));
				assertThrowsCheck(() -> u.keys(NO_JOB, -1, -1, 0, 0));
			}
		}

		@Test
		void insertReqBoard() {
			assumeWritable(c);
			try (Update u = update(c, INSERT_REQ_BOARD)) {
				assertEquals(3, u.getNumArguments());
				// No such job or board
				assertThrowsFK(() -> u.keys(NO_JOB, NO_BOARD, 0));
			}
		}

		@Test
		void updateKeepalive() {
			assumeWritable(c);
			try (Update u = update(c, UPDATE_KEEPALIVE)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call("gorp", NO_JOB));
			}
		}

		@Test
		void destroyJob() {
			assumeWritable(c);
			try (Update u = update(c, DESTROY_JOB)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call("gorp", NO_JOB));
			}
		}

		@Test
		void deleteTask() {
			assumeWritable(c);
			try (Update u = update(c, DELETE_TASK)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_JOB));
			}
		}

		@Test
		void allocateBoardsJob() {
			assumeWritable(c);
			try (Update u = update(c, ALLOCATE_BOARDS_JOB)) {
				assertEquals(6, u.getNumArguments());
				assertEquals(0, u.call(-1, -1, -1, NO_BOARD, 0, NO_JOB));
			}
		}

		@Test
		void deallocateBoardsJob() {
			assumeWritable(c);
			try (Update u = update(c, DEALLOCATE_BOARDS_JOB)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_JOB));
			}
		}

		@Test
		void allocateBoardsBoard() {
			assumeWritable(c);
			try (Update u = update(c, ALLOCATE_BOARDS_BOARD)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(NO_JOB, NO_BOARD));
			}
		}

		@Test
		void setStatePending() {
			assumeWritable(c);
			try (Update u = update(c, SET_STATE_PENDING)) {
				assertEquals(3, u.getNumArguments());
				assertEquals(0, u.call(JobState.UNKNOWN, 0, NO_JOB));
			}
		}

		@Test
		void bumpImportance() {
			assumeWritable(c);
			try (Update u = update(c, BUMP_IMPORTANCE)) {
				assertEquals(0, u.getNumArguments());
				// table should be empty
				assertEquals(0, u.call());
			}
		}

		@Test
		void killJobAllocTask() {
			assumeWritable(c);
			try (Update u = update(c, KILL_JOB_ALLOC_TASK)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_JOB));
			}
		}

		@Test
		void killJobPending() {
			assumeWritable(c);
			try (Update u = update(c, KILL_JOB_PENDING)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_JOB));
			}
		}

		@Test
		void setInProgress() {
			assumeWritable(c);
			try (Update u = update(c, SET_IN_PROGRESS)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(false, NO_JOB));
			}
		}

		@Test
		void issueChangeForJob() {
			assumeWritable(c);
			try (Update u = update(c, issueChangeForJob)) {
				assertEquals(11, u.getNumArguments());
				// No such job
				assertThrowsFK(() -> u.keys(NO_JOB, NO_BOARD, 0, 0, //
						true, false, false, false, false, false, false));
			}
		}

		@Test
		void setBoardPower() {
			assumeWritable(c);
			try (Update u = update(c, SET_BOARD_POWER)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(false, NO_BOARD));
			}
		}

		@Test
		void finishedPending() {
			assumeWritable(c);
			try (Update u = update(c, FINISHED_PENDING)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_CHANGE));
			}
		}

		@Test
		void insertMachine() {
			assumeWritable(c);
			try (Update u = update(c, INSERT_MACHINE_SPINN_5)) {
				assertEquals(4, u.getNumArguments());
				// Bad depth
				assertThrowsCheck(() -> u.keys("gorp", -1, -1, -1));
			}
		}

		@Test
		void insertTags() {
			assumeWritable(c);
			try (Update u = update(c, INSERT_TAG)) {
				assertEquals(2, u.getNumArguments());
				// No machine
				assertThrowsFK(() -> u.keys(NO_MACHINE, "gorp"));
			}
		}

		@Test
		void insertBMP() {
			assumeWritable(c);
			try (Update u = update(c, INSERT_BMP)) {
				assertEquals(4, u.getNumArguments());
				// No machine
				assertThrowsFK(() -> u.keys(NO_MACHINE, "gorp", 0, 0));
			}
		}

		@Test
		void insertBoard() {
			assumeWritable(c);
			try (Update u = update(c, INSERT_BOARD)) {
				assertEquals(10, u.getNumArguments());
				// No machine
				assertThrowsFK(() -> u.keys(NO_MACHINE, "gorp", NO_BMP, 0, 0, 0,
						0, 0, 0, true));
			}
		}

		@Test
		void insertLink() {
			assumeWritable(c);
			try (Update u = update(c, INSERT_LINK)) {
				assertEquals(5, u.getNumArguments());
				// No board
				assertThrowsFK(() -> u.keys(NO_BOARD, Direction.N, NO_BOARD,
						Direction.S, false));
			}
		}

		@Test
		void setMaxCoords() {
			assumeWritable(c);
			try (Update u = update(c, SET_MAX_COORDS)) {
				assertEquals(3, u.getNumArguments());
				// No machine
				assertEquals(0, u.call(0, 0, NO_MACHINE));
			}
		}

		@Test
		void setFunctioningField() {
			assumeWritable(c);
			try (Update u = update(c, SET_FUNCTIONING_FIELD)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(false, NO_BOARD));
			}
		}

		@Test
		void decrementQuota() {
			assumeWritable(c);
			try (Update u = update(c, DECREMENT_QUOTA)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(0, NO_USER)); // really quota_id
			}
		}

		@Test
		void markConsolidated() {
			assumeWritable(c);
			try (Update u = update(c, MARK_CONSOLIDATED)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_JOB));
			}
		}

		@Test
		void markLoginSuccess() {
			assumeWritable(c);
			try (Update u = update(c, MARK_LOGIN_SUCCESS)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_USER));
			}
		}

		@Test
		void markLoginFailure() {
			assumeWritable(c);
			// Tricky! Has a RETURNING clause
			try (Query u = query(c, MARK_LOGIN_FAILURE)) {
				assertEquals(2, u.getNumArguments());
				assertSetEquals(set("locked"), u.getRowColumnNames());
				assertFalse(u.call1(0, NO_USER).isPresent());
			}
		}

		@Test
		void unlockLockedUsers() {
			assumeWritable(c);
			// Tricky! Has a RETURNING clause
			try (Query u = query(c, UNLOCK_LOCKED_USERS)) {
				assertEquals(1, u.getNumArguments());
				assertSetEquals(set("user_name"), u.getRowColumnNames());
				assertFalse(u.call1(Duration.ofDays(1000)).isPresent());
			}
		}

		@Test
		void deleteUser() {
			assumeWritable(c);
			try (Update u = update(c, DELETE_USER)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_USER));
			}
		}

		@Test
		void setUserQuota() {
			assumeWritable(c);
			try (Update u = update(c, SET_USER_QUOTA)) {
				assertEquals(3, u.getNumArguments());
				assertEquals(0, u.call(0L, NO_USER, "gorp"));
			}
		}

		@Test
		void setUserTrust() {
			assumeWritable(c);
			try (Update u = update(c, SET_USER_TRUST)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(TrustLevel.BASIC, NO_USER));
			}
		}

		@Test
		void setUserLocked() {
			assumeWritable(c);
			try (Update u = update(c, SET_USER_LOCKED)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(false, NO_USER));
			}
		}

		@Test
		void setUserDisabled() {
			assumeWritable(c);
			try (Update u = update(c, SET_USER_DISABLED)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(false, NO_USER));
			}
		}

		@Test
		void setUserPass() {
			assumeWritable(c);
			try (Update u = update(c, SET_USER_PASS)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call("*", NO_USER));
			}
		}

		@Test
		void setUserName() {
			assumeWritable(c);
			try (Update u = update(c, SET_USER_NAME)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call("gorp", NO_USER));
			}
		}

		@Test
		void createUser() {
			assumeWritable(c);
			try (Update u = update(c, CREATE_USER)) {
				assertEquals(4, u.getNumArguments());
				// DB was userless; this makes one
				assertEquals(1, u.call("gorp", "*", TrustLevel.BASIC, true));
			}
		}

		@Test
		void createQuota() {
			assumeWritable(c);
			try (Update u = update(c, CREATE_QUOTA)) {
				assertEquals(3, u.getNumArguments());
				assertEquals(0, u.call(NO_USER, 0, "gorp"));
			}
		}

		@Test
		void createQuotasFromDefaults() {
			assumeWritable(c);
			try (Update u = update(c, CREATE_QUOTAS_FROM_DEFAULTS)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_USER));
			}
		}

		@Test
		void addQuotaForAllMachines() {
			assumeWritable(c);
			try (Update u = update(c, ADD_QUOTA_FOR_ALL_MACHINES)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(NO_USER, 0));
			}
		}

		@Test
		void insertBoardReport() {
			assumeWritable(c);
			try (Update u = update(c, INSERT_BOARD_REPORT)) {
				assertEquals(4, u.getNumArguments());
				assertThrowsFK(() -> u.call(NO_BOARD, NO_JOB, "gorp", NO_USER));
			}
		}

		@Test
		void deleteJobRecord() {
			assumeWritable(c);
			try (Update u = update(c, DELETE_JOB_RECORD)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_JOB));
			}
		}

		@Test
		void copyToHistoricalData() {
			assumeWritable(c);
			try (Query q = query(c, copyToHistoricalData)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("job_id"), q.getRowColumnNames());
				assertFalse(q.call1(1000000).isPresent());
			}
		}
	}
}
