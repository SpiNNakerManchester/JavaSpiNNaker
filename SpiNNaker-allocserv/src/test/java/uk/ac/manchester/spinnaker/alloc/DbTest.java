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

import java.sql.Connection;
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
import org.sqlite.SQLiteException;

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
	private static <T extends Comparable<T>> void assertSetEquals(
			Set<T> expected, Set<T> actual) {
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
	 * @throws SQLException
	 *             If anything goes wrong. (Not expected)
	 */
	private static void assertCanMakeBoardLocation(Query q)
			throws SQLException {
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
		SQLiteException e = assertThrows(SQLiteException.class, op);
		assertEquals(SQLITE_CONSTRAINT_FOREIGNKEY, e.getResultCode());
	}

	/**
	 * <em>Assert</em> that execution of the supplied executable throws an
	 * exception due to a CHECK constraint failure.
	 *
	 * @param op
	 *            The executable operation being tested.
	 */
	private static void assertThrowsCheck(Executable op) {
		SQLiteException e = assertThrows(SQLiteException.class, op);
		assertEquals(SQLITE_CONSTRAINT_CHECK, e.getResultCode());
	}

	@BeforeAll
	void getMemoryDatabase() {
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

	@Test
	void testDbConn() throws SQLException {
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

	@Test
	void testDbChanges() throws SQLException {
		assumeFalse(c.isReadOnly(), "connection is read-only");
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
			SQLException e =
					assertThrows(SQLException.class, () -> q.call(1, 2, 3));
			assertEquals("prepared statement takes 2 arguments, not 3",
					e.getMessage());

			SQLException e2 =
					assertThrows(SQLException.class, () -> q2.call(1));
			assertEquals("prepared statement takes no arguments",
					e2.getMessage());

			// Test what happens when we give too few arguments
			SQLException e3 = assertThrows(SQLException.class, () -> q.call(1));
			assertEquals("prepared statement takes 2 arguments, not 1",
					e3.getMessage());
		}
	}

	/**
	 * Tests of queries. Ensures that the SQL and the schema remain
	 * synchronized.
	 */
	@Nested
	class DQLBasicChecks extends SQLQueries {
		@Test
		void getAllMachines() throws SQLException {
			try (Query q = query(c, GET_ALL_MACHINES)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(
						set("machine_id", "machine_name", "width", "height"),
						q.getRowColumnNames());
				assertFalse(q.call1().isPresent());
			}
		}

		@Test
		void listMachineNames() throws SQLException {
			try (Query q = query(c, LIST_MACHINE_NAMES)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(set("machine_name"), q.getRowColumnNames());
				assertFalse(q.call1().isPresent());
			}
		}

		@Test
		void getMachineById() throws SQLException {
			try (Query q = query(c, GET_MACHINE_BY_ID)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(
						set("machine_id", "machine_name", "width", "height"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getNamedMachine() throws SQLException {
			try (Query q = query(c, GET_NAMED_MACHINE)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(
						set("machine_id", "machine_name", "width", "height"),
						q.getRowColumnNames());
				assertFalse(q.call1("gorp").isPresent());
			}
		}

		@Test
		void getMachineJobs() throws SQLException {
			try (Query q = query(c, GET_MACHINE_JOBS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("job_id", "owner_name"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getJobIds() throws SQLException {
			try (Query q = query(c, GET_JOB_IDS)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("job_id", "machine_id", "job_state",
						"keepalive_timestamp"), q.getRowColumnNames());
				assertFalse(q.call1(0, 0).isPresent());
			}
		}

		@Test
		void getLiveJobIds() throws SQLException {
			try (Query q = query(c, GET_LIVE_JOB_IDS)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("job_id", "machine_id", "job_state",
						"keepalive_timestamp"), q.getRowColumnNames());
				q.call(0, 0);
				// Must not throw
			}
		}

		@Test
		void getJob() throws SQLException {
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
		void getJobBoards() throws SQLException {
			try (Query q = query(c, GET_JOB_BOARDS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("board_id"), q.getRowColumnNames());
				assertFalse(q.call1(NO_JOB).isPresent());
			}
		}

		@Test
		void getJobBoardCoords() throws SQLException {
			try (Query q = query(c, GET_JOB_BOARD_COORDS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(BOARD_COORDS_REQUIRED_COLUMNS,
						q.getRowColumnNames());
				assertFalse(q.call1(NO_JOB).isPresent());
			}
		}

		@Test
		void getJobChipDimensions() throws SQLException {
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
		void getRootOfBoard() throws SQLException {
			try (Query q = query(c, GET_ROOT_OF_BOARD)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("root_x", "root_y"), q.getRowColumnNames());
				assertFalse(q.call1(NO_BOARD).isPresent());
			}
		}

		@Test
		void getRootBMPAddress() throws SQLException {
			try (Query q = query(c, GET_ROOT_BMP_ADDRESS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("address"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getBoardNumbers() throws SQLException {
			try (Query q = query(c, GET_BOARD_NUMBERS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("board_num"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getLiveBoards() throws SQLException {
			try (Query q = query(c, GET_LIVE_BOARDS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(BOARD_COORDS_REQUIRED_COLUMNS,
						q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getDeadBoards() throws SQLException {
			try (Query q = query(c, GET_DEAD_BOARDS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(BOARD_COORDS_REQUIRED_COLUMNS,
						q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getDeadLinks() throws SQLException {
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
		void getAvailableBoardNumbers() throws SQLException {
			try (Query q = query(c, GET_AVAILABLE_BOARD_NUMBERS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("board_num"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getTags() throws SQLException {
			try (Query q = query(c, GET_TAGS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("tag"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getBoardPower() throws SQLException {
			// This query always produces one row
			try (Query q = query(c, GET_BOARD_POWER)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("total_on"), q.getRowColumnNames());
				Row row = q.call1(NO_JOB).get();
				assertEquals(0, row.getInt("total_on"));
			}
		}

		@Test
		void getBoardConnectInfo() throws SQLException {
			try (Query q = query(c, GET_BOARD_CONNECT_INFO)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("board_id", "address", "x", "y", "z",
						"root_x", "root_y"), q.getRowColumnNames());
				assertFalse(q.call1(NO_JOB).isPresent());
			}
		}

		@Test
		void getRootCoords() throws SQLException {
			try (Query q = query(c, GET_ROOT_COORDS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("x", "y", "z", "root_x", "root_y"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_BOARD).isPresent());
			}
		}

		@Test
		void getAllocationTasks() throws SQLException {
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
		void findFreeBoard() throws SQLException {
			try (Query q = query(c, FIND_FREE_BOARD)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("x", "y", "z"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getBoardByCoords() throws SQLException {
			try (Query q = query(c, GET_BOARD_BY_COORDS)) {
				assertEquals(4, q.getNumArguments());
				assertSetEquals(set("board_id"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE, -1, -1, -1).isPresent());
			}
		}

		@Test
		void findExpiredJobs() throws SQLException {
			try (Query q = query(c, FIND_EXPIRED_JOBS)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(set("job_id"), q.getRowColumnNames());
				assertFalse(q.call1().isPresent());
			}
		}

		@Test
		void loadDirInfo() throws SQLException {
			try (Query q = query(c, LOAD_DIR_INFO)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(set("z", "direction", "dx", "dy", "dz"),
						q.getRowColumnNames());
				q.call();
			}
		}

		@Test
		void getChanges() throws SQLException {
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
		void findRectangle() throws SQLException {
			try (Query q = query(c, findRectangle)) {
				assertEquals(4, q.getNumArguments());
				assertSetEquals(set("id", "x", "y", "z", "available"),
						q.getRowColumnNames());
				assertFalse(q.call1(-1, -1, NO_MACHINE, 0).isPresent());
			}
		}

		@Test
		void findLocation() throws SQLException {
			try (Query q = query(c, findLocation)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("x", "y", "z"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE, NO_BOARD).isPresent());
			}
		}

		@Test
		void countConnected() throws SQLException {
			try (Query q = query(c, countConnected)) {
				assertEquals(5, q.getNumArguments());
				assertSetEquals(set("connected_size"), q.getRowColumnNames());
				Row row = q.call1(NO_MACHINE, -1, -1, -1, -1).get();
				assertEquals(0, row.getInt("connected_size"));
			}
		}

		@Test
		void countPendingChanges() throws SQLException {
			try (Query q = query(c, COUNT_PENDING_CHANGES)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(set("c"), q.getRowColumnNames());
				Row row = q.call1().get();
				assertEquals(0, row.getInt("c"));
			}
		}

		@Test
		void getPerimeterLinks() throws SQLException {
			try (Query q = query(c, getPerimeterLinks)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("board_id", "direction"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_JOB).isPresent());
			}
		}

		@Test
		void findBoardByGlobalChip() throws SQLException {
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
		void findBoardByJobChip() throws SQLException {
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
		void findBoardByLogicalCoords() throws SQLException {
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
		void findBoardByPhysicalCoords() throws SQLException {
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
		void findBoardByIPAddress() throws SQLException {
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
		void getJobsWithChanges() throws SQLException {
			try (Query q = query(c, getJobsWithChanges)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("job_id"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE).isPresent());
			}
		}

		@Test
		void getConnectedBoards() throws SQLException {
			try (Query q = query(c, getConnectedBoards)) {
				assertEquals(7, q.getNumArguments());
				assertSetEquals(set("board_id"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE, -1, -1, -1, -1, -1, -1)
						.isPresent());
			}
		}

		@Test
		void findBoardByNameAndXYZ() throws SQLException {
			try (Query q = query(c, FIND_BOARD_BY_NAME_AND_XYZ)) {
				assertEquals(4, q.getNumArguments());
				assertSetEquals(MSC_BOARD_COORDS, q.getRowColumnNames());
				assertFalse(q.call1("gorp", -1, -1, -1).isPresent());
			}
		}

		@Test
		void findBoardByNameAndCFB() throws SQLException {
			try (Query q = query(c, FIND_BOARD_BY_NAME_AND_CFB)) {
				assertEquals(4, q.getNumArguments());
				assertSetEquals(MSC_BOARD_COORDS, q.getRowColumnNames());
				assertFalse(q.call1("gorp", -1, -1, -1).isPresent());
			}
		}

		@Test
		void findBoardByNameAndIPAddress() throws SQLException {
			try (Query q = query(c, FIND_BOARD_BY_NAME_AND_IP_ADDRESS)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(MSC_BOARD_COORDS, q.getRowColumnNames());
				assertFalse(q.call1("gorp", "256.256.256.256").isPresent());
			}
		}

		@Test
		void getFunctioningField() throws SQLException {
			try (Query q = query(c, GET_FUNCTIONING_FIELD)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("functioning"), q.getRowColumnNames());
				assertFalse(q.call1(NO_BOARD).isPresent());
			}
		}

		@Test
		void getUserQuota() throws SQLException {
			try (Query q = query(c, GET_USER_QUOTA)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("quota", "user_id"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE, "gorp").isPresent());
			}
		}

		@Test
		void getCurrentUsage() throws SQLException {
			try (Query q = query(c, GET_CURRENT_USAGE)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("current_usage"), q.getRowColumnNames());
				assertNull(q.call1(NO_MACHINE, "gorp").get()
						.getObject("current_usage"));
			}
		}

		@Test
		void getJobUsageAndQuota() throws SQLException {
			try (Query q = query(c, GET_JOB_USAGE_AND_QUOTA)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("quota", "usage"), q.getRowColumnNames());
				assertFalse(q.call1(NO_MACHINE, NO_JOB).isPresent());
			}
		}

		@Test
		void getConsolidationTargets() throws SQLException {
			try (Query q = query(c, GET_CONSOLIDATION_TARGETS)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(set("job_id", "quota_id", "usage"),
						q.getRowColumnNames());
				// Empty DB has no consolidation targets
				assertFalse(q.call1().isPresent());
			}
		}

		@Test
		void isUserLocked() throws SQLException {
			try (Query q = query(c, IS_USER_LOCKED)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("disabled", "locked", "user_id"),
						q.getRowColumnNames());
				// Empty DB has no consolidation targets
				assertFalse(q.call1("").isPresent());
			}
		}

		@Test
		void getUserAuthorities() throws SQLException {
			try (Query q = query(c, GET_USER_AUTHORITIES)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("trust_level", "password"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_USER).isPresent());
			}
		}

		@Test
		void listAllUsers() throws SQLException {
			try (Query q = query(c, LIST_ALL_USERS)) {
				assertEquals(0, q.getNumArguments());
				assertSetEquals(set("user_id", "user_name"),
						q.getRowColumnNames());
				// Testing DB has no users by default
				assertFalse(q.call1().isPresent());
			}
		}

		@Test
		void getUserId() throws SQLException {
			try (Query q = query(c, GET_USER_ID)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("user_id"), q.getRowColumnNames());
				// Testing DB has no users by default
				assertFalse(q.call1("gorp").isPresent());
			}
		}

		@Test
		void getUserDetails() throws SQLException {
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
		void getQuotaDetails() throws SQLException {
			try (Query q = query(c, GET_QUOTA_DETAILS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("machine_name", "quota"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_USER).isPresent());
			}
		}

		@Test
		void countMachineThings() throws SQLException {
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
		void countPoweredBoards() throws SQLException {
			try (Query q = query(c, COUNT_POWERED_BOARDS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("c"), q.getRowColumnNames());
				assertEquals(0, q.call1(NO_JOB).get().getInt("c"));
			}
		}

		@Test
		void listLiveJobs() throws SQLException {
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
		void getLocalUserDetails() throws SQLException {
			try (Query q = query(c, GET_LOCAL_USER_DETAILS)) {
				assertEquals(1, q.getNumArguments());
				assertSetEquals(set("user_id", "user_name"),
						q.getRowColumnNames());
				assertFalse(q.call1(NO_USER).isPresent());
			}
		}

		@Test
		void isUserPassMatched() throws SQLException {
			try (Query q = query(c, IS_USER_PASS_MATCHED)) {
				assertEquals(2, q.getNumArguments());
				assertSetEquals(set("matches"), q.getRowColumnNames());
				assertFalse(q.call1("*", NO_USER).isPresent());
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
		void insertJob() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			Duration d = Duration.ofSeconds(100);
			try (Update u = update(c, INSERT_JOB)) {
				assertEquals(4, u.getNumArguments());
				// No such machine
				assertThrowsFK(
						() -> u.keys(NO_MACHINE, NO_USER, d, new byte[0]));
			}
		}

		@Test
		void insertReqNBoards() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, INSERT_REQ_N_BOARDS)) {
				assertEquals(4, u.getNumArguments());
				// No such job
				assertThrowsFK(() -> u.keys(NO_JOB, 1, 0, 0));
				assertThrowsCheck(() -> u.keys(NO_JOB, -1, 0, 0));
			}
		}

		@Test
		void insertReqSize() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, INSERT_REQ_SIZE)) {
				assertEquals(5, u.getNumArguments());
				// No such job
				assertThrowsFK(() -> u.keys(NO_JOB, 1, 1, 0, 0));
				assertThrowsCheck(() -> u.keys(NO_JOB, -1, -1, 0, 0));
			}
		}

		@Test
		void insertReqBoard() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, INSERT_REQ_BOARD)) {
				assertEquals(3, u.getNumArguments());
				// No such job or board
				assertThrowsFK(() -> u.keys(NO_JOB, NO_BOARD, 0));
			}
		}

		@Test
		void updateKeepalive() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, UPDATE_KEEPALIVE)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call("gorp", NO_JOB));
			}
		}

		@Test
		void destroyJob() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, DESTROY_JOB)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call("gorp", NO_JOB));
			}
		}

		@Test
		void deleteTask() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, DELETE_TASK)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_JOB));
			}
		}

		@Test
		void allocateBoardsJob() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, ALLOCATE_BOARDS_JOB)) {
				assertEquals(6, u.getNumArguments());
				assertEquals(0, u.call(-1, -1, -1, NO_BOARD, 0, NO_JOB));
			}
		}

		@Test
		void deallocateBoardsJob() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, DEALLOCATE_BOARDS_JOB)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_JOB));
			}
		}

		@Test
		void allocateBoardsBoard() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, ALLOCATE_BOARDS_BOARD)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(NO_JOB, NO_BOARD));
			}
		}

		@Test
		void setStatePending() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, SET_STATE_PENDING)) {
				assertEquals(3, u.getNumArguments());
				assertEquals(0, u.call(JobState.UNKNOWN, 0, NO_JOB));
			}
		}

		@Test
		void bumpImportance() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, BUMP_IMPORTANCE)) {
				assertEquals(0, u.getNumArguments());
				// table should be empty
				assertEquals(0, u.call());
			}
		}

		@Test
		void killJobAllocTask() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, KILL_JOB_ALLOC_TASK)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_JOB));
			}
		}

		@Test
		void killJobPending() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, KILL_JOB_PENDING)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_JOB));
			}
		}

		@Test
		void setInProgress() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, SET_IN_PROGRESS)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(false, NO_JOB));
			}
		}

		@Test
		void issueChangeForJob() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, issueChangeForJob)) {
				assertEquals(11, u.getNumArguments());
				// No such job
				assertThrowsFK(() -> u.keys(NO_JOB, NO_BOARD, 0, 0, //
						true, false, false, false, false, false, false));
			}
		}

		@Test
		void setBoardPower() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, SET_BOARD_POWER)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(false, NO_BOARD));
			}
		}

		@Test
		void finishedPending() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, FINISHED_PENDING)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_CHANGE));
			}
		}

		@Test
		void insertMachine() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, INSERT_MACHINE_SPINN_5)) {
				assertEquals(4, u.getNumArguments());
				// Bad depth
				assertThrowsCheck(() -> u.keys("gorp", -1, -1, -1));
			}
		}

		@Test
		void insertTags() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, INSERT_TAG)) {
				assertEquals(2, u.getNumArguments());
				// No machine
				assertThrowsFK(() -> u.keys(NO_MACHINE, "gorp"));
			}
		}

		@Test
		void insertBMP() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, INSERT_BMP)) {
				assertEquals(4, u.getNumArguments());
				// No machine
				assertThrowsFK(() -> u.keys(NO_MACHINE, "gorp", 0, 0));
			}
		}

		@Test
		void insertBoard() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, INSERT_BOARD)) {
				assertEquals(10, u.getNumArguments());
				// No machine
				assertThrowsFK(() -> u.keys(NO_MACHINE, "gorp", NO_BMP, 0, 0, 0,
						0, 0, 0, true));
			}
		}

		@Test
		void insertLink() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, INSERT_LINK)) {
				assertEquals(5, u.getNumArguments());
				// No board
				assertThrowsFK(() -> u.keys(NO_BOARD, Direction.N, NO_BOARD,
						Direction.S, false));
			}
		}

		@Test
		void setFunctioningField() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, SET_FUNCTIONING_FIELD)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(false, NO_BOARD));
			}
		}

		@Test
		void decrementQuota() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, DECREMENT_QUOTA)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(0, NO_USER)); // really quota_id
			}
		}

		@Test
		void markConsolidated() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, MARK_CONSOLIDATED)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_JOB));
			}
		}

		@Test
		void markLoginSuccess() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, MARK_LOGIN_SUCCESS)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_USER));
			}
		}

		@Test
		void markLoginFailure() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			// Tricky! Has a RETURNING clause
			try (Query u = query(c, MARK_LOGIN_FAILURE)) {
				assertEquals(2, u.getNumArguments());
				assertSetEquals(set("locked"), u.getRowColumnNames());
				assertFalse(u.call1(0, NO_USER).isPresent());
			}
		}

		@Test
		void unlockLockedUsers() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			// Tricky! Has a RETURNING clause
			try (Query u = query(c, UNLOCK_LOCKED_USERS)) {
				assertEquals(1, u.getNumArguments());
				assertSetEquals(set("user_name"), u.getRowColumnNames());
				assertFalse(u.call1(Duration.ofDays(1000)).isPresent());
			}
		}

		@Test
		void deleteUser() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, DELETE_USER)) {
				assertEquals(1, u.getNumArguments());
				assertEquals(0, u.call(NO_USER));
			}
		}

		@Test
		void setUserQuota() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, SET_USER_QUOTA)) {
				assertEquals(3, u.getNumArguments());
				assertEquals(0, u.call(0L, NO_USER, "gorp"));
			}
		}

		@Test
		void setUserTrust() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, SET_USER_TRUST)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(TrustLevel.BASIC, NO_USER));
			}
		}

		@Test
		void setUserLocked() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, SET_USER_LOCKED)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(false, NO_USER));
			}
		}

		@Test
		void setUserDisabled() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, SET_USER_DISABLED)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(false, NO_USER));
			}
		}

		@Test
		void setUserPass() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, SET_USER_PASS)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call("*", NO_USER));
			}
		}

		@Test
		void setUserName() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, SET_USER_NAME)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call("gorp", NO_USER));
			}
		}

		@Test
		void createUser() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, CREATE_USER)) {
				assertEquals(4, u.getNumArguments());
				// DB was userless; this makes one
				assertEquals(1, u.call("gorp", "*", TrustLevel.BASIC, true));
			}
		}

		@Test
		void createQuota() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, CREATE_QUOTA)) {
				assertEquals(3, u.getNumArguments());
				assertEquals(0, u.call(NO_USER, 0, "gorp"));
			}
		}

		@Test
		void addQuotaForAllMachines() throws SQLException {
			assumeFalse(c.isReadOnly(), "connection is read-only");
			try (Update u = update(c, ADD_QUOTA_FOR_ALL_MACHINES)) {
				assertEquals(2, u.getNumArguments());
				assertEquals(0, u.call(NO_USER, 0));
			}
		}
	}
}
