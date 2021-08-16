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
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.model.JobState;

@SpringBootTest
@SpringJUnitWebConfig(AllocatorTest.Config.class)
@ActiveProfiles("unittest") // Disable booting CXF
@TestPropertySource(properties = {
	"spalloc.database-path=" + AllocatorTest.DB, "spalloc.master.pause=true"
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

	@Autowired
	private AllocatorTask alloc;

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
			u.call(MACHINE, "foo", 1, 1, 1, 10000);
		}
		try (Update u = update(c,
				"INSERT OR IGNORE INTO bmp(bmp_id, machine_id, address, "
						+ "cabinet, frame) VALUES (?, ?, ?, ?, ?)")) {
			u.call(BMP, MACHINE, "1.1.1.1", 1, 1);
		}
		try (Update u = update(c,
				"INSERT OR IGNORE INTO boards(board_id, address, "
						+ "bmp_id, board_num, machine_id, x, y, z, "
						+ "root_x, root_y, board_power) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
			u.call(BOARD, "2.2.2.2", BMP, 0, MACHINE, 0, 0, 0, 0, 0, false);
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
	private int makeJob(Connection c, int time) throws SQLException {
		try (Update u = update(c,
				"INSERT INTO jobs(machine_id, owner, root_id, job_state, "
						+ "create_timestamp, keepalive_interval) "
						+ "VALUES (?, ?, ?, ?, 0, ?)")) {
			for (Integer k : u.keys(MACHINE, USER, BOARD, JobState.QUEUED,
					time)) {
				return k;
			}
		}
		throw new RuntimeException("failed to insert job");
	}

	private void makeAllocBySizeRequest(Connection c, int job, int size)
			throws SQLException {
		try (Update u =
				update(c, "INSERT INTO job_request(job_id, num_boards) "
						+ "VALUES (?, ?)")) {
			u.call(job, size);
		}
	}

	private void makeAllocByDimensionsRequest(Connection c, int job, int width,
			int height) throws SQLException {
		try (Update u =
				update(c, "INSERT INTO job_request(job_id, width, height) "
						+ "VALUES (?, ?, ?)")) {
			u.call(job, width, height);
		}
	}

	private void makeAllocByBoardIdRequest(Connection c, int job, int board)
			throws SQLException {
		try (Update u = update(c, "INSERT INTO job_request(job_id, board_id) "
				+ "VALUES (?, ?)")) {
			u.call(job, board);
		}
	}

	private JobState getJobState(Query q, int job) throws SQLException {
		return q.call1(job).get().getEnum("job_state", JobState.class);
	}

	@Test
	void testDoAllocBySize() throws SQLException {
		db.executeVoid(c -> {
			// Does a job get consolidated once and only once
			try (Query q = query(c, GET_JOB)) {
				int job = makeJob(c, 100);
				alloc.allocate(c);
				assertEquals(JobState.QUEUED, getJobState(q, job));
				makeAllocBySizeRequest(c, job, 1);
				assertEquals(JobState.QUEUED, getJobState(q, job));
				alloc.allocate(c);
				assertEquals(JobState.POWER, getJobState(q, job));
				alloc.allocate(c);
				assertEquals(JobState.POWER, getJobState(q, job));
			} finally {
				c.rollback();
			}
		});
	}

	@Test
	void testDoAllocByDimensions() throws SQLException {
		db.executeVoid(c -> {
			// Does a job get consolidated once and only once
			try (Query q = query(c, GET_JOB)) {
				int job = makeJob(c, 100);
				alloc.allocate(c);
				assertEquals(JobState.QUEUED, getJobState(q, job));
				makeAllocByDimensionsRequest(c, job, 1, 1);
				assertEquals(JobState.QUEUED, getJobState(q, job));
				alloc.allocate(c);
				assertEquals(JobState.POWER, getJobState(q, job));
				alloc.allocate(c);
				assertEquals(JobState.POWER, getJobState(q, job));
			} finally {
				c.rollback();
			}
		});
	}

	@Test
	void testDoAllocByBoardId() throws SQLException {
		db.executeVoid(c -> {
			// Does a job get consolidated once and only once
			try (Query q = query(c, GET_JOB)) {
				int job = makeJob(c, 100);
				alloc.allocate(c);
				assertEquals(JobState.QUEUED, getJobState(q, job));
				makeAllocByBoardIdRequest(c, job, BOARD);
				assertEquals(JobState.QUEUED, getJobState(q, job));
				alloc.allocate(c);
				assertEquals(JobState.POWER, getJobState(q, job));
				alloc.allocate(c);
				assertEquals(JobState.POWER, getJobState(q, job));
			} finally {
				c.rollback();
			}
		});
	}
}
