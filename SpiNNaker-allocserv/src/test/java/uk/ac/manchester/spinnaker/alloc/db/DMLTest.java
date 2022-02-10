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
package uk.ac.manchester.spinnaker.alloc.db;

import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_CHECK;
import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.sqlite.SQLiteException;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.security.TrustLevel;

/**
 * Tests of inserts, updates and deletes. Ensures that the SQL and the schema
 * remain synchronized.
 *
 * @author Donal Fellows
 */
@SpringBootTest
@TestInstance(PER_CLASS)
@ActiveProfiles("unittest")
class DMLTest extends SQLQueries {
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

	// Not equal to any group_id
	private static final int NO_GROUP = -1;

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
	 * <em>Assert</em> that execution of the supplied executable throws an
	 * exception due to a foreign key constraint failure.
	 *
	 * @param op
	 *            The executable operation being tested.
	 */
	private static void assertThrowsFK(Executable op) {
		DataAccessException e = assertThrows(DataAccessException.class, op);
		assertEquals(SQLiteException.class,
				e.getMostSpecificCause().getClass());
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
		assertEquals(SQLiteException.class,
				e.getMostSpecificCause().getClass());
		SQLiteException exn = (SQLiteException) e.getMostSpecificCause();
		assertEquals(SQLITE_CONSTRAINT_CHECK, exn.getResultCode());
	}

	@BeforeAll
	void getMemoryDatabase() {
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

	private static void assumeWritable(Connection c) {
		assumeFalse(c.isReadOnly(), "connection is read-only");
	}

	@Test
	void insertJob() {
		assumeWritable(c);
		Duration d = Duration.ofSeconds(100);
		try (Update u = c.update(INSERT_JOB)) {
			assertEquals(5, u.getNumArguments());
			c.transaction(() -> {
				// No such machine
				assertThrowsFK(() -> u.keys(NO_MACHINE, NO_USER, NO_GROUP, d,
						new byte[0]));
			});
		}
	}

	@Test
	void insertReqNBoards() {
		assumeWritable(c);
		try (Update u = c.update(INSERT_REQ_N_BOARDS)) {
			assertEquals(4, u.getNumArguments());
			c.transaction(() -> {
				// No such job
				assertThrowsFK(() -> u.keys(NO_JOB, 1, 0, 0));
				assertThrowsCheck(() -> u.keys(NO_JOB, -1, 0, 0));
			});
		}
	}

	@Test
	void insertReqSize() {
		assumeWritable(c);
		try (Update u = c.update(INSERT_REQ_SIZE)) {
			assertEquals(5, u.getNumArguments());
			c.transaction(() -> {
				// No such job
				assertThrowsFK(() -> u.keys(NO_JOB, 1, 1, 0, 0));
				assertThrowsCheck(() -> u.keys(NO_JOB, -1, -1, 0, 0));
			});
		}
	}

	@Test
	void insertReqBoard() {
		assumeWritable(c);
		try (Update u = c.update(INSERT_REQ_BOARD)) {
			c.transaction(() -> {
				assertEquals(3, u.getNumArguments());
				// No such job or board
				assertThrowsFK(() -> u.keys(NO_JOB, NO_BOARD, 0));
			});
		}
	}

	@Test
	void updateKeepalive() {
		assumeWritable(c);
		try (Update u = c.update(UPDATE_KEEPALIVE)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call("gorp", NO_JOB));
			});
		}
	}

	@Test
	void destroyJob() {
		assumeWritable(c);
		try (Update u = c.update(DESTROY_JOB)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call("gorp", NO_JOB));
			});
		}
	}

	@Test
	void deleteTask() {
		assumeWritable(c);
		try (Update u = c.update(DELETE_TASK)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_JOB));
			});
		}
	}

	@Test
	void allocateBoardsJob() {
		assumeWritable(c);
		try (Update u = c.update(ALLOCATE_BOARDS_JOB)) {
			assertEquals(6, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(-1, -1, -1, NO_BOARD, 0, NO_JOB));
			});
		}
	}

	@Test
	void deallocateBoardsJob() {
		assumeWritable(c);
		try (Update u = c.update(DEALLOCATE_BOARDS_JOB)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_JOB));
			});
		}
	}

	@Test
	void allocateBoardsBoard() {
		assumeWritable(c);
		try (Update u = c.update(ALLOCATE_BOARDS_BOARD)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_JOB, NO_BOARD));
			});
		}
	}

	@Test
	void setStatePending() {
		assumeWritable(c);
		try (Update u = c.update(SET_STATE_PENDING)) {
			assertEquals(3, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(JobState.UNKNOWN, 0, NO_JOB));
			});
		}
	}

	@Test
	void bumpImportance() {
		assumeWritable(c);
		try (Update u = c.update(BUMP_IMPORTANCE)) {
			assertEquals(0, u.getNumArguments());
			c.transaction(() -> {
				// table should be empty
				assertEquals(0, u.call());
			});
		}
	}

	@Test
	void killJobAllocTask() {
		assumeWritable(c);
		try (Update u = c.update(KILL_JOB_ALLOC_TASK)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_JOB));
			});
		}
	}

	@Test
	void killJobPending() {
		assumeWritable(c);
		try (Update u = c.update(KILL_JOB_PENDING)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_JOB));
			});
		}
	}

	@Test
	void setInProgress() {
		assumeWritable(c);
		try (Update u = c.update(SET_IN_PROGRESS)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(false, NO_JOB));
			});
		}
	}

	@Test
	void issueChangeForJob() {
		assumeWritable(c);
		try (Update u = c.update(issueChangeForJob)) {
			assertEquals(11, u.getNumArguments());
			c.transaction(() -> {
				// No such job
				assertThrowsFK(() -> u.keys(NO_JOB, NO_BOARD, 0, 0, //
						true, false, false, false, false, false, false));
			});
		}
	}

	@Test
	void setBoardPower() {
		assumeWritable(c);
		try (Update u = c.update(SET_BOARD_POWER)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(false, NO_BOARD));
			});
		}
	}

	@Test
	void finishedPending() {
		assumeWritable(c);
		try (Update u = c.update(FINISHED_PENDING)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_CHANGE));
			});
		}
	}

	@Test
	void insertMachine() {
		assumeWritable(c);
		try (Update u = c.update(INSERT_MACHINE_SPINN_5)) {
			assertEquals(4, u.getNumArguments());
			c.transaction(() -> {
				// Bad depth
				assertThrowsCheck(() -> u.keys("gorp", -1, -1, -1));
			});
		}
	}

	@Test
	void insertTag() {
		assumeWritable(c);
		try (Update u = c.update(INSERT_TAG)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				// No machine
				assertThrowsFK(() -> u.keys(NO_MACHINE, "gorp"));
			});
		}
	}

	@Test
	void deleteMachineTags() {
		assumeWritable(c);
		try (Update u = c.update(DELETE_MACHINE_TAGS)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				// No machine, no tags for it
				assertEquals(0, u.call(NO_MACHINE));
			});
		}
	}

	@Test
	void insertBMP() {
		assumeWritable(c);
		try (Update u = c.update(INSERT_BMP)) {
			assertEquals(4, u.getNumArguments());
			c.transaction(() -> {
				// No machine
				assertThrowsFK(() -> u.keys(NO_MACHINE, "gorp", 0, 0));
			});
		}
	}

	@Test
	void insertBoard() {
		assumeWritable(c);
		try (Update u = c.update(INSERT_BOARD)) {
			assertEquals(10, u.getNumArguments());
			c.transaction(() -> {
				// No machine
				assertThrowsFK(() -> u.keys(NO_MACHINE, "gorp", NO_BMP, 0, 0, 0,
						0, 0, 0, true));
			});
		}
	}

	@Test
	void insertLink() {
		assumeWritable(c);
		try (Update u = c.update(INSERT_LINK)) {
			assertEquals(5, u.getNumArguments());
			c.transaction(() -> {
				// No board
				assertThrowsFK(() -> u.keys(NO_BOARD, Direction.N, NO_BOARD,
						Direction.S, false));
			});
		}
	}

	@Test
	void setMaxCoords() {
		assumeWritable(c);
		try (Update u = c.update(SET_MAX_COORDS)) {
			assertEquals(3, u.getNumArguments());
			c.transaction(() -> {
				// No machine
				assertEquals(0, u.call(0, 0, NO_MACHINE));
			});
		}
	}

	@Test
	void setMachineState() {
		assumeWritable(c);
		try (Update u = c.update(SET_MACHINE_STATE)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				// No machine
				assertEquals(0, u.call(true, "gorp"));
			});
		}
	}

	@Test
	void setFunctioningField() {
		assumeWritable(c);
		try (Update u = c.update(SET_FUNCTIONING_FIELD)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(false, NO_BOARD));
			});
		}
	}

	@Test
	void decrementQuota() {
		assumeWritable(c);
		try (Update u = c.update(DECREMENT_QUOTA)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(0, NO_USER)); // really quota_id
			});
		}
	}

	@Test
	void markConsolidated() {
		assumeWritable(c);
		try (Update u = c.update(MARK_CONSOLIDATED)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_JOB));
			});
		}
	}

	@Test
	void adjustQuota() {
		assumeWritable(c);
		try (Update u = c.update(ADJUST_QUOTA)) {
			assertEquals(3, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(0, "", NO_USER));
			});
		}
	}

	@Test
	void markLoginSuccess() {
		assumeWritable(c);
		try (Update u = c.update(MARK_LOGIN_SUCCESS)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_USER));
			});
		}
	}

	@Test
	void markLoginFailure() {
		assumeWritable(c);
		// Tricky! Has a RETURNING clause
		try (Query u = c.query(MARK_LOGIN_FAILURE)) {
			assertEquals(2, u.getNumArguments());
			assertSetEquals(set("locked"), u.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(u.call1(0, NO_USER).isPresent());
			});
		}
	}

	@Test
	void unlockLockedUsers() {
		assumeWritable(c);
		// Tricky! Has a RETURNING clause
		try (Query u = c.query(UNLOCK_LOCKED_USERS)) {
			assertEquals(1, u.getNumArguments());
			assertSetEquals(set("user_name"), u.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(u.call1(Duration.ofDays(1000)).isPresent());
			});
		}
	}

	@Test
	void deleteUser() {
		assumeWritable(c);
		try (Update u = c.update(DELETE_USER)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_USER));
			});
		}
	}

	@Test
	void setUserQuota() {
		assumeWritable(c);
		try (Update u = c.update(SET_USER_QUOTA)) {
			assertEquals(3, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(0L, NO_USER, "gorp"));
			});
		}
	}

	@Test
	void setUserTrust() {
		assumeWritable(c);
		try (Update u = c.update(SET_USER_TRUST)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(TrustLevel.BASIC, NO_USER));
			});
		}
	}

	@Test
	void setUserLocked() {
		assumeWritable(c);
		try (Update u = c.update(SET_USER_LOCKED)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(false, NO_USER));
			});
		}
	}

	@Test
	void setUserDisabled() {
		assumeWritable(c);
		try (Update u = c.update(SET_USER_DISABLED)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(false, NO_USER));
			});
		}
	}

	@Test
	void setUserPass() {
		assumeWritable(c);
		try (Update u = c.update(SET_USER_PASS)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call("*", NO_USER));
			});
		}
	}

	@Test
	void setUserName() {
		assumeWritable(c);
		try (Update u = c.update(SET_USER_NAME)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call("gorp", NO_USER));
			});
		}
	}

	@Test
	void createUser() {
		assumeWritable(c);
		try (Update u = c.update(CREATE_USER)) {
			assertEquals(4, u.getNumArguments());
			c.transaction(() -> {
				// DB was userless; this makes one
				assertEquals(1, u.call("gorp", "*", TrustLevel.BASIC, true));
			});
		}
	}

	@Test
	void createQuota() {
		assumeWritable(c);
		try (Update u = c.update(CREATE_QUOTA)) {
			assertEquals(3, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_USER, 0, "gorp"));
			});
		}
	}

	@Test
	void createQuotasFromDefaults() {
		assumeWritable(c);
		try (Update u = c.update(CREATE_QUOTAS_FROM_DEFAULTS)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_USER));
			});
		}
	}

	@Test
	void addQuotaForAllMachines() {
		assumeWritable(c);
		try (Update u = c.update(ADD_QUOTA_FOR_ALL_MACHINES)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_USER, 0));
			});
		}
	}

	@Test
	void insertBoardReport() {
		assumeWritable(c);
		try (Update u = c.update(INSERT_BOARD_REPORT)) {
			assertEquals(4, u.getNumArguments());
			c.transaction(() -> {
				assertThrowsFK(() -> u.call(NO_BOARD, NO_JOB, "gorp", NO_USER));
			});
		}
	}

	@Test
	void deleteJobRecord() {
		assumeWritable(c);
		try (Update u = c.update(DELETE_JOB_RECORD)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_JOB));
			});
		}
	}

	@Test
	void copyToHistoricalData() {
		assumeWritable(c);
		try (Query q = c.query(copyToHistoricalData)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("job_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(1000000).isPresent());
			});
		}
	}

	@Test
	void clearStuckPending() {
		assumeWritable(c);
		try (Update u = c.update(CLEAR_STUCK_PENDING)) {
			assertEquals(0, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call());
			});
		}
	}
}
