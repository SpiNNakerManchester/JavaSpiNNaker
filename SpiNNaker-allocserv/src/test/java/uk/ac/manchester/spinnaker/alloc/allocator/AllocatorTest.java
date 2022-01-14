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

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;

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
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Transacted;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.security.TrustLevel;

@SpringBootTest
@SpringJUnitWebConfig(AllocatorTest.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + AllocatorTest.DB,
	"spalloc.historical-data.path=" + AllocatorTest.HIST_DB
})
@SuppressWarnings("deprecation") // Yes, we're allowed to poke inside here
class AllocatorTest extends SQLQueries {
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

	void doTest(Transacted action) {
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
			c.transaction(() -> setupDB(c));
		}
	}

	private static final int MACHINE = 1000;

	private static final int BMP = 2000;

	private static final int BOARD = 3000;

	private static final int USER = 4000;

	private void setupDB(Connection c) {
		// A simple machine
		try (Update u = c.update("INSERT OR IGNORE INTO machines("
				+ "machine_id, machine_name, width, height, [depth], "
				+ "default_quota, board_model) "
				+ "VALUES (?, ?, ?, ?, ?, ?, 5)")) {
			u.call(MACHINE, "foo", 1, 1, 3, 10000);
		}
		try (Update u = c.update(
				"INSERT OR IGNORE INTO bmp(bmp_id, machine_id, address, "
						+ "cabinet, frame) VALUES (?, ?, ?, ?, ?)")) {
			u.call(BMP, MACHINE, "1.1.1.1", 1, 1);
		}
		int b0 = BOARD, b1 = BOARD + 1, b2 = BOARD + 2;
		try (Update u =
				c.update("INSERT OR IGNORE INTO boards(board_id, address, "
						+ "bmp_id, board_num, machine_id, x, y, z, "
						+ "root_x, root_y, board_power) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
			u.call(b0, "2.2.2.2", BMP, 0, MACHINE, 0, 0, 0, 0, 0, false);
			u.call(b1, "2.2.2.3", BMP, 1, MACHINE, 0, 0, 1, 8, 4, false);
			u.call(b2, "2.2.2.4", BMP, 2, MACHINE, 0, 0, 2, 4, 8, false);
		}
		try (Update u = c.update(
				"INSERT OR IGNORE INTO links(board_1, dir_1, board_2, dir_2) "
						+ "VALUES (?, ?, ?, ?)")) {
			u.call(b0, 0, b1, 3);
			u.call(b0, 1, b2, 4);
			u.call(b1, 2, b2, 5);
		}
		// A disabled permission-less user with a quota
		try (Update u = c.update("INSERT OR IGNORE INTO user_info("
				+ "user_id, user_name, trust_level, disabled) "
				+ "VALUES (?, ?, ?, ?)")) {
			u.call(USER, "bar", TrustLevel.BASIC, true);
		}
		try (Update u = c.update("INSERT OR REPLACE INTO quotas("
				+ "user_id, machine_id, quota) VALUES (?, ?, ?)")) {
			u.call(USER, MACHINE, 1024);
		}
	}

	/**
	 * Insert a live job of a given size and length.
	 *
	 * @param c
	 *            DB connection
	 * @param size
	 *            Number of boards
	 * @param time
	 *            Length of time (seconds)
	 * @return Job ID
	 */
	private int makeJob(int time) {
		try (Update u =
				conn.update("INSERT INTO jobs(machine_id, owner, job_state, "
						+ "create_timestamp, keepalive_interval, "
						+ "keepalive_timestamp) "
						+ "VALUES (?, ?, ?, 0, ?, ?)")) {
			return conn.transaction(
					() -> u.key(MACHINE, USER, JobState.QUEUED, time, now())
							.orElseThrow(() -> new RuntimeException(
									"failed to insert job")));
		}
	}

	private void makeAllocBySizeRequest(int job, int size) {
		try (Update u =
				conn.update("INSERT INTO job_request(job_id, num_boards) "
						+ "VALUES (?, ?)")) {
			conn.transaction(() -> u.call(job, size));
		}
	}

	private void makeAllocByDimensionsRequest(int job, int width, int height,
			int allowedDead) {
		try (Update u =
				conn.update("INSERT INTO job_request(job_id, width, height, "
						+ "max_dead_boards) VALUES (?, ?, ?, ?)")) {
			conn.transaction(() -> u.call(job, width, height, allowedDead));
		}
	}

	private void makeAllocByBoardIdRequest(int job, int board) {
		try (Update u = conn.update("INSERT INTO job_request(job_id, board_id) "
				+ "VALUES (?, ?)")) {
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
		try (Query q = conn.query("SELECT COUNT(*) AS cnt FROM job_request")) {
			return conn.transaction(() -> q.call1().get().getInt("cnt"));
		}
	}

	private int getPendingPowerChanges() {
		try (Query q =
				conn.query("SELECT COUNT(*) AS cnt FROM pending_changes")) {
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
				() -> "expected " + expected + " but got " + got);
	}

	@Test
	void testDoAllocBySize1() {
		doTest(() -> {
			int job = makeJob(100);
			alloc.allocate(conn);

			assertState(job, JobState.QUEUED, 0, 0);

			makeAllocBySizeRequest(job, 1);

			assertState(job, JobState.QUEUED, 1, 0);

			assertTrue(alloc.allocate(conn));

			assertState(job, JobState.POWER, 0, 1);

			assertFalse(alloc.allocate(conn));

			assertState(job, JobState.POWER, 0, 1);

			alloc.destroyJob(conn, job, "test");

			assertState(job, JobState.DESTROYED, 0, 0);
		});
	}

	@Test
	void testDoAllocBySize3() {
		doTest(() -> {
			int job = makeJob(100);
			alloc.allocate(conn);

			assertState(job, JobState.QUEUED, 0, 0);

			makeAllocBySizeRequest(job, 3);

			assertState(job, JobState.QUEUED, 1, 0);

			assertTrue(alloc.allocate(conn));

			assertState(job, JobState.POWER, 0, 3);

			assertFalse(alloc.allocate(conn));

			assertState(job, JobState.POWER, 0, 3);

			alloc.destroyJob(conn, job, "test");

			assertState(job, JobState.DESTROYED, 0, 0);
		});
	}

	@Test
	void testDoAllocByDimensions() {
		doTest(() -> {
			int job = makeJob(100);
			alloc.allocate(conn);

			assertState(job, JobState.QUEUED, 0, 0);

			makeAllocByDimensionsRequest(job, 1, 1, 0);

			assertState(job, JobState.QUEUED, 1, 0);

			assertTrue(alloc.allocate(conn));

			assertState(job, JobState.POWER, 0, 1);

			assertFalse(alloc.allocate(conn));

			assertState(job, JobState.POWER, 0, 1);

			alloc.destroyJob(conn, job, "test");

			assertState(job, JobState.DESTROYED, 0, 0);
		});
	}

	@Test
	void testDoAllocByDimensions1x2() {
		doTest(() -> {
			int job = makeJob(100);
			alloc.allocate(conn);

			assertState(job, JobState.QUEUED, 0, 0);

			makeAllocByDimensionsRequest(job, 1, 2, 0);

			assertState(job, JobState.QUEUED, 1, 0);

			assertTrue(alloc.allocate(conn));

			// Allocates a whole triad
			assertState(job, JobState.POWER, 0, 3);

			assertFalse(alloc.allocate(conn));

			assertState(job, JobState.POWER, 0, 3);

			alloc.destroyJob(conn, job, "test");

			assertState(job, JobState.DESTROYED, 0, 0);
		});
	}

	@Test
	void testDoAllocByBoardId() {
		doTest(() -> {
			int job = makeJob(100);
			alloc.allocate(conn);

			assertState(job, JobState.QUEUED, 0, 0);

			makeAllocByBoardIdRequest(job, BOARD);

			assertState(job, JobState.QUEUED, 1, 0);

			assertTrue(alloc.allocate(conn));

			assertState(job, JobState.POWER, 0, 1);

			assertFalse(alloc.allocate(conn));

			assertState(job, JobState.POWER, 0, 1);

			alloc.destroyJob(conn, job, "test");

			assertState(job, JobState.DESTROYED, 0, 0);
		});
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

	@Test
	void testExpireInitial() {
		doTest(() -> {
			int job = makeJob(1);
			snooze();

			assumeState(job, JobState.QUEUED, 0, 0);

			alloc.expireJobs(conn);

			assertState(job, JobState.DESTROYED, 0, 0);
		});
	}

	@Test
	void testExpireQueued1() {
		doTest(() -> {
			int job = makeJob(1);
			alloc.allocate(conn);
			snooze();

			assumeState(job, JobState.QUEUED, 0, 0);

			alloc.expireJobs(conn);

			assertState(job, JobState.DESTROYED, 0, 0);
		});
	}

	@Test
	void testExpireQueued2() {
		doTest(() -> {
			int job = makeJob(1);
			alloc.allocate(conn);
			makeAllocBySizeRequest(job, 1);
			snooze();

			assumeState(job, JobState.QUEUED, 1, 0);

			alloc.expireJobs(conn);

			assertState(job, JobState.DESTROYED, 0, 0);
		});
	}

	@Test
	void testExpirePower() {
		doTest(() -> {
			int job = makeJob(1);
			makeAllocBySizeRequest(job, 1);
			alloc.allocate(conn);
			snooze();

			assumeState(job, JobState.POWER, 0, 1);

			alloc.expireJobs(conn);

			assertState(job, JobState.DESTROYED, 0, 0);
		});
	}

	@Test
	void testExpireReady() throws Exception {
		// This is messier; can't have a transaction open and roll it back
		try (Connection c = db.getConnection()) {
			this.conn = c;
			int job = makeJob(1);
			try {
				makeAllocBySizeRequest(job, 1);
				c.transaction(() -> {
					alloc.allocate(c);
				});
				snooze();
				processBMPRequests();

				assumeState(job, JobState.READY, 0, 0);

				c.transaction(() -> {
					alloc.expireJobs(c);
				});

				assertState(job, JobState.DESTROYED, 0, 1);

				// HACK! Allow immediate switch off (OK because not real BMP)
				c.transaction(() -> {
					c.update("UPDATE boards SET "
							+ "power_on_timestamp = power_on_timestamp - 1000")
							.call();
				});
				processBMPRequests();

				assertState(job, JobState.DESTROYED, 0, 0);
			} finally {
				c.transaction(() -> {
					alloc.destroyJob(c, job, "test");
					c.update("DELETE FROM job_request").call();
					c.update("DELETE FROM pending_changes").call();
				});
			}
		}
	}

	private void processBMPRequests() throws Exception {
		bmpCtrl.processRequests();
		snooze();
		bmpCtrl.processRequests();
	}

	private int countJobInTable(int job, String table) {
		return conn.query(
				"SELECT COUNT(*) AS c FROM " + table + " WHERE job_id = :job")
				.call1(job).get().getInt("c");
	}

	@Test
	void tombstone() throws Exception {
		doTest(() -> {
			int job = makeJob(1);
			conn.update("UPDATE jobs SET job_state = 4 WHERE job_id = :job")
					.call(job);
			conn.update(
					"UPDATE jobs SET death_timestamp = 0 WHERE job_id = :job")
					.call(job);
			int preMain = countJobInTable(job, "jobs");
			assertTrue(preMain == 1,
					() -> "must have created a job we can tombstone");
			int preTomb = countJobInTable(job, "tombstone.jobs");
			List<Integer> movedJobs = alloc.tombstone(conn);
			assertEquals(1, movedJobs.size());
			assertEquals(preMain - 1, countJobInTable(job, "jobs"));
			assertEquals(preTomb + 1, countJobInTable(job, "tombstone.jobs"));
		});
	}
}
