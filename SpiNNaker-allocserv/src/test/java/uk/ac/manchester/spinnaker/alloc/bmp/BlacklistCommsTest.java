/*
 * Copyright (c) 2022-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.bmp;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import uk.ac.manchester.spinnaker.alloc.TestSupport;
import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;
import uk.ac.manchester.spinnaker.utils.OneShotEvent;

/**
 * Tests whether we can do blacklist pushing and pulling through the DB so that
 * the front-end will be able to communicate with the BMP.
 */
@SpringBootTest
@SpringJUnitWebConfig(TestSupport.Config.class)
@ActiveProfiles("unittest")
@TestPropertySource(properties = {
	"spalloc.database-path=" + BlacklistCommsTest.DB,
	"spalloc.historical-data.path=" + BlacklistCommsTest.HIST_DB
})
class BlacklistCommsTest extends TestSupport {
	/** The DB file. */
	static final String DB = "target/blcomms_test.sqlite3";

	/** The DB file. */
	static final String HIST_DB = "target/blcomms_test_hist.sqlite3";

	/** Timeouts on individual tests, in seconds. */
	private static final int TEST_TIMEOUT = 15;

	/** Basic delay in fake BMP thread, in microseconds. */
	private static final int TEST_DELAY = 750;

	private static final String BMP_DONE_TOKEN = "BMP done";

	private static final Blacklist WRITE_BASELINE =
			new Blacklist("chip 4 4 core 6,4");

	@Autowired
	private MachineStateControl stateCtrl;

	private BMPController.TestAPI bmpCtrl;

	private TransceiverFactory.TestAPI txrxFactory;

	private ExecutorService exec;

	@BeforeAll
	static void clearDB() throws IOException {
		killDB(DB);
	}

	@BeforeEach
	@SuppressWarnings("deprecation")
	void checkSetup(@Autowired BMPController bmpCtrl,
			@Autowired TransceiverFactory txrxFactory) {
		assumeTrue(db != null, "spring-configured DB engine absent");
		setupDB1();
		// Get the test stuff set up
		this.bmpCtrl = bmpCtrl.getTestAPI();
		MockTransceiver.installIntoFactory(txrxFactory);
		this.txrxFactory = txrxFactory.getTestAPI();
		exec = newSingleThreadExecutor();
		this.bmpCtrl.clearBmpException();
	}

	@AfterEach
	void stopExecutor() {
		exec.shutdown();
		var exn = bmpCtrl.getBmpException();
		assertDoesNotThrow(() -> {
			if (exn != null) {
				throw new Exception(exn);
			}
		}, "BMP controller must not have thrown, but did");
	}

	/**
	 * A faked up running of the BMP worker thread because the main schedule is
	 * disabled.
	 *
	 * @return The future to wait for as part of shutting down. The value is
	 *         meaningless, but the exceptions potentially thrown are not.
	 * @throws InterruptedException If interrupted.
	 */
	private Future<String> bmpWorker()
			throws InterruptedException {
		var ready = new OneShotEvent();
		var future = exec.submit(() -> {
			ready.fire();
			// Time to allow main thread to submit the work we'll carry out
			Thread.sleep(TEST_DELAY);
			bmpCtrl.processRequests(TEST_DELAY);
			return BMP_DONE_TOKEN;
		});
		ready.await();
		return future;
	}

	@Test
	@Timeout(TEST_TIMEOUT)
	public void getSerialNumber() throws Exception {
		var bs = stateCtrl.findId(BOARD).orElseThrow();
		var future = bmpWorker();

		var serialNumber = stateCtrl.getSerialNumber(bs);

		assertEquals(BMP_DONE_TOKEN, future.get());
		assertEquals("gorp", serialNumber); // Magic value in dummy!
	}

	@Test
	@Timeout(TEST_TIMEOUT)
	public void readBlacklistFromMachine() throws Exception {
		var bs = stateCtrl.findId(BOARD).orElseThrow();
		var future = bmpWorker();

		var bl = stateCtrl.readBlacklistFromMachine(bs).orElseThrow();

		assertEquals(BMP_DONE_TOKEN, future.get());
		assertEquals(new Blacklist("chip 5 5 core 5"), bl);
	}

	@Test
	@Timeout(TEST_TIMEOUT)
	public void writeBlacklistToMachine() throws Exception {
		var bs = stateCtrl.findId(BOARD).orElseThrow();
		var future = bmpWorker();
		assertNotEquals(WRITE_BASELINE, txrxFactory.getCurrentBlacklist());

		stateCtrl.writeBlacklistToMachine(bs,
				new Blacklist("chip 4 4 core 4,6"));

		assertEquals(BMP_DONE_TOKEN, future.get());
		assertEquals(WRITE_BASELINE, txrxFactory.getCurrentBlacklist());
	}
}
