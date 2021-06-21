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
import static java.util.Collections.unmodifiableSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.sqlite.SQLiteException;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.allocator.JobState;
import uk.ac.manchester.spinnaker.alloc.allocator.Direction;

/**
 * Test that the database engine interface works and that the queries are
 * synchronised with the schema. Deliberately does not do meaningful testing of
 * the data in the database.
 *
 * @author Donal Fellows
 */
@SpringBootTest
@TestInstance(PER_CLASS)
class DbTest extends SQLQueries {
	// Not equal to any machine_id
	private static final int NO_MACHINE = -1;

	// Not equal to any job_id
	private static final int NO_JOB = -1;

	// Not equal to any board_id
	private static final int NO_BOARD = -1;

	// Not equal to any change_id
	private static final int NO_CHANGE = -1;

	@Autowired
	private DatabaseEngine mainDBEngine;

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
				// For v2, v3, v4 and v5 board configs
				assertTrue(row.getInt("c") == 104);
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

	private static Set<String> set(String... strings) {
		return unmodifiableSet(new HashSet<>(Arrays.asList(strings)));
	}

	private static final Set<String> BOARD_LOCATION_REQUIRED_COLUMNS =
			set("machine_name", "x", "y", "z", "cabinet", "frame", "board_num",
					"chip_x", "chip_y", "board_chip_x", "board_chip_y",
					"job_id", "job_root_chip_x", "job_root_chip_y");

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
		assertTrue(q.getRowColumnNames()
				.containsAll(BOARD_LOCATION_REQUIRED_COLUMNS));
	}

	@Test
	void getAllMachines() throws SQLException {
		try (Query q = query(c, GET_ALL_MACHINES)) {
			assertEquals(0, q.getNumArguments());
			assertSetEquals(
					set("machine_id", "machine_name", "width", "height"),
					q.getRowColumnNames());
			q.call();
			// Must not throw
		}
	}

	@Test
	void getMachineById() throws SQLException {
		try (Query q = query(c, GET_MACHINE_BY_ID)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(
					set("machine_id", "machine_name", "width", "height"),
					q.getRowColumnNames());
			q.call(NO_MACHINE);
			// Must not throw
		}
	}

	@Test
	void getNamedMachine() throws SQLException {
		try (Query q = query(c, GET_NAMED_MACHINE)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(
					set("machine_id", "machine_name", "width", "height"),
					q.getRowColumnNames());
			q.call("gorp");
			// Must not throw
		}
	}

	@Test
	void getJobIds() throws SQLException {
		try (Query q = query(c, GET_JOB_IDS)) {
			assertEquals(2, q.getNumArguments());
			assertSetEquals(set("job_id", "machine_id", "job_state",
					"keepalive_timestamp"), q.getRowColumnNames());
			q.call(0, 0);
			// Must not throw
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
					set("machine_id", "width", "height", "depth", "root_id",
							"job_state", "keepalive_timestamp",
							"keepalive_host", "create_timestamp",
							"death_reason", "death_timestamp",
							"original_request"),
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
	void getDeadBoardNumbers() throws SQLException {
		try (Query q = query(c, GET_DEAD_BOARD_NUMBERS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("board_num"), q.getRowColumnNames());
			assertFalse(q.call1(NO_MACHINE).isPresent());
		}
	}

	@Test
	void getDeadLinkNumbers() throws SQLException {
		try (Query q = query(c, GET_DEAD_LINK_NUMBERS)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("board_1", "dir_1", "board_2", "dir_2"),
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
			assertSetEquals(set("board_id", "address", "x", "y", "z", "root_x",
					"root_y"), q.getRowColumnNames());
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
	void getTasks() throws SQLException {
		try (Query q = query(c, GET_TASKS)) {
			assertEquals(0, q.getNumArguments());
			assertSetEquals(
					set("req_id", "job_id", "num_boards", "width", "height",
							"x", "y", "z", "machine_id", "max_dead_boards"),
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
			assertEquals(4, q.getNumArguments());
			assertSetEquals(set("x", "y", "z"), q.getRowColumnNames());
			assertFalse(q.call1(NO_MACHINE, -1, -1, -1).isPresent());
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
			assertSetEquals(set("board_id", "address", "bmp_id", "x", "y", "z",
					"job_id", "machine_name", "cabinet", "frame", "board_num",
					"chip_x", "chip_y", "board_chip_x", "board_chip_y",
					"job_root_chip_x", "job_root_chip_y"),
					q.getRowColumnNames());
			assertCanMakeBoardLocation(q);
			assertFalse(q.call1(NO_MACHINE, -1, -1).isPresent());
		}
	}

	@Test
	void findBoardByJobChip() throws SQLException {
		try (Query q = query(c, findBoardByJobChip)) {
			assertEquals(4, q.getNumArguments());
			assertSetEquals(
					set("board_id", "address", "x", "y", "z", "job_id",
							"machine_name", "cabinet", "frame", "board_num",
							"chip_x", "chip_y", "board_chip_x", "board_chip_y",
							"job_root_chip_x", "job_root_chip_y"),
					q.getRowColumnNames());
			assertCanMakeBoardLocation(q);
			assertFalse(q.call1(NO_JOB, NO_BOARD, -1, -1).isPresent());
		}
	}

	@Test
	void findBoardByLogicalCoords() throws SQLException {
		try (Query q = query(c, findBoardByLogicalCoords)) {
			assertEquals(4, q.getNumArguments());
			assertSetEquals(set("board_id", "address", "bmp_id", "x", "y", "z",
					"job_id", "machine_name", "cabinet", "frame", "board_num",
					"chip_x", "chip_y", "board_chip_x", "board_chip_y",
					"job_root_chip_x", "job_root_chip_y"),
					q.getRowColumnNames());
			assertCanMakeBoardLocation(q);
			assertFalse(q.call1(NO_MACHINE, -1, -1, -1).isPresent());
		}
	}

	@Test
	void findBoardByPhysicalCoords() throws SQLException {
		try (Query q = query(c, findBoardByPhysicalCoords)) {
			assertEquals(4, q.getNumArguments());
			assertSetEquals(set("board_id", "address", "bmp_id", "x", "y", "z",
					"job_id", "machine_name", "cabinet", "frame", "board_num",
					"chip_x", "chip_y", "board_chip_x", "board_chip_y",
					"job_root_chip_x", "job_root_chip_y"),
					q.getRowColumnNames());
			assertCanMakeBoardLocation(q);
			assertFalse(q.call1(NO_MACHINE, -1, -1, -1).isPresent());
		}
	}

	@Test
	void getJobsWithChanges() throws SQLException {
		Duration d = Duration.ofSeconds(1);
		try (Query q = query(c, getJobsWithChanges)) {
			assertEquals(3, q.getNumArguments());
			assertSetEquals(set("job_id"), q.getRowColumnNames());
			assertFalse(q.call1(NO_MACHINE, d, d).isPresent());
		}
	}

	@Test
	void getConnectedBoards() throws SQLException {
		try (Query q = query(c, getConnectedBoards)) {
			assertEquals(7, q.getNumArguments());
			assertSetEquals(set("board_id"), q.getRowColumnNames());
			assertFalse(
					q.call1(NO_MACHINE, -1, -1, -1, -1, -1, -1).isPresent());
		}
	}

	@Test
	void insertJob() throws SQLException {
		assumeFalse(c.isReadOnly(), "connection is read-only");
		Duration d = Duration.ofSeconds(100);
		try (Update u = update(c, INSERT_JOB)) {
			assertEquals(4, u.getNumArguments());
			// No such machine
			assertThrowsFK(() -> u.keys(NO_MACHINE, "gorp", d, new byte[0]));
		}
	}

	@Test
	void insertReqNBoards() throws SQLException {
		assumeFalse(c.isReadOnly(), "connection is read-only");
		try (Update u = update(c, INSERT_REQ_N_BOARDS)) {
			assertEquals(3, u.getNumArguments());
			// No such job
			assertThrowsFK(() -> u.keys(NO_JOB, 1, 0));
			assertThrowsCheck(() -> u.keys(NO_JOB, -1, 0));
		}
	}

	@Test
	void insertReqSize() throws SQLException {
		assumeFalse(c.isReadOnly(), "connection is read-only");
		try (Update u = update(c, INSERT_REQ_SIZE)) {
			assertEquals(4, u.getNumArguments());
			// No such job
			assertThrowsFK(() -> u.keys(NO_JOB, 1, 1, 0));
			assertThrowsCheck(() -> u.keys(NO_JOB, -1, -1, 0));
		}
	}

	@Test
	void insertReqLocation() throws SQLException {
		assumeFalse(c.isReadOnly(), "connection is read-only");
		try (Update u = update(c, INSERT_REQ_LOCATION)) {
			assertEquals(4, u.getNumArguments());
			// No such job
			assertThrowsFK(() -> u.keys(NO_JOB, 0, 0, 0));
			assertThrowsCheck(() -> u.keys(NO_JOB, -1, -1, -1));
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
			assertEquals(5, u.getNumArguments());
			assertEquals(0, u.call(-1, -1, -1, NO_BOARD, NO_JOB));
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
}
