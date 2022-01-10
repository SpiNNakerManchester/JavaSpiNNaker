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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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

import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.storage.Parameter;
import uk.ac.manchester.spinnaker.storage.ResultColumn;

@SpringBootTest
@SpringJUnitWebConfig(QuotaManagerTest.Config.class)
@ActiveProfiles("unittest") // Disable booting CXF
@TestPropertySource(properties = {
	"spalloc.database-path=" + QuotaManagerTest.DB,
	"spalloc.historical-data.path=" + QuotaManagerTest.HIST_DB,
	// Stop scheduled tasks from running
	"spalloc.pause=true"
})
class QuotaManagerTest extends SQLQueries {
	/** The DB file. */
	static final String DB = "target/qm_test.sqlite3";

	/** The DB file. */
	static final String HIST_DB = "target/qm_test_hist.sqlite3";

	@Parameter("machine_id")
	@Parameter("user_id")
	@ResultColumn("quota")
	private static final String GET_QUOTA =
			"SELECT quota FROM quotas WHERE machine_id = ? AND user_id = ?";

	private static final Logger log = getLogger(QuotaManagerTest.class);

	@Configuration
	@ComponentScan(basePackageClasses = SpallocProperties.class)
	static class Config {
	}

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private QuotaManager qm;

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
				+ "board_model) VALUES (?, ?, ?, ?, ?, 5)")) {
			u.call(MACHINE, "foo", 1, 1, 1);
		}
		try (Update u = c.update(
				"INSERT OR IGNORE INTO bmp(bmp_id, machine_id, address, "
						+ "cabinet, frame) VALUES (?, ?, ?, ?, ?)")) {
			u.call(BMP, MACHINE, "1.1.1.1", 1, 1);
		}
		try (Update u =
				c.update("INSERT OR IGNORE INTO boards(board_id, address, "
						+ "bmp_id, board_num, machine_id, x, y, z, "
						+ "root_x, root_y, board_power) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
			u.call(BOARD, "2.2.2.2", BMP, 0, MACHINE, 0, 0, 0, 0, 0, false);
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
	 * Insert a dead job of a given size and length.
	 *
	 * @param c
	 *            DB connection
	 * @param size
	 *            Number of boards
	 * @param time
	 *            Length of time (seconds)
	 * @return Job ID
	 */
	private int makeJob(Connection c, int size, int time) {
		try (Update u = c.update(
				"INSERT INTO jobs(machine_id, owner, root_id, job_state, "
						+ "create_timestamp, allocation_timestamp, "
						+ "death_timestamp, allocation_size, "
						+ "keepalive_interval) VALUES "
						+ "(?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
			int t0 = 0;
			return u.key(MACHINE, USER, BOARD, JobState.DESTROYED, t0,
					t0 + time, t0 + time + time, size, time).orElseThrow(
							() -> new RuntimeException("failed to insert job"));
		}
	}

	private Object getQuota(Connection c) {
		try (Query q = c.query(GET_QUOTA)) {
			return q.call1(MACHINE, USER).get().getObject("quota");
		}
	}

	private void setQuota(Connection c, Integer quota) {
		try (Update u = c.update("UPDATE quotas SET quota = :quota "
				+ "WHERE machine_id = :machine AND user_id = :user")) {
			u.call(quota, MACHINE, USER);
		}
	}

	@Test
	void testDoConsolidate() {
		db.executeVoid(c -> {
			// Does a job get consolidated once and only once
			try {
				makeJob(c, 1, 100);
				assertEquals(1024, getQuota(c));
				qm.doConsolidate(c);
				assertEquals(924, getQuota(c));
				qm.doConsolidate(c);
				assertEquals(924, getQuota(c));
			} finally {
				c.rollback();
			}
		});
	}

	@Test
	void testDoNoConsolidate() {
		db.executeVoid(c -> {
			try {
				// Delete the quota
				setQuota(c, null);
				makeJob(c, 1, 100);
				// Does a job NOT get consolidated if there's no quota
				assertNull(getQuota(c));
				qm.doConsolidate(c);
				assertNull(getQuota(c));
			} finally {
				c.rollback();
			}
		});
	}

}
