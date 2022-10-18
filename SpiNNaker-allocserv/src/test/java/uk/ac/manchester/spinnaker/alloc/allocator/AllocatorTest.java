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
package uk.ac.manchester.spinnaker.alloc.allocator;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.POWER;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.READY;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.TestSupport;
import uk.ac.manchester.spinnaker.alloc.allocator.AllocatorTask.TestAPI;
import uk.ac.manchester.spinnaker.alloc.bmp.BMPController;
import uk.ac.manchester.spinnaker.alloc.bmp.MockTransceiver;
import uk.ac.manchester.spinnaker.alloc.bmp.TransceiverFactory;
import uk.ac.manchester.spinnaker.alloc.model.JobState;

@SpringBootTest
@SpringJUnitWebConfig(TestSupport.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + AllocatorTest.DB,
	"spalloc.historical-data.path=" + AllocatorTest.HIST_DB
})
class AllocatorTest extends TestSupport {
	/** The name of the database file. */
	static final String DB = "target/alloc_test.sqlite3";

	/** The name of the database file. */
	static final String HIST_DB = "target/alloc_test-hist.sqlite3";

	@Autowired
	private AllocatorTask alloc;

	private BMPController.TestAPI bmpCtrl;

	@BeforeAll
	static void clearDB() throws IOException {
		killDB(DB);
	}

	@BeforeEach
	@SuppressWarnings("deprecation")
	void checkSetup(@Autowired TransceiverFactory txrxFactory,
			@Autowired BMPController bmpCtrl) {
		assumeTrue(db != null, "spring-configured DB engine absent");
		MockTransceiver.installIntoFactory(txrxFactory);
		setupDB3();
		this.bmpCtrl = bmpCtrl.getTestAPI();
	}

	private void assertState(int jobId, JobState state, int requestCount,
			int powerCount) {
		var expected = List.of(state, "req", requestCount, "power", powerCount);
		var got = List.of(getJobState(jobId), "req", getJobRequestCount(),
				"power", getPendingPowerChanges());
		assertEquals(expected, got);
	}

	private void assumeState(int jobId, JobState state, int requestCount,
			int powerCount) {
		var expected = List.of(state, "req", requestCount, "power", powerCount);
		var got = List.of(getJobState(jobId), "req", getJobRequestCount(),
				"power", getPendingPowerChanges());
		assumeTrue(expected.equals(got),
				() -> format("expected %s but got %s", expected, got));
	}

	/**
	 * Expiry tests need a two second sleep to get things to tick over to *past*
	 * the expiration timestamp.
	 */
	private static final int DELAY_MS = 2000;

	private void processBMPRequests() throws Exception {
		bmpCtrl.processRequests(DELAY_MS);
	}

	@SuppressWarnings("deprecation")
	private TestAPI getAllocTester() {
		return alloc.getTestAPI(conn);
	}

	@SuppressWarnings("CompileTimeConstant")
	private int countJobInTable(int job, String table) {
		// Table names CANNOT be parameters; they're not values
		return conn.query(format(
				"SELECT COUNT(*) AS c FROM %s WHERE job_id = :job", table))
				.call1(job).orElseThrow().getInt("c");
	}

	// The actual tests

	@Test
	public void allocateBySize1board() {
		doTransactionalTest(() -> {
			int job = makeQueuedJob(100);

			getAllocTester().allocate();

			assertState(job, QUEUED, 0, 0);

			makeAllocBySizeRequest(job, 1);

			assertState(job, QUEUED, 1, 0);

			assertTrue(getAllocTester().allocate());

			assertState(job, POWER, 0, 1);

			assertFalse(getAllocTester().allocate());

			assertState(job, POWER, 0, 1);

			getAllocTester().destroyJob(job, "test");

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void allocateBySize3boards() {
		doTransactionalTest(() -> {
			int job = makeQueuedJob(100);
			getAllocTester().allocate();

			assertState(job, QUEUED, 0, 0);

			makeAllocBySizeRequest(job, 3);

			assertState(job, QUEUED, 1, 0);

			assertTrue(getAllocTester().allocate());

			assertState(job, POWER, 0, 3);

			assertFalse(getAllocTester().allocate());

			assertState(job, POWER, 0, 3);

			getAllocTester().destroyJob(job, "test");

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void allocateByDimensions1x1x1() {
		doTransactionalTest(() -> {
			int job = makeQueuedJob(100);
			getAllocTester().allocate();

			assertState(job, QUEUED, 0, 0);

			makeAllocByDimensionsRequest(job, 1, 1, 2);

			assertState(job, QUEUED, 1, 0);

			assertTrue(getAllocTester().allocate());

			assertState(job, POWER, 0, 1);

			assertFalse(getAllocTester().allocate());

			assertState(job, POWER, 0, 1);

			getAllocTester().destroyJob(job, "test");

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void allocateByDimensions1x1x3() {
		doTransactionalTest(() -> {
			int job = makeQueuedJob(100);
			getAllocTester().allocate();

			assertState(job, QUEUED, 0, 0);

			makeAllocByDimensionsRequest(job, 1, 1, 0);

			assertState(job, QUEUED, 1, 0);

			assertTrue(getAllocTester().allocate());

			assertState(job, POWER, 0, 3);

			assertFalse(getAllocTester().allocate());

			assertState(job, POWER, 0, 3);

			getAllocTester().destroyJob(job, "test");

			assertState(job, DESTROYED, 0, 0);
		});
	}

	// This is a test with a job that is too large to fit
	@Test
	public void allocateByDimensions1x2() {
		doTransactionalTest(() -> {
			int job = makeQueuedJob(100);
			getAllocTester().allocate();

			assertState(job, QUEUED, 0, 0);

			makeAllocByDimensionsRequest(job, 1, 2, 0);

			assertState(job, QUEUED, 1, 0);

			var ex = assertThrows(IllegalArgumentException.class,
					() -> getAllocTester().allocate());
			assertEquals("that job cannot possibly fit on this machine",
					ex.getMessage());

			// No change in state
			assertState(job, QUEUED, 1, 0);

			getAllocTester().destroyJob(job, "test");

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void allocateByBoardId() {
		doTransactionalTest(() -> {
			int job = makeQueuedJob(100);
			getAllocTester().allocate();

			assertState(job, QUEUED, 0, 0);

			makeAllocByBoardIdRequest(job, BOARD);

			assertState(job, QUEUED, 1, 0);

			assertTrue(getAllocTester().allocate());

			assertState(job, POWER, 0, 1);

			assertFalse(getAllocTester().allocate());

			assertState(job, POWER, 0, 1);

			getAllocTester().destroyJob(job, "test");

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void expireInitial() {
		doTransactionalTest(() -> {
			int job = makeQueuedJob(1);
			snooze1s();
			snooze1s();

			assumeState(job, QUEUED, 0, 0);

			getAllocTester().expireJobs();

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void expireQueued1() {
		doTransactionalTest(() -> {
			int job = makeQueuedJob(1);
			getAllocTester().allocate();
			snooze1s();
			snooze1s();

			assumeState(job, QUEUED, 0, 0);

			getAllocTester().expireJobs();

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void expireQueued2() {
		doTransactionalTest(() -> {
			int job = makeQueuedJob(1);
			getAllocTester().allocate();
			makeAllocBySizeRequest(job, 1);
			snooze1s();
			snooze1s();

			assumeState(job, QUEUED, 1, 0);

			getAllocTester().expireJobs();

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void expirePower() {
		doTransactionalTest(() -> {
			int job = makeQueuedJob(1);
			makeAllocBySizeRequest(job, 1);
			getAllocTester().allocate();
			snooze1s();
			snooze1s();

			assumeState(job, POWER, 0, 1);

			getAllocTester().expireJobs();

			assertState(job, DESTROYED, 0, 0);
		});
	}

	private static final String HACK_POWER_TIMESTAMP =
			"UPDATE boards SET power_on_timestamp = power_on_timestamp - 1000";

	@Test
	public void expireReady() throws Exception {
		// This is messier; can't have a transaction open and roll it back
		try (var c = db.getConnection()) {
			this.conn = c;
			int job = c.transaction(() -> makeQueuedJob(1));
			try {
				makeAllocBySizeRequest(job, 1);
				c.transaction(() -> {
					getAllocTester().allocate();
				});
				snooze1s();
				snooze1s();
				processBMPRequests();

				assumeState(job, READY, 0, 0);

				c.transaction(() -> {
					getAllocTester().expireJobs();
				});

				assertState(job, DESTROYED, 0, 1);

				// HACK! Allow immediate switch off (OK because not real BMP)
				c.transaction(() -> c.update(HACK_POWER_TIMESTAMP).call());
				processBMPRequests();

				assertState(job, DESTROYED, 0, 0);
			} finally {
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

	@Test
	public void tombstone() throws Exception {
		doTransactionalTest(() -> {
			int job = makeQueuedJob(1);
			conn.update(TEST_SET_JOB_STATE).call(DESTROYED, job);
			conn.update(TEST_SET_JOB_DEATH_TIME).call(0, job);
			int preMain = countJobInTable(job, "jobs");
			assertTrue(preMain == 1,
					() -> "must have created a job we can tombstone");
			int preTomb = countJobInTable(job, "tombstone.jobs");

			var moved = getAllocTester().tombstone();

			assertEquals(1, moved.numJobs());
			// No resources were ever allocated, so no moves to do
			assertEquals(0, moved.numAllocs());
			assertEquals(preMain - 1, countJobInTable(job, "jobs"));
			assertEquals(preTomb + 1, countJobInTable(job, "tombstone.jobs"));
		});
	}
}
