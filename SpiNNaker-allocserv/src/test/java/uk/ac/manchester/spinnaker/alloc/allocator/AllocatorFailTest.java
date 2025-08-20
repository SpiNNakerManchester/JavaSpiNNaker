/*
 * Copyright (c) 2025 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.allocator;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.READY;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import net.jcip.annotations.NotThreadSafe;
import uk.ac.manchester.spinnaker.alloc.TestSupport;
import uk.ac.manchester.spinnaker.alloc.allocator.AllocatorTask.TestAPI;
import uk.ac.manchester.spinnaker.alloc.bmp.BMPController;
import uk.ac.manchester.spinnaker.alloc.bmp.MockFailTransceiver;
import uk.ac.manchester.spinnaker.alloc.bmp.MockTransceiver;
import uk.ac.manchester.spinnaker.alloc.bmp.TransceiverFactory;
import uk.ac.manchester.spinnaker.alloc.model.JobState;

@SpringBootTest
@SpringJUnitWebConfig(TestSupport.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	// These tests sometimes hold transactions for a long time; this is OK
	"spalloc.sqlite.lock-note-threshold=2200ms",
	"spalloc.sqlite.lock-warn-threshold=3s"
})
@NotThreadSafe
public class AllocatorFailTest extends TestSupport {

	@Autowired
	private AllocatorTask alloc;

	private BMPController.TestAPI bmpCtrl;

	private TransceiverFactory txrxFactory;

	@BeforeEach
	@SuppressWarnings("deprecation")
	void checkSetup(@Autowired TransceiverFactory txrxFactory,
			@Autowired BMPController bmpCtrl) throws IOException {

		assumeTrue(db != null, "spring-configured DB engine absent");
		killDB();
		setupDB3();
		this.txrxFactory = txrxFactory;
		this.bmpCtrl = bmpCtrl.getTestAPI();
		this.bmpCtrl.prepare(false);
		this.bmpCtrl.resetTransceivers();
		this.bmpCtrl.clearBmpException();
	}

	@Test
	public void testBoardFail() {
		// This is messier; can't have a transaction open and roll it back
		try (var c = db.getConnection()) {
			this.conn = c;
			int job = c.transaction(() -> makeQueuedJob(100000));
			try {
				log.info("Created job {}", job);
				makeAllocBySizeRequest(job, 1);
				var allocations = c.transaction(() -> {
					return getAllocTester().allocate();
				});

				var boards = c.query(GET_JOB_BOARDS).call(row -> {
					return row.getInt("board_id");
				}, job);
				assertEquals(1, boards.size());
				var firstBoard = boards.get(0);
				log.info("Allocated board: {}", firstBoard);

				// Check the board last power on time
				var powerOffTime = c.query(GET_BOARD_POWER_INFO).call1(row -> {
					return row.getLong("power_off_timestamp");
				}, firstBoard);
				log.info("Power off time: {}", powerOffTime);

				// Make a transceiver that will fail the allocation
				MockFailTransceiver.installIntoFactory(txrxFactory);
				allocations.updateBMPs();
				snooze1s();
				snooze1s();

				// By now it should have all failed, so the boards should
				// no longer be allocated
				var jobDeallocatedBoards = c.query(GET_JOB_BOARDS).call(row -> {
					return row.getInt("board_id");
				}, job);
				assertEquals(0, jobDeallocatedBoards.size(),
						"Boards should be unallocated, but got: "
						+ jobDeallocatedBoards);

				// The failed board should have powered off again so new time
				var newPowerOffTime = c.query(GET_BOARD_POWER_INFO).call1(row -> {
					return row.getLong("power_off_timestamp");
				}, firstBoard);
				log.info("New Power off time: {}", newPowerOffTime);
				assertNotEquals(powerOffTime, newPowerOffTime);

				// The job should be queued again, with a single request
				assertState(job, QUEUED, 1, 0);

				// Make a transceiver that *doesn't* fail the requests
				MockTransceiver.installIntoFactory(txrxFactory);
				MockTransceiver.fpgaResults.clear();
				this.bmpCtrl.resetTransceivers();

				// So we should be able to reallocate the job to a new board
				var newAllocations = c.transaction(() -> {
					return getAllocTester().allocate();
				});
				var newBoards = c.query(GET_JOB_BOARDS).call(row -> {
					return row.getInt("board_id");
				}, job);
				assertEquals(1, newBoards.size());
				var newFirstBoard = newBoards.get(0);
				log.info("Allocated new board: {}", newFirstBoard);

				// That board should be different
				assertNotEquals(firstBoard, newFirstBoard);

				// And now lets see if it works...
				newAllocations.updateBMPs();
				snooze1s();
				snooze1s();
				assertState(job, READY, 0, 0);

				// We should also now not have any job requests
				var requestCount = c.query(TEST_COUNT_REQUESTS_FOR_JOB).call1(
						row -> {return row.getInt("cnt");}, job).get();
				assertEquals(0, requestCount);
			} finally {
				log.info("Finishing test by deleting things");
				c.transaction(() -> {
					// Reset the state
					getAllocTester().destroyJob(job, "test");
					c.update("DELETE FROM job_request").call();
					c.update("DELETE FROM pending_changes").call();
					c.update("UPDATE boards SET allocated_job = NULL, "
							+ "power_on_timestamp = 0, "
							+ "power_off_timestamp = 0").call();
				});
			}
		}
	}

	@SuppressWarnings("deprecation")
	private TestAPI getAllocTester() {
		return alloc.getTestAPI(conn);
	}

	private void assertState(int jobId, JobState state, int requestCount,
			int powerCount) {
		var expected = List.of(state, "req", requestCount, "power", powerCount);
		var got = List.of(getJobState(jobId), "req", getJobRequestCount(),
				"power", getPendingPowerChanges());
		assertEquals(expected, got);
	}
}
