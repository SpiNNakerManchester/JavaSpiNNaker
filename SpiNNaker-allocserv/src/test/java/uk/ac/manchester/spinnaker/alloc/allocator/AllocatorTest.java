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
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.BOARD;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.makeJob;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.setupDB3;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.POWER;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.QUEUED;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.READY;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.bmp.BMPController;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Transacted;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.model.JobState;

@SpringBootTest
@SpringJUnitWebConfig(AllocatorTest.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + AllocatorTest.DB,
	"spalloc.historical-data.path=" + AllocatorTest.HIST_DB
})
@SuppressWarnings("deprecation") // Yes, we're allowed to poke inside here
class AllocatorTest extends SQLQueries implements SupportQueries {
	private static final Logger log = getLogger(AllocatorTest.class);

	/** The name of the database file. */
	static final String DB = "target/alloc_test.sqlite3";

	/** The name of the database file. */
	static final String HIST_DB = "target/alloc_test-hist.sqlite3";

	@Configuration
	@ComponentScan(basePackageClasses = SpallocProperties.class)
	static class Config {
	}

	@Autowired
	private DatabaseEngine db;

	private Connection conn;

	@Autowired
	private AllocatorTask alloc;

	@Autowired
	private BMPController bmpCtrl;

	private void doTest(Transacted action) {
		try (Connection c = db.getConnection()) {
			c.transaction(() -> {
				try {
					conn = c;
					action.act();
				} finally {
					try {
						c.rollback();
					} catch (DataAccessException ignored) {
					}
					conn = null;
				}
			});
		}
	}

	@BeforeAll
	static void clearDB() throws IOException {
		Path dbp = Paths.get(DB);
		if (exists(dbp)) {
			log.info("deleting old database: {}", dbp);
			delete(dbp);
		}
	}

	@BeforeEach
	void checkSetup() {
		assumeTrue(db != null, "spring-configured DB engine absent");
		try (Connection c = db.getConnection()) {
			c.transaction(() -> setupDB3(c));
		}
	}

	/**
	 * Insert a live job. Needs a matching allocation request.
	 *
	 * @param c
	 *            DB connection
	 * @param time
	 *            Length of time for keepalive (seconds)
	 * @return Job ID
	 */
	private int makeQueuedJob(int time) {
		return makeJob(conn, null, QUEUED, null, ofEpochMilli(0), null, null,
				ofSeconds(time), now());
	}

	private void makeAllocBySizeRequest(int job, int size) {
		try (Update u = conn.update(TEST_INSERT_REQ_SIZE)) {
			conn.transaction(() -> u.call(job, size));
		}
	}

	private void makeAllocByDimensionsRequest(int job, int width, int height,
			int allowedDead) {
		try (Update u = conn.update(TEST_INSERT_REQ_DIMS)) {
			conn.transaction(() -> u.call(job, width, height, allowedDead));
		}
	}

	private void makeAllocByBoardIdRequest(int job, int board) {
		try (Update u = conn.update(TEST_INSERT_REQ_BOARD)) {
			conn.transaction(() -> u.call(job, board));
		}
	}

	private JobState getJobState(int job) {
		try (Query q = conn.query(GET_JOB)) {
			return conn.transaction(() -> q.call1(job).get()
					.getEnum("job_state", JobState.class));
		}
	}

	private int getJobRequestCount() {
		try (Query q = conn.query(TEST_COUNT_REQUESTS)) {
			return conn.transaction(() -> q.call1(QUEUED).get().getInt("cnt"));
		}
	}

	private int getPendingPowerChanges() {
		try (Query q = conn.query(TEST_COUNT_POWER_CHANGES)) {
			return conn.transaction(() -> q.call1().get().getInt("cnt"));
		}
	}

	private void assertState(int jobId, JobState state, int requestCount,
			int powerCount) {
		List<?> expected =
				asList(state, "req", requestCount, "power", powerCount);
		List<?> got = asList(getJobState(jobId), "req", getJobRequestCount(),
				"power", getPendingPowerChanges());
		assertEquals(expected, got);
	}

	private void assumeState(int jobId, JobState state, int requestCount,
			int powerCount) {
		List<?> expected =
				asList(state, "req", requestCount, "power", powerCount);
		List<?> got = asList(getJobState(jobId), "req", getJobRequestCount(),
				"power", getPendingPowerChanges());
		assumeTrue(expected.equals(got),
				() -> format("expected %s but got %s", expected, got));
	}

	/**
	 * Expiry tests need a two second sleep to get things to tick over to *past*
	 * the expiration timestamp.
	 */
	private static void snooze() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			assumeTrue(false, "sleep() was interrupted");
		}
	}

	private void processBMPRequests() throws Exception {
		bmpCtrl.processRequests();
		snooze();
		bmpCtrl.processRequests();
	}

	private int countJobInTable(int job, String table) {
		// Table names CANNOT be parameters; they're not values
		return conn.query(format(
				"SELECT COUNT(*) AS c FROM %s WHERE job_id = :job", table))
				.call1(job).get().getInt("c");
	}

	// The actual tests

	@Test
	public void allocateBySize1board() {
		doTest(() -> {
			int job = makeQueuedJob(100);
			alloc.allocate(conn);

			assertState(job, QUEUED, 0, 0);

			makeAllocBySizeRequest(job, 1);

			assertState(job, QUEUED, 1, 0);

			assertTrue(alloc.allocate(conn));

			assertState(job, POWER, 0, 1);

			assertFalse(alloc.allocate(conn));

			assertState(job, POWER, 0, 1);

			alloc.destroyJob(conn, job, "test");

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void allocateBySize3boards() {
		doTest(() -> {
			int job = makeQueuedJob(100);
			alloc.allocate(conn);

			assertState(job, QUEUED, 0, 0);

			makeAllocBySizeRequest(job, 3);

			assertState(job, QUEUED, 1, 0);

			assertTrue(alloc.allocate(conn));

			assertState(job, POWER, 0, 3);

			assertFalse(alloc.allocate(conn));

			assertState(job, POWER, 0, 3);

			alloc.destroyJob(conn, job, "test");

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void allocateByDimensions1x1() {
		doTest(() -> {
			int job = makeQueuedJob(100);
			alloc.allocate(conn);

			assertState(job, QUEUED, 0, 0);

			makeAllocByDimensionsRequest(job, 1, 1, 0);

			assertState(job, QUEUED, 1, 0);

			assertTrue(alloc.allocate(conn));

			assertState(job, POWER, 0, 1);

			assertFalse(alloc.allocate(conn));

			assertState(job, POWER, 0, 1);

			alloc.destroyJob(conn, job, "test");

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void allocateByDimensions1x2() {
		doTest(() -> {
			int job = makeQueuedJob(100);
			alloc.allocate(conn);

			assertState(job, QUEUED, 0, 0);

			makeAllocByDimensionsRequest(job, 1, 2, 0);

			assertState(job, QUEUED, 1, 0);

			assertTrue(alloc.allocate(conn));

			// Allocates a whole triad
			assertState(job, POWER, 0, 3);

			assertFalse(alloc.allocate(conn));

			assertState(job, POWER, 0, 3);

			alloc.destroyJob(conn, job, "test");

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void allocateByBoardId() {
		doTest(() -> {
			int job = makeQueuedJob(100);
			alloc.allocate(conn);

			assertState(job, QUEUED, 0, 0);

			makeAllocByBoardIdRequest(job, BOARD);

			assertState(job, QUEUED, 1, 0);

			assertTrue(alloc.allocate(conn));

			assertState(job, POWER, 0, 1);

			assertFalse(alloc.allocate(conn));

			assertState(job, POWER, 0, 1);

			alloc.destroyJob(conn, job, "test");

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void expireInitial() {
		doTest(() -> {
			int job = makeQueuedJob(1);
			snooze();

			assumeState(job, QUEUED, 0, 0);

			alloc.expireJobs(conn);

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void expireQueued1() {
		doTest(() -> {
			int job = makeQueuedJob(1);
			alloc.allocate(conn);
			snooze();

			assumeState(job, QUEUED, 0, 0);

			alloc.expireJobs(conn);

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void expireQueued2() {
		doTest(() -> {
			int job = makeQueuedJob(1);
			alloc.allocate(conn);
			makeAllocBySizeRequest(job, 1);
			snooze();

			assumeState(job, QUEUED, 1, 0);

			alloc.expireJobs(conn);

			assertState(job, DESTROYED, 0, 0);
		});
	}

	@Test
	public void expirePower() {
		doTest(() -> {
			int job = makeQueuedJob(1);
			makeAllocBySizeRequest(job, 1);
			alloc.allocate(conn);
			snooze();

			assumeState(job, POWER, 0, 1);

			alloc.expireJobs(conn);

			assertState(job, DESTROYED, 0, 0);
		});
	}

	private static final String HACK_POWER_TIMESTAMP =
			"UPDATE boards SET power_on_timestamp = power_on_timestamp - 1000";

	@Test
	public void expireReady() throws Exception {
		// This is messier; can't have a transaction open and roll it back
		try (Connection c = db.getConnection()) {
			this.conn = c;
			int job = makeQueuedJob(1);
			try {
				makeAllocBySizeRequest(job, 1);
				c.transaction(() -> {
					alloc.allocate(c);
				});
				snooze();
				processBMPRequests();

				assumeState(job, READY, 0, 0);

				c.transaction(() -> {
					alloc.expireJobs(c);
				});

				assertState(job, DESTROYED, 0, 1);

				// HACK! Allow immediate switch off (OK because not real BMP)
				c.transaction(() -> c.update(HACK_POWER_TIMESTAMP).call());
				processBMPRequests();

				assertState(job, DESTROYED, 0, 0);
			} finally {
				c.transaction(() -> {
					alloc.destroyJob(c, job, "test");
					c.update("DELETE FROM job_request").call();
					c.update("DELETE FROM pending_changes").call();
				});
			}
		}
	}

	@Test
	public void tombstone() throws Exception {
		doTest(() -> {
			int job = makeQueuedJob(1);
			conn.update(TEST_SET_JOB_STATE).call(DESTROYED, job);
			conn.update(TEST_SET_JOB_DEATH_TIME).call(0, job);
			int preMain = countJobInTable(job, "jobs");
			assertTrue(preMain == 1,
					() -> "must have created a job we can tombstone");
			int preTomb = countJobInTable(job, "tombstone.jobs");
			AllocatorTask.Copied moved = alloc.tombstone(conn);
			assertEquals(1, moved.numJobs());
			// No resources were ever allocated, so no moves to do
			assertEquals(0, moved.numAllocs());
			assertEquals(preMain - 1, countJobInTable(job, "jobs"));
			assertEquals(preTomb + 1, countJobInTable(job, "tombstone.jobs"));
		});
	}
}
