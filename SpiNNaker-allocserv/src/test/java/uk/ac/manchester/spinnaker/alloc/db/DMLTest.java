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
package uk.ac.manchester.spinnaker.alloc.db;

import static org.junit.jupiter.api.Assertions.*;
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
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.assertThrowsCheck;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.assertThrowsFK;
import static uk.ac.manchester.spinnaker.alloc.db.DBTestingUtils.assumeWritable;
import static uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType.INTERNAL;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.UNKNOWN;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
class DMLTest extends SimpleDBTestBase {
	// Many many seconds
	private static final int A_LONG_TIME = 1000000;

	// No such alloc record
	private static final int NO_ALLOC = -1;

	@Test
	void insertJob() {
		assumeWritable(c);
		var d = Duration.ofSeconds(100);
		try (var u = c.update(INSERT_JOB)) {
			c.transaction(() -> {
				// No such machine
				assertThrowsFK(() -> u.call(NO_MACHINE, NO_USER, NO_GROUP, d,
						new byte[0], 0, 0));
			});
		}
	}

	@Test
	void insertReqNBoards() {
		assumeWritable(c);
		try (var u = c.update(INSERT_REQ_N_BOARDS)) {
			c.transaction(() -> {
				// No such job
				assertThrowsFK(() -> u.call(NO_JOB, 1, 0, 0));
				assertThrowsCheck(() -> u.call(NO_JOB, -1, 0, 0));
			});
		}
	}

	@Test
	void insertReqSize() {
		assumeWritable(c);
		try (var u = c.update(INSERT_REQ_SIZE)) {
			c.transaction(() -> {
				// No such job
				assertThrowsFK(() -> u.call(NO_JOB, 1, 1, 0, 0));
				assertThrowsCheck(() -> u.call(NO_JOB, -1, -1, 0, 0));
			});
		}
	}

	@Test
	void insertReqSizeBoard() {
		assumeWritable(c);
		try (var u = c.update(INSERT_REQ_SIZE_BOARD)) {
			c.transaction(() -> {
				// No such job
				assertThrowsFK(() -> u.call(NO_JOB, NO_BOARD, 1, 1, 0, 0));
				assertThrowsCheck(() -> u.call(NO_JOB, NO_BOARD, -1, -1, 0, 0));
			});
		}
	}

	@Test
	void insertReqBoard() {
		assumeWritable(c);
		try (var u = c.update(INSERT_REQ_BOARD)) {
			c.transaction(() -> {
				// No such job or board
				assertThrowsFK(() -> u.call(NO_JOB, NO_BOARD, 0));
			});
		}
	}

	@Test
	void updateKeepalive() {
		assumeWritable(c);
		try (var u = c.update(UPDATE_KEEPALIVE)) {
			c.transaction(() -> {
				assertEquals(0, u.call(0, NO_NAME, NO_JOB));
			});
		}
	}

	@Test
	void destroyJob() {
		assumeWritable(c);
		try (var u = c.update(DESTROY_JOB)) {
			c.transaction(() -> {
				assertEquals(0, u.call("anything", 0, NO_JOB));
			});
		}
	}

	@Test
	void deleteTask() {
		assumeWritable(c);
		try (var u = c.update(DELETE_TASK)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_JOB));
			});
		}
	}

	@Test
	void allocateBoardsJob() {
		assumeWritable(c);
		try (var u = c.update(ALLOCATE_BOARDS_JOB)) {
			c.transaction(() -> {
				assertEquals(0, u.call(-1, -1, -1, NO_BOARD, 0, 0, NO_BOARD,
						NO_JOB));
			});
		}
	}

	@Test
	void deallocateBoardsJob() {
		assumeWritable(c);
		try (var u = c.update(DEALLOCATE_BOARDS_JOB)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_JOB));
			});
		}
	}

	@Test
	void allocateBoardsBoard() {
		assumeWritable(c);
		try (var u = c.update(ALLOCATE_BOARDS_BOARD)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_JOB, NO_BOARD));
			});
		}
	}

	@Test
	void setStatePending() {
		assumeWritable(c);
		try (var u = c.update(SET_STATE_PENDING)) {
			c.transaction(() -> {
				assertEquals(0, u.call(JobState.UNKNOWN, 0, NO_JOB));
			});
		}
	}

	@Test
	void setStateDestroyed() {
		assumeWritable(c);
		try (var u = c.update(SET_STATE_DESTROYED)) {
			c.transaction(() -> {
				assertEquals(0, u.call(0, 0, NO_JOB));
			});
		}
	}

	@Test
	void bumpImportance() {
		assumeWritable(c);
		try (var u = c.update(BUMP_IMPORTANCE)) {
			c.transaction(() -> {
				// table should be empty
				assertEquals(0, u.call());
			});
		}
	}

	@Test
	void killJobAllocTask() {
		assumeWritable(c);
		try (var u = c.update(KILL_JOB_ALLOC_TASK)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_JOB));
			});
		}
	}

	@Test
	void killJobPending() {
		assumeWritable(c);
		try (var u = c.update(KILL_JOB_PENDING)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_JOB));
			});
		}
	}

	@Test
	void setInProgress() {
		assumeWritable(c);
		try (var u = c.update(SET_IN_PROGRESS)) {
			c.transaction(() -> {
				assertEquals(0, u.call(false, NO_JOB));
			});
		}
	}

	@Test
	void issueChangeForJob() {
		assumeWritable(c);
		try (var u = c.update(issueChangeForJob)) {
			c.transaction(() -> {
				// No such job
				assertThrowsFK(() -> u.call(NO_JOB, NO_BOARD, UNKNOWN, UNKNOWN,
						true, false, false, false, false, false, false));
			});
		}
	}

	@Test
	void setBoardPowerOn() {
		assumeWritable(c);
		try (var u = c.update(SET_BOARD_POWER_ON)) {
			c.transaction(() -> {
				assertEquals(0, u.call(0, NO_BOARD));
			});
		}
	}

	@Test
	void setBoardPowerOff() {
		assumeWritable(c);
		try (var u = c.update(SET_BOARD_POWER_OFF)) {
			c.transaction(() -> {
				assertEquals(0, u.call(0, NO_BOARD));
			});
		}
	}

	@Test
	void finishedPending() {
		assumeWritable(c);
		try (var u = c.update(FINISHED_PENDING)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_CHANGE));
			});
		}
	}

	@Test
	void insertMachine() {
		assumeWritable(c);
		try (var u = c.update(INSERT_MACHINE_SPINN_5)) {
			c.transaction(() -> {
				// Bad depth
				assertThrowsCheck(() -> u.call(NO_NAME, -1, -1, -1));
			});
		}
	}

	@Test
	void insertTag() {
		assumeWritable(c);
		try (var u = c.update(INSERT_TAG)) {
			c.transaction(() -> {
				// No machine
				assertThrowsFK(() -> u.call(NO_MACHINE, NO_NAME));
			});
		}
	}

	@Test
	void deleteMachineTags() {
		assumeWritable(c);
		try (var u = c.update(DELETE_MACHINE_TAGS)) {
			c.transaction(() -> {
				// No machine, no tags for it
				assertEquals(0, u.call(NO_MACHINE));
			});
		}
	}

	@Test
	void insertBMP() {
		assumeWritable(c);
		try (var u = c.update(INSERT_BMP)) {
			c.transaction(() -> {
				// No machine
				assertThrowsFK(() -> u.call(NO_MACHINE, NO_NAME, 0, 0));
			});
		}
	}

	@Test
	void insertBoard() {
		assumeWritable(c);
		try (var u = c.update(INSERT_BOARD)) {
			c.transaction(() -> {
				// No machine
				assertThrowsFK(() -> u.call(NO_MACHINE, NO_NAME, NO_BMP, 0, 0,
						0, 0, 0, 0, true));
			});
		}
	}

	@Test
	void insertLink() {
		assumeWritable(c);
		try (var u = c.update(INSERT_LINK)) {
			c.transaction(() ->
				// No board, but no failure because "IGNORE"
				u.call(NO_BOARD, Direction.N, NO_BOARD,
						Direction.S, false));
		}
	}

	@Test
	void setMaxCoords() {
		assumeWritable(c);
		try (var u = c.update(SET_MAX_COORDS)) {
			c.transaction(() -> {
				// No machine
				assertEquals(0, u.call(0, 0, NO_MACHINE));
			});
		}
	}

	@Test
	void setMachineState() {
		assumeWritable(c);
		try (var u = c.update(SET_MACHINE_STATE)) {
			c.transaction(() -> {
				// No machine
				assertEquals(0, u.call(true, NO_NAME));
			});
		}
	}

	@Test
	void setFunctioningField() {
		assumeWritable(c);
		try (var u = c.update(SET_FUNCTIONING_FIELD)) {
			c.transaction(() -> {
				assertEquals(0, u.call(false, NO_BOARD));
			});
		}
	}

	@Test
	void decrementQuota() {
		assumeWritable(c);
		try (var u = c.update(DECREMENT_QUOTA)) {
			c.transaction(() -> {
				assertEquals(0, u.call(0, NO_USER)); // really quota_id
			});
		}
	}

	@Test
	void markConsolidated() {
		assumeWritable(c);
		try (var u = c.update(MARK_CONSOLIDATED)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_JOB));
			});
		}
	}

	@Test
	void adjustQuota() {
		assumeWritable(c);
		try (var u = c.update(ADJUST_QUOTA)) {
			c.transaction(() -> {
				assertEquals(0, u.call(0, NO_GROUP));
			});
		}
	}

	@Test
	void markLoginSuccess() {
		assumeWritable(c);
		try (var u = c.update(MARK_LOGIN_SUCCESS)) {
			c.transaction(() -> {
				assertEquals(0, u.call(0, NO_NAME, NO_USER));
			});
		}
	}

	@Test
	void markLoginFailure() {
		assumeWritable(c);
		// Tricky! Has a RETURNING clause
		try (var u = c.update(MARK_LOGIN_FAILURE)) {
			c.transaction(() -> {
				assertEquals(0, u.call(0, 0, NO_USER));
			});
		}
	}

	@Test
	void unlockLockedUsers() {
		assumeWritable(c);
		// Tricky! Has a RETURNING clause
		try (var u = c.update(UNLOCK_LOCKED_USERS)) {
			c.transaction(() -> {
				assertEquals(0, u.call(Duration.ofDays(1000), 0));
			});
		}
	}

	@Test
	void deleteUser() {
		assumeWritable(c);
		try (var u = c.update(DELETE_USER)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_USER));
			});
		}
	}

	@Test
	void setUserTrust() {
		assumeWritable(c);
		try (var u = c.update(SET_USER_TRUST)) {
			c.transaction(() -> {
				assertEquals(0, u.call(TrustLevel.BASIC, NO_USER));
			});
		}
	}

	@Test
	void setUserLocked() {
		assumeWritable(c);
		try (var u = c.update(SET_USER_LOCKED)) {
			c.transaction(() -> {
				assertEquals(0, u.call(false, NO_USER));
			});
		}
	}

	@Test
	void setUserDisabled() {
		assumeWritable(c);
		try (var u = c.update(SET_USER_DISABLED)) {
			c.transaction(() -> {
				assertEquals(0, u.call(false, NO_USER));
			});
		}
	}

	@Test
	void setUserPass() {
		assumeWritable(c);
		try (var u = c.update(SET_USER_PASS)) {
			c.transaction(() -> {
				assertEquals(0, u.call("*", NO_USER));
			});
		}
	}

	@Test
	void setUserName() {
		assumeWritable(c);
		try (var u = c.update(SET_USER_NAME)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_NAME, NO_USER));
			});
		}
	}

	@Test
	void createUser() {
		assumeWritable(c);
		try (var u = c.update(CREATE_USER)) {
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
		try (var u = c.update(CREATE_GROUP)) {
			c.transaction(() -> {
				// DB was groupless; this makes one
				assertEquals(1, u.call(NO_NAME, 0, 0));
			});
		}
	}

	@Test
	void createGroupIfNotExists() {
		assumeWritable(c);
		try (var u = c.update(CREATE_GROUP_IF_NOT_EXISTS)) {
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
		try (var u = c.update(UPDATE_GROUP)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_NAME, 0, NO_GROUP));
			});
		}
	}

	@Test
	void deleteGroup() {
		assumeWritable(c);
		try (var u = c.update(DELETE_GROUP)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_GROUP));
			});
		}
	}

	@Test
	void addUserToGroup() {
		assumeWritable(c);
		try (var u = c.update(ADD_USER_TO_GROUP)) {
			c.transaction(() -> {
				// Can't do this; neither exists
				assertThrowsFK(() -> u.call(NO_USER, NO_GROUP));
			});
		}
	}

	@Test
	void removeUserFromGroup() {
		assumeWritable(c);
		try (var u = c.update(REMOVE_USER_FROM_GROUP)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_USER, NO_GROUP));
			});
		}
	}

	@Test
	void synchGroups() {
		// Compound test: these statements are designed to be used together
		assumeWritable(c);
		try (var u1 = c.update(GROUP_SYNC_MAKE_TEMP_TABLE)) {
			c.transaction(() -> {
				u1.call();
				try (var u2 = c.update(GROUP_SYNC_INSERT_TEMP_ROW);
						var u3 = c.update(GROUP_SYNC_ADD_GROUPS);
						var u4 = c.update(GROUP_SYNC_REMOVE_GROUPS);
						var u5 = c.update(GROUP_SYNC_DROP_TEMP_TABLE)) {
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
		try (var u = c.update(INSERT_BOARD_REPORT)) {
			c.transaction(() -> {
				assertThrowsFK(
						() -> u.call(NO_BOARD, NO_JOB, NO_NAME, NO_USER, 0));
			});
		}
	}

	@Test
	void deleteJobRecord() {
		assumeWritable(c);
		try (var u = c.update(DELETE_JOB_RECORD)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_JOB));
			});
		}
	}

	@Test
	void deleteAllocRecord() {
		assumeWritable(c);
		try (var u = c.update(DELETE_ALLOC_RECORD)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_ALLOC));
			});
		}
	}

	@Test
	void readAllocsFromHistoricalData() {
		assumeTrue(db.isHistoricalDBAvailable());
		try (var q = c.query(READ_HISTORICAL_ALLOCS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, A_LONG_TIME, 0).isPresent());
			});
		}
	}

	@Test
	void readJobsFromHistoricalData() {
		assumeTrue(db.isHistoricalDBAvailable());
		try (var q = c.query(READ_HISTORICAL_JOBS)) {
			c.transaction(() -> {
				assertFalse(q.call1((row) -> 1, A_LONG_TIME, 0).isPresent());
			});
		}
	}

	@Test
	void writeAllocsToHistoricalData() {
		assumeTrue(db.isHistoricalDBAvailable());
		try (var conn = db.getHistoricalConnection();
				var q = conn.update(WRITE_HISTORICAL_ALLOCS)) {
			conn.transaction(() -> {
				assertEquals(1, q.call(0, 0, 0, A_LONG_TIME));
			});
		}
	}

	@Test
	void writeJopsToHistoricalData() {
		assumeTrue(db.isHistoricalDBAvailable());
		try (var conn = db.getHistoricalConnection();
				var q = conn.update(WRITE_HISTORICAL_JOBS)) {
			conn.transaction(() -> {
				assertEquals(1, q.call(0, 0, "", A_LONG_TIME, 0, 0, 0, 0,
						A_LONG_TIME, "", "", A_LONG_TIME, new byte[] {},
						A_LONG_TIME, 0, "", "", 0, ""));
			});
		}
	}

	@Test
	void clearStuckPending() {
		assumeWritable(c);
		try (var u = c.update(CLEAR_STUCK_PENDING)) {
			c.transaction(() -> {
				assertEquals(0, u.call());
			});
		}
	}

	@Test
	void setBoardSerialIds() {
		assumeWritable(c);
		try (var u = c.update(SET_BOARD_SERIAL_IDS)) {
			c.transaction(() -> {
				assertThrowsFK(() -> u.call(NO_BOARD, "foo", "bar"));
			});
		}
	}

	@Test
	void completedBlacklistRead() {
		assumeWritable(c);
		var bl = new Blacklist("");
		try (var u = c.update(COMPLETED_BLACKLIST_READ)) {
			c.transaction(() -> {
				assertEquals(0, u.call(bl, NO_BLACKLIST_OP));
			});
		}
	}

	@Test
	void completedBlacklistWrite() {
		assumeWritable(c);
		try (var u = c.update(COMPLETED_BLACKLIST_WRITE)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_BLACKLIST_OP));
			});
		}
	}

	@Test
	void completedGetSerialReq() {
		assumeWritable(c);
		try (var u = c.update(COMPLETED_GET_SERIAL_REQ)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_BLACKLIST_OP));
			});
		}
	}

	@Test
	void failedBlacklistOp() {
		assumeWritable(c);
		try (var u = c.update(FAILED_BLACKLIST_OP)) {
			c.transaction(() -> {
				assertEquals(0, u.call(new Exception(), NO_BLACKLIST_OP));
			});
		}
	}

	@Test
	void createBlacklistRead() {
		assumeWritable(c);
		try (var u = c.update(CREATE_BLACKLIST_READ)) {
			c.transaction(() -> {
				assertThrowsFK(() -> u.call(NO_BOARD));
			});
		}
	}

	@Test
	void createBlacklistWrite() {
		assumeWritable(c);
		var bl = new Blacklist("");
		try (var u = c.update(CREATE_BLACKLIST_WRITE)) {
			c.transaction(() -> {
				assertThrowsFK(() -> u.call(NO_BOARD, bl));
			});
		}
	}

	@Test
	void createSerialReadReq() {
		assumeWritable(c);
		try (var u = c.update(CREATE_SERIAL_READ_REQ)) {
			c.transaction(() -> {
				assertThrowsFK(() -> u.call(NO_BOARD));
			});
		}
	}

	@Test
	void createTempReadReq() {
		assumeWritable(c);
		try (var u = c.update(CREATE_TEMP_READ_REQ)) {
			//assertEquals(1, u.getNumArguments());
			c.transaction(() -> {
				assertThrowsFK(() -> u.call(NO_BOARD));
			});
		}
	}

	@Test
	void deleteBlacklistOp() {
		assumeWritable(c);
		try (var u = c.update(DELETE_BLACKLIST_OP)) {
			c.transaction(() -> {
				assertEquals(0, u.call(NO_BLACKLIST_OP));
			});
		}
	}

	@Test
	void addBlacklistedChip() {
		assumeWritable(c);
		try (var u = c.update(ADD_BLACKLISTED_CHIP)) {
			c.transaction(() -> {
				// No such board, so no insert
				assertEquals(0, u.call(NO_BOARD, -1, -1));
			});
		}
	}

	@Test
	void addBlacklistedCore() {
		assumeWritable(c);
		try (var u = c.update(ADD_BLACKLISTED_CORE)) {
			c.transaction(() -> {
				// No such board, so no insert
				assertEquals(0, u.call(NO_BOARD, -1, -1, -1));
			});
		}
	}

	@Test
	void addBlacklistedLink() {
		assumeWritable(c);
		try (var u = c.update(ADD_BLACKLISTED_LINK)) {
			c.transaction(() -> {
				// No such board, so no insert
				assertEquals(0, u.call(NO_BOARD, -1, -1, Direction.N));
			});
		}
	}

	@Test
	void clearBlacklistedChips() {
		assumeWritable(c);
		try (var u = c.update(CLEAR_BLACKLISTED_CHIPS)) {
			c.transaction(() -> {
				// No such board, so no delete
				assertEquals(0, u.call(NO_BOARD));
			});
		}
	}

	@Test
	void clearBlacklistedCores() {
		assumeWritable(c);
		try (var u = c.update(CLEAR_BLACKLISTED_CORES)) {
			c.transaction(() -> {
				// No such board, so no delete
				assertEquals(0, u.call(NO_BOARD));
			});
		}
	}

	@Test
	void markBoardBlacklistChanged() {
		assumeWritable(c);
		try (var u = c.update(MARK_BOARD_BLACKLIST_CHANGED)) {
			c.transaction(() -> {
				// No such board, so no delete
				assertEquals(0, u.call(0, NO_BOARD));
			});
		}
	}

	@Test
	void markBoardBlacklistSynched() {
		assumeWritable(c);
		try (var u = c.update(MARK_BOARD_BLACKLIST_SYNCHED)) {
			c.transaction(() -> {
				// No such board, so no delete
				assertEquals(0, u.call(0, NO_BOARD));
			});
		}
	}
}
