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
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;
//import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.BOARD;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.makeJob;
import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.setupDB1;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.allocator.AllocatorTask.TestAPI;
import uk.ac.manchester.spinnaker.alloc.bmp.BMPController;
import uk.ac.manchester.spinnaker.alloc.bmp.MockTransceiver;
import uk.ac.manchester.spinnaker.alloc.bmp.TransceiverFactory;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.messages.model.FPGA;

@SpringBootTest
@SpringJUnitWebConfig(FirmwareLoaderTest.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + FirmwareLoaderTest.DB,
	"spalloc.historical-data.path=" + FirmwareLoaderTest.HIST_DB,
	"spalloc.transceiver.fpga-reload=true"
})
class FirmwareLoaderTest extends SQLQueries implements SupportQueries {
	private static final Logger log = getLogger(FirmwareLoaderTest.class);

	/** The name of the database file. */
	static final String DB = "target/firmware_test.sqlite3";

	/** The name of the database file. */
	static final String HIST_DB = "target/firmware_test-hist.sqlite3";

	@Configuration
	@ComponentScan(basePackageClasses = SpallocProperties.class)
	static class Config {
	}

	@Autowired
	private DatabaseEngine db;

	private Connection conn;

	@Autowired
	private AllocatorTask alloc;

	private BMPController.TestAPI bmpCtrl;

	@BeforeAll
	static void clearDB() throws IOException {
		Path dbp = Paths.get(DB);
		if (exists(dbp)) {
			log.info("deleting old database: {}", dbp);
			delete(dbp);
		}
	}

	@BeforeEach
	@SuppressWarnings("deprecation")
	void checkSetup(@Autowired TransceiverFactory txrxFactory,
			@Autowired BMPController bmpCtrl) {
		MockTransceiver.installIntoFactory(txrxFactory);
		assumeTrue(db != null, "spring-configured DB engine absent");
		try (Connection c = db.getConnection()) {
			c.transaction(() -> setupDB1(c));
		}
		this.bmpCtrl = bmpCtrl.getTestAPI();
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

	private JobState getJobState(int job) {
		try (Query q = conn.query(GET_JOB)) {
			return conn.transaction(() -> q.call1(job).get()
					.getEnum("job_state", JobState.class));
		}
	}

	private static final int DELAY_MS = 2000;

	/**
	 * Expiry tests need a two second sleep to get things to tick over to *past*
	 * the expiration timestamp.
	 */
	private static void snooze() {
		try {
			Thread.sleep(DELAY_MS);
		} catch (InterruptedException e) {
			assumeTrue(false, "sleep() was interrupted");
		}
	}

	private void processBMPRequests() throws Exception {
		bmpCtrl.processRequests(DELAY_MS);
	}

	@SuppressWarnings("deprecation")
	private TestAPI getAllocTester() {
		return alloc.getTestAPI(conn);
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

	private void makeAllocBySizeRequest(int job, int size) {
		try (Update u = conn.update(TEST_INSERT_REQ_SIZE)) {
			conn.transaction(() -> u.call(job, size));
		}
	}

	// The actual tests

	@Test
	public void bootWithReboot() throws Exception {
		MockTransceiver.fpgaResults.add(FPGA.FPGA_ALL.value);
		try (Connection c = db.getConnection()) {
			this.conn = c;
			int job = makeQueuedJob(1);
			log.info("job id = {}", job);
			try {
				makeAllocBySizeRequest(job, 1);
				c.transaction(() -> {
					getAllocTester().allocate();
				});
				snooze();
				processBMPRequests();

				assertState(job, READY, 0, 0);
			} finally {
				c.transaction(() -> {
					getAllocTester().destroyJob(job, "test");
					c.update("DELETE FROM job_request").call();
					c.update("DELETE FROM pending_changes").call();
				});
			}
		}
	}

	@Test
	public void bootWithFirmwareReload() throws Exception {
		MockTransceiver.fpgaResults.add(FPGA.FPGA_ALL.value);
		MockTransceiver.fpgaResults.add(FPGA.FPGA_ALL.value);
		try (Connection c = db.getConnection()) {
			this.conn = c;
			int job = makeQueuedJob(1);
			log.info("job id = {}", job);
			try {
				makeAllocBySizeRequest(job, 1);
				c.transaction(() -> {
					getAllocTester().allocate();
				});
				snooze();
				processBMPRequests();
				// This is a long delay!
				snooze();
				snooze();
				snooze();
				snooze();
				snooze();
				snooze();
				processBMPRequests();

				assertState(job, READY, 0, 0);
			} finally {
				c.transaction(() -> {
					getAllocTester().destroyJob(job, "test");
					c.update("DELETE FROM job_request").call();
					c.update("DELETE FROM pending_changes").call();
				});
			}
		}
	}
}
