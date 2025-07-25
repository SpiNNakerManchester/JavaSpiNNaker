/*
 * Copyright (c) 2022 The University of Manchester
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

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
//import static uk.ac.manchester.spinnaker.alloc.allocator.Cfg.BOARD;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.READY;

import java.io.IOException;
import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.messages.model.FPGA;

@SpringBootTest
@SpringJUnitWebConfig(TestSupport.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.transceiver.fpga-reload=true" // Enable reloading!
})
class FirmwareLoaderTest extends TestSupport {

	private static final int TEST_TIMEOUT = 10;

	@Autowired
	private AllocatorTask alloc;

	private BMPController.TestAPI bmpCtrl;

	@BeforeEach
	@SuppressWarnings("deprecation")
	void checkSetup(@Autowired TransceiverFactory txrxFactory,
			@Autowired BMPController bmpCtrl) throws IOException {
		MockTransceiver.installIntoFactory(txrxFactory);
		assumeTrue(db != null, "spring-configured DB engine absent");
		killDB();
		setupDB1();
		this.bmpCtrl = bmpCtrl.getTestAPI();
		this.bmpCtrl.prepare(true);
		this.bmpCtrl.resetTransceivers();
		this.bmpCtrl.clearBmpException();
	}

	@AfterEach
	void checkBMPDidNotThrow() {
		var exn = bmpCtrl.getBmpException();
		assertDoesNotThrow(() -> {
			if (exn != null) {
				throw exn;
			}
		}, "BMP controller must not have thrown, but did");
	}

	/**
	 * Expiry tests need a two second sleep to get things to tick over to *past*
	 * the expiration timestamp.
	 */
	private static final int DELAY_MS = 2000;

	/**
	 * Run a BMP processing cycle. <em>Do not</em> hold a transaction when
	 * calling this!
	 *
	 * @throws Exception
	 *             if anything fails
	 */
	private void processBMPRequests(Collection<Integer> bmps) throws Exception {
		bmpCtrl.processRequests(DELAY_MS, bmps);
	}

	@SuppressWarnings("deprecation")
	private TestAPI getAllocTester() {
		return alloc.getTestAPI(conn);
	}

	private void assertState(int jobId, JobState state, int requestCount,
			int powerCount) {
		var expected = asList(state, "req", requestCount, "power", powerCount);
		var got = asList(getJobState(jobId), "req", getJobRequestCount(),
				"power", getPendingPowerChanges());
		assertEquals(expected, got);
	}

	private void waitForBMPCompletion(int job, Collection<Integer> bmps)
			throws Exception {
		for (int i = 0; i < 10; i++) {
			processBMPRequests(bmps);
			if (getJobState(job) == READY) {
				break;
			}
			snooze1s();
			snooze1s();
		}
	}

	private void resetDBState(Connection c, int job) throws Exception {
		var allocations = c.transaction(
				() -> getAllocTester().destroyJob(job, "test"));
		processBMPRequests(allocations.bmps);
		c.transaction(() -> {
			if (log.isDebugEnabled()) {
				/*
				 * NB: explicit map(Object::toString) needed so strings are
				 * created while row is open; without that, the error messages
				 * are very strange.
				 */
				log.debug("state to force reset: {} {} {}",
						c.query("SELECT * FROM job_request")
								.call(Object::toString),
						c.query("SELECT * FROM pending_changes")
								.call(Object::toString),
						c.query("SELECT * FROM boards")
								.call(Object::toString));
			}
			c.update("DELETE FROM job_request").call();
			c.update("DELETE FROM pending_changes").call();
			// Board must be bootable now to not interfere with following tests
			c.update("UPDATE boards SET allocated_job = NULL, "
					+ "power_on_timestamp = 0, power_off_timestamp = 0").call();
		});
	}

	// The actual tests

	@Test
	@Timeout(TEST_TIMEOUT)
	public void bootSimply() throws Exception {
		try (var c = db.getConnection()) {
			this.conn = c;
			int job = c.transaction(() -> makeQueuedJob(1));
			log.info("job id = {}", job);
			try {
				makeAllocBySizeRequest(job, 1);
				var allocs = c.transaction(() -> getAllocTester().allocate());
				waitForBMPCompletion(job, allocs.bmps);

				assertState(job, READY, 0, 0);
			} finally {
				resetDBState(c, job);
			}
		}
	}

	@Test
	@Timeout(TEST_TIMEOUT)
	public void bootWithReboot() throws Exception {
		/*
		 * One failure triggers an extra power cycle of the board.
		 *
		 * FPGA_ALL is a bogus value for the result of the particular
		 * readFPGARegister() call we care about.
		 */
		MockTransceiver.fpgaResults.clear();
		MockTransceiver.fpgaResults.add(FPGA.FPGA_ALL);
		try (var c = db.getConnection()) {
			this.conn = c;
			int job = c.transaction(() -> makeQueuedJob(1));
			log.info("job id = {}", job);
			try {
				makeAllocBySizeRequest(job, 1);
				var allocs = c.transaction(() -> getAllocTester().allocate());
				waitForBMPCompletion(job, allocs.bmps);

				assertState(job, READY, 0, 0);
			} finally {
				resetDBState(c, job);
			}
		}
	}

	@Test
	@Timeout(TEST_TIMEOUT + TEST_TIMEOUT)
	public void bootWithFirmwareReload() throws Exception {
		/*
		 * Two failures trigger a the reloading of the firmware. This is slow
		 * (even slower in reality).
		 */
		MockTransceiver.fpgaResults.clear();
		MockTransceiver.fpgaResults.add(FPGA.FPGA_ALL);
		MockTransceiver.fpgaResults.add(FPGA.FPGA_ALL);
		try (var c = db.getConnection()) {
			this.conn = c;
			int job = c.transaction(() -> makeQueuedJob(1));
			log.info("job id = {}", job);
			try {
				makeAllocBySizeRequest(job, 1);
				var allocs = c.transaction(() -> getAllocTester().allocate());
				waitForBMPCompletion(job, allocs.bmps);

				assertState(job, READY, 0, 0);
			} finally {
				resetDBState(c, job);
			}
		}
	}
}
