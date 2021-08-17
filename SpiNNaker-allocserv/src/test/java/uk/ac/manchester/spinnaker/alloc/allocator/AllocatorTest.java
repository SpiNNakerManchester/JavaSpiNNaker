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
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Transacted;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.model.JobState;

@SpringBootTest
@SpringJUnitWebConfig(AllocatorTest.Config.class)
@ActiveProfiles("unittest") // Disable booting CXF
@TestPropertySource(properties = {
	"spalloc.database-path=" + AllocatorTest.DB,
	// Stop scheduled tasks from running
	"spalloc.master.pause=true"
})
class AllocatorTest extends SQLQueries {
	private static final Logger log = getLogger(AllocatorTest.class);

	/** The name of the database file. */
	static final String DB = "alloc_test.sqlite3";

	@Configuration
	@ComponentScan(basePackageClasses = DatabaseEngine.class)
	static class Config {
	}

	@Autowired
	private DatabaseEngine db;

	private Connection conn;

	@Autowired
	private AllocatorTask alloc;

	void doTest(Transacted action) throws SQLException {
		try (Connection c = db.getConnection()) {
			transaction(c, () -> {
				try {
					conn = c;
					action.act();
				} finally {
					c.rollback();
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
	void checkSetup() throws SQLException {
		assumeTrue(db != null, "spring-configured DB engine absent");
		try (Connection c = db.getConnection()) {
			setupDB(c);
		}
	}

	private static final int MACHINE = 1000;

	private static final int BMP = 2000;

	private static final int BOARD = 3000;

	private static final int USER = 4000;

	private void setupDB(Connection c) throws SQLException {
		// A simple machine
		try (Update u = update(c,
				"INSERT OR IGNORE INTO machines("
						+ "machine_id, machine_name, width, height, [depth], "
						+ "default_quota, board_model) "
						+ "VALUES (?, ?, ?, ?, ?, ?, 5)")) {
			u.call(MACHINE, "foo", 1, 1, 3, 10000);
		}
		try (Update u = update(c,
				"INSERT OR IGNORE INTO bmp(bmp_id, machine_id, address, "
						+ "cabinet, frame) VALUES (?, ?, ?, ?, ?)")) {
			u.call(BMP, MACHINE, "1.1.1.1", 1, 1);
		}
		int b0 = BOARD, b1 = BOARD + 1, b2 = BOARD + 2;
		try (Update u = update(c,
				"INSERT OR IGNORE INTO boards(board_id, address, "
						+ "bmp_id, board_num, machine_id, x, y, z, "
						+ "root_x, root_y, board_power) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
			u.call(b0, "2.2.2.2", BMP, 0, MACHINE, 0, 0, 0, 0, 0, false);
			u.call(b1, "2.2.2.3", BMP, 1, MACHINE, 0, 0, 1, 8, 4, false);
			u.call(b2, "2.2.2.4", BMP, 2, MACHINE, 0, 0, 2, 4, 8, false);
		}
		try (Update u = update(c,
				"INSERT OR IGNORE INTO links(board_1, dir_1, board_2, dir_2) "
						+ "VALUES (?, ?, ?, ?)")) {
			u.call(b0, 0, b1, 3);
			u.call(b0, 1, b2, 4);
			u.call(b1, 2, b2, 5);
		}
		// A disabled permission-less user with a quota
		try (Update u = update(c,
				"INSERT OR IGNORE INTO user_info("
						+ "user_id, user_name, trust_level, disabled) "
						+ "VALUES (?, ?, ?, ?)")) {
			u.call(USER, "bar", TrustLevel.BASIC, true);
		}
		try (Update u = update(c, "INSERT OR REPLACE INTO quotas("
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
	private int makeJob(int time) throws SQLException {
		try (Update u = update(conn,
				"INSERT INTO jobs(machine_id, owner, job_state, "
						+ "create_timestamp, keepalive_interval) "
						+ "VALUES (?, ?, ?, 0, ?)")) {
			for (Integer k : u.keys(MACHINE, USER, JobState.QUEUED, time)) {
				return k;
			}
		}
		throw new RuntimeException("failed to insert job");
	}

	private void makeAllocBySizeRequest(int job, int size) throws SQLException {
		try (Update u =
				update(conn, "INSERT INTO job_request(job_id, num_boards) "
						+ "VALUES (?, ?)")) {
			u.call(job, size);
		}
	}

	private void makeAllocByDimensionsRequest(int job, int width, int height,
			int allowedDead) throws SQLException {
		try (Update u =
				update(conn, "INSERT INTO job_request(job_id, width, height, "
						+ "max_dead_boards) VALUES (?, ?, ?, ?)")) {
			u.call(job, width, height, allowedDead);
		}
	}

	private void makeAllocByBoardIdRequest(int job, int board)
			throws SQLException {
		try (Update u =
				update(conn, "INSERT INTO job_request(job_id, board_id) "
						+ "VALUES (?, ?)")) {
			u.call(job, board);
		}
	}

	private JobState getJobState(int job) throws SQLException {
		try (Query q = query(conn, GET_JOB)) {
			return q.call1(job).get().getEnum("job_state", JobState.class);
		}
	}

	private int getPendingPowerChanges() throws SQLException {
		try (Query q =
				query(conn, "SELECT COUNT(*) AS cnt FROM pending_changes")) {
			return q.call1().get().getInt("cnt");
		}
	}

	@Test
	void testDoAllocBySize1() throws SQLException {
		doTest(() -> {
			int job = makeJob(100);
			alloc.allocate(conn);

			assertEquals(JobState.QUEUED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());

			makeAllocBySizeRequest(job, 1);

			assertEquals(JobState.QUEUED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());

			alloc.allocate(conn);

			assertEquals(JobState.POWER, getJobState(job));
			assertEquals(1, getPendingPowerChanges());

			alloc.allocate(conn);

			assertEquals(JobState.POWER, getJobState(job));
			assertEquals(1, getPendingPowerChanges());

			alloc.destroyJob(job, "test");

			assertEquals(JobState.DESTROYED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());
		});
	}

	@Test
	void testDoAllocBySize3() throws SQLException {
		doTest(() -> {
			int job = makeJob(100);
			alloc.allocate(conn);

			assertEquals(JobState.QUEUED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());

			makeAllocBySizeRequest(job, 3);

			assertEquals(JobState.QUEUED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());

			alloc.allocate(conn);

			assertEquals(JobState.POWER, getJobState(job));
			assertEquals(3, getPendingPowerChanges());

			alloc.allocate(conn);

			assertEquals(JobState.POWER, getJobState(job));
			assertEquals(3, getPendingPowerChanges());

			alloc.destroyJob(job, "test");

			assertEquals(JobState.DESTROYED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());
		});
	}

	@Test
	void testDoAllocByDimensions() throws SQLException {
		doTest(() -> {
			int job = makeJob(100);
			alloc.allocate(conn);

			assertEquals(JobState.QUEUED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());

			makeAllocByDimensionsRequest(job, 1, 1, 0);

			assertEquals(JobState.QUEUED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());

			alloc.allocate(conn);

			assertEquals(JobState.POWER, getJobState(job));
			assertEquals(1, getPendingPowerChanges());

			alloc.allocate(conn);

			assertEquals(JobState.POWER, getJobState(job));
			assertEquals(1, getPendingPowerChanges());

			alloc.destroyJob(job, "test");

			assertEquals(JobState.DESTROYED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());
		});
	}

	@Test
	void testDoAllocByDimensions1x2() throws SQLException {
		doTest(() -> {
			int job = makeJob(100);
			alloc.allocate(conn);

			assertEquals(JobState.QUEUED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());

			makeAllocByDimensionsRequest(job, 1, 2, 0);

			assertEquals(JobState.QUEUED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());

			alloc.allocate(conn);

			assertEquals(JobState.POWER, getJobState(job));
			// Allocates a whole triad
			assertEquals(3, getPendingPowerChanges());

			alloc.allocate(conn);

			assertEquals(JobState.POWER, getJobState(job));
			assertEquals(3, getPendingPowerChanges());

			alloc.destroyJob(job, "test");

			assertEquals(JobState.DESTROYED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());
		});
	}

	@Test
	void testDoAllocByBoardId() throws SQLException {
		doTest(() -> {
			int job = makeJob(100);
			alloc.allocate(conn);

			assertEquals(JobState.QUEUED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());

			makeAllocByBoardIdRequest(job, BOARD);

			assertEquals(JobState.QUEUED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());

			alloc.allocate(conn);

			assertEquals(JobState.POWER, getJobState(job));
			assertEquals(1, getPendingPowerChanges());

			alloc.allocate(conn);

			assertEquals(JobState.POWER, getJobState(job));
			assertEquals(1, getPendingPowerChanges());

			alloc.destroyJob(job, "test");

			assertEquals(JobState.DESTROYED, getJobState(job));
			assertEquals(0, getPendingPowerChanges());
		});
	}
}
