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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_BLACKLIST_OP;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_BMP;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_BOARD;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_CHANGE;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_GROUP;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_JOB;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_MACHINE;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_NAME;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.NO_USER;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.assertSetEquals;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.assertThrowsCheck;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.assertThrowsFK;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.assumeWritable;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.set;
import static uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType.INTERNAL;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.UNKNOWN;

import java.nio.ByteBuffer;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.security.TrustLevel;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;

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
	// Many many seconds
	private static final int A_LONG_TIME = 1000000;

	// No such alloc record
	private static final int NO_ALLOC = -1;

	private DatabaseEngine memdb;

	private Connection c;

	@BeforeAll
	void getMemoryDatabase(@Autowired DatabaseEngine mainDBEngine) {
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
	void insertReqSizeBoard() {
		assumeWritable(c);
		try (Update u = c.update(INSERT_REQ_SIZE_BOARD)) {
			assertEquals(6, u.getNumArguments());
			c.transaction(() -> {
				// No such job
				assertThrowsFK(() -> u.keys(NO_JOB, NO_BOARD, 1, 1, 0, 0));
				assertThrowsCheck(() -> u.keys(NO_JOB, NO_BOARD, -1, -1, 0, 0));
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
				assertEquals(0, u.call(NO_NAME, NO_JOB));
			});
		}
	}

	@Test
	void destroyJob() {
		assumeWritable(c);
		try (Update u = c.update(DESTROY_JOB)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call("anything", NO_JOB));
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
				assertThrowsFK(() -> u.keys(NO_JOB, NO_BOARD, UNKNOWN, UNKNOWN,
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
				assertThrowsCheck(() -> u.keys(NO_NAME, -1, -1, -1));
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
				assertThrowsFK(() -> u.keys(NO_MACHINE, NO_NAME));
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
				assertThrowsFK(() -> u.keys(NO_MACHINE, NO_NAME, 0, 0));
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
				assertThrowsFK(() -> u.keys(NO_MACHINE, NO_NAME, NO_BMP, 0, 0,
						0, 0, 0, 0, true));
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
				assertEquals(0, u.call(true, NO_NAME));
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
		try (Query u = c.query(ADJUST_QUOTA)) {
			assertEquals(2, u.getNumArguments());
			assertSetEquals(set("group_name", "quota"), u.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(u.call1(0, NO_GROUP).isPresent());
			});
		}
	}

	@Test
	void markLoginSuccess() {
		assumeWritable(c);
		try (Update u = c.update(MARK_LOGIN_SUCCESS)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_NAME, NO_USER));
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
				assertEquals(0, u.call(NO_NAME, NO_USER));
			});
		}
	}

	@Test
	void createUser() {
		assumeWritable(c);
		try (Update u = c.update(CREATE_USER)) {
			assertEquals(5, u.getNumArguments());
			c.transaction(() -> {
				// DB was userless; this makes one
				assertEquals(1,
						u.call(NO_NAME, "*", TrustLevel.BASIC, true, NO_NAME));
			});
		}
	}

	@Test
	void createGroup() {
		assumeWritable(c);
		try (Update u = c.update(CREATE_GROUP)) {
			assertEquals(3, u.getNumArguments());
			c.transaction(() -> {
				// DB was groupless; this makes one
				assertEquals(1, u.call(NO_NAME, 0, 0));
			});
		}
	}

	@Test
	void createGroupIfNotExists() {
		assumeWritable(c);
		try (Update u = c.update(CREATE_GROUP_IF_NOT_EXISTS)) {
			assertEquals(3, u.getNumArguments());
			c.transaction(() -> {
				// DB was groupless; this makes one
				assertEquals(1, u.call(NO_NAME, 0, 0));
				// Second time does NOT create a group
				assertEquals(0, u.call(NO_NAME, 0, 0));
			});
		}
	}

	@Test
	void updateGroup() {
		assumeWritable(c);
		try (Query u = c.query(UPDATE_GROUP)) {
			assertEquals(3, u.getNumArguments());
			assertSetEquals(
					set("group_id", "group_name", "quota", "group_type"),
					u.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(u.call1(NO_NAME, 0, NO_GROUP).isPresent());
			});
		}
	}

	@Test
	void deleteGroup() {
		assumeWritable(c);
		try (Query u = c.query(DELETE_GROUP)) {
			assertEquals(1, u.getNumArguments());
			assertSetEquals(set("group_name"), u.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(u.call1(NO_GROUP).isPresent());
			});
		}
	}

	@Test
	void addUserToGroup() {
		assumeWritable(c);
		try (Update u = c.update(ADD_USER_TO_GROUP)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				// Can't do this; neither exists
				assertThrowsFK(() -> u.call(NO_USER, NO_GROUP));
			});
		}
	}

	@Test
	void removeUserFromGroup() {
		assumeWritable(c);
		try (Update u = c.update(REMOVE_USER_FROM_GROUP)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_USER, NO_GROUP));
			});
		}
	}

	@Test
	void synchGroups() {
		// Compound test: these statements are designed to be used together
		assumeWritable(c);
		try (Update u1 = c.update(GROUP_SYNC_MAKE_TEMP_TABLE)) {
			c.transaction(() -> {
				assertEquals(0, u1.getNumArguments());
				u1.call();
				try (Update u2 = c.update(GROUP_SYNC_INSERT_TEMP_ROW);
						Update u3 = c.update(GROUP_SYNC_ADD_GROUPS);
						Update u4 = c.update(GROUP_SYNC_REMOVE_GROUPS);
						Update u5 = c.update(GROUP_SYNC_DROP_TEMP_TABLE)) {
					assertEquals(2, u2.getNumArguments());
					assertEquals(1, u3.getNumArguments());
					assertEquals(1, u4.getNumArguments());
					assertEquals(0, u5.getNumArguments());
					assertEquals(0, u2.call(NO_NAME, INTERNAL));
					assertEquals(0, u3.call(NO_USER));
					assertEquals(0, u4.call(NO_USER));
					u5.call();
				}
			});
		}
	}

	@Test
	void insertBoardReport() {
		assumeWritable(c);
		try (Update u = c.update(INSERT_BOARD_REPORT)) {
			assertEquals(4, u.getNumArguments());
			c.transaction(() -> {
				assertThrowsFK(
						() -> u.call(NO_BOARD, NO_JOB, NO_NAME, NO_USER));
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
	void deleteAllocRecord() {
		assumeWritable(c);
		try (Update u = c.update(DELETE_ALLOC_RECORD)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_ALLOC));
			});
		}
	}

	@Test
	void copyAllocsToHistoricalData() {
		assumeWritable(c);
		try (Query q = c.query(copyAllocsToHistoricalData)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("alloc_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(A_LONG_TIME).isPresent());
			});
		}
	}

	@Test
	void copyJobsToHistoricalData() {
		assumeWritable(c);
		try (Query q = c.query(copyJobsToHistoricalData)) {
			assertEquals(1, q.getNumArguments());
			assertSetEquals(set("job_id"), q.getRowColumnNames());
			c.transaction(() -> {
				assertFalse(q.call1(A_LONG_TIME).isPresent());
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

	@Test
	void setBoardSerialIds() {
		assumeWritable(c);
		try (Update u = c.update(SET_BOARD_SERIAL_IDS)) {
			assertEquals(3, u.getNumArguments());
			c.transaction(() -> {
				assertThrowsFK(() -> u.call(NO_BOARD, "foo", "bar"));
			});
		}
	}

	private Blacklist dummyBlacklist() {
		ByteBuffer b = ByteBuffer.allocate(4);
		b.putInt(0);
		b.flip();
		return new Blacklist(b);
	}

	@Test
	void completedBlacklistRead() {
		assumeWritable(c);
		Blacklist bl = dummyBlacklist();
		try (Update u = c.update(COMPLETED_BLACKLIST_READ)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(bl, NO_BLACKLIST_OP));
			});
		}
	}

	@Test
	void completedBlacklistWrite() {
		assumeWritable(c);
		try (Update u = c.update(COMPLETED_BLACKLIST_WRITE)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_BLACKLIST_OP));
			});
		}
	}

	@Test
	void completedGetSerialReq() {
		assumeWritable(c);
		try (Update u = c.update(COMPLETED_GET_SERIAL_REQ)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_BLACKLIST_OP));
			});
		}
	}

	@Test
	void failedBlacklistOp() {
		assumeWritable(c);
		try (Update u = c.update(FAILED_BLACKLIST_OP)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(new Exception(), NO_BLACKLIST_OP));
			});
		}
	}

	@Test
	void createBlacklistRead() {
		assumeWritable(c);
		try (Update u = c.update(CREATE_BLACKLIST_READ)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertThrowsFK(() -> u.call(NO_BOARD));
			});
		}
	}

	@Test
	void createBlacklistWrite() {
		assumeWritable(c);
		Blacklist bl = dummyBlacklist();
		try (Update u = c.update(CREATE_BLACKLIST_WRITE)) {
			assertEquals(2, u.getNumArguments());
			c.transaction(() -> {
				assertThrowsFK(() -> u.call(NO_BOARD, bl));
			});
		}
	}

	@Test
	void createSerialReadReq() {
		assumeWritable(c);
		try (Update u = c.update(CREATE_SERIAL_READ_REQ)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertThrowsFK(() -> u.call(NO_BOARD));
			});
		}
	}

	@Test
	void deleteBlacklistOp() {
		assumeWritable(c);
		try (Update u = c.update(DELETE_BLACKLIST_OP)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertEquals(0, u.call(NO_BLACKLIST_OP));
			});
		}
	}

	@Test
	void addBlacklistedChip() {
		assumeWritable(c);
		try (Update u = c.update(ADD_BLACKLISTED_CHIP)) {
			assertEquals(3, u.getNumArguments());
			c.transaction(() -> {
				// No such board, so no insert
				assertEquals(0, u.call(NO_BOARD, -1, -1));
			});
		}
	}

	@Test
	void addBlacklistedCore() {
		assumeWritable(c);
		try (Update u = c.update(ADD_BLACKLISTED_CORE)) {
			assertEquals(4, u.getNumArguments());
			c.transaction(() -> {
				// No such board, so no insert
				assertEquals(0, u.call(NO_BOARD, -1, -1, -1));
			});
		}
	}

	@Test
	void addBlacklistedLink() {
		assumeWritable(c);
		try (Update u = c.update(ADD_BLACKLISTED_LINK)) {
			assertEquals(4, u.getNumArguments());
			c.transaction(() -> {
				// No such board, so no insert
				assertEquals(0, u.call(NO_BOARD, -1, -1, Direction.N));
			});
		}
	}

	@Test
	void clearBlacklistedChips() {
		assumeWritable(c);
		try (Update u = c.update(CLEAR_BLACKLISTED_CHIPS)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				// No such board, so no delete
				assertEquals(0, u.call(NO_BOARD));
			});
		}
	}

	@Test
	void clearBlacklistedCores() {
		assumeWritable(c);
		try (Update u = c.update(CLEAR_BLACKLISTED_CORES)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				// No such board, so no delete
				assertEquals(0, u.call(NO_BOARD));
			});
		}
	}

	@Test
	void markBoardBlacklistChanged() {
		assumeWritable(c);
		try (Update u = c.update(MARK_BOARD_BLACKLIST_CHANGED)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				// No such board, so no delete
				assertEquals(0, u.call(NO_BOARD));
			});
		}
	}

	@Test
	void markBoardBlacklistSynched() {
		assumeWritable(c);
		try (Update u = c.update(MARK_BOARD_BLACKLIST_SYNCHED)) {
			assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				// No such board, so no delete
				assertEquals(0, u.call(NO_BOARD));
			});
		}
	}
}
