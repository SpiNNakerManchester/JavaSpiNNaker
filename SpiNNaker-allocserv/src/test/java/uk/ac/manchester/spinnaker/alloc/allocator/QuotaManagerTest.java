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
import static java.time.Duration.ofSeconds;
import static java.time.Instant.ofEpochSecond;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.BOARD;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.GROUP;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.INITIAL_QUOTA;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.MACHINE;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.USER;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.makeJob;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.setupDB1;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;

import java.io.IOException;
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

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connected;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;

@SpringBootTest
@SpringJUnitWebConfig(QuotaManagerTest.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + QuotaManagerTest.DB,
	"spalloc.historical-data.path=" + QuotaManagerTest.HIST_DB
})
class QuotaManagerTest extends SQLQueries implements SupportQueries {
	/** The DB file. */
	static final String DB = "target/qm_test.sqlite3";

	/** The DB file. */
	static final String HIST_DB = "target/qm_test_hist.sqlite3";

	private static final Logger log = getLogger(QuotaManagerTest.class);

	@Configuration
	@ComponentScan(basePackageClasses = SpallocProperties.class)
	static class Config {
	}

	@Autowired
	private DatabaseEngine db;

	/** Because the regular scheduled actions are not running. */
	private QuotaManager.TestAPI qm;

	@BeforeAll
	static void clearDB() throws IOException {
		var dbp = Paths.get(DB);
		if (exists(dbp)) {
			log.info("deleting old database: {}", dbp);
			delete(dbp);
		}
	}

	@BeforeEach
	@SuppressWarnings("deprecation")
	void checkSetup(@Autowired QuotaManager qm) {
		assumeTrue(db != null, "spring-configured DB engine absent");
		try (var c = db.getConnection()) {
			c.transaction(() -> setupDB1(c));
		}
		this.qm = qm.getTestAPI();
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
	private int makeFinishedJob(Connection c, int size, int time) {
		return makeJob(c, BOARD, DESTROYED, size, ofEpochSecond(0),
				ofEpochSecond(time), ofEpochSecond(time + time),
				ofSeconds(time), null);
	}

	private Object getQuota(Connection c) {
		try (var q = c.query(TEST_GET_QUOTA)) {
			return q.call1(MACHINE, USER).get().getObject("quota");
		}
	}

	private void setQuota(Connection c, Integer quota) {
		try (var u = c.update(TEST_SET_QUOTA)) {
			u.call(quota, GROUP);
		}
	}

	private void checkAndRollback(Connected act) {
		db.executeVoid(c -> {
			try {
				act.act(c);
			} finally {
				c.rollback();
			}
		});
	}

	/** Does a job get consolidated once and only once. */
	@Test
	public void consolidate() {
		checkAndRollback(c -> {
			int used = 100;
			makeFinishedJob(c, 1, used);
			assertEquals(INITIAL_QUOTA, getQuota(c));
			qm.doConsolidate(c);
			assertEquals(INITIAL_QUOTA - used, getQuota(c));
			qm.doConsolidate(c);
			assertEquals(INITIAL_QUOTA - used, getQuota(c));
		});
	}

	/** Does a job <em>not</em> get consolidated if there's no quota. */
	@Test
	public void noConsolidate() {
		checkAndRollback(c -> {
			// Delete the quota
			setQuota(c, null);
			makeFinishedJob(c, 1, 100);
			assertNull(getQuota(c));
			qm.doConsolidate(c);
			assertNull(getQuota(c));
		});
	}

}
