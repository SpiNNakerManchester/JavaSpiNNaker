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
package uk.ac.manchester.spinnaker.alloc.bmp;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
@Execution(SAME_THREAD)
class BlacklistCommsTest extends TestSupport {

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

	@BeforeEach
	@SuppressWarnings("deprecation")
	void checkSetup(@Autowired BMPController bmpCtrl,
			@Autowired TransceiverFactory txrxFactory) throws IOException {
		assumeTrue(db != null, "spring-configured DB engine absent");
		killDB();
		setupDB1();
		// Get the test stuff set up
		MockTransceiver.installIntoFactory(txrxFactory);
		this.txrxFactory = txrxFactory.getTestAPI();
		exec = newSingleThreadExecutor();
		this.bmpCtrl = bmpCtrl.getTestAPI();
		this.bmpCtrl.prepare();
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
	 * @param bmps
	 *            The IDs of the BMPs to process information for.
	 * @param count
	 *            The number of times to run the processing loop.
	 * @return The future to wait for as part of shutting down. The value is
	 *         meaningless, but the exceptions potentially thrown are not.
	 * @throws InterruptedException
	 *             If interrupted.
	 */
	private Future<String> bmpWorker(Collection<Integer> bmps, int count)
			throws InterruptedException {
		var ready = new OneShotEvent();
		var future = exec.submit(() -> {
			ready.fire();
			// Time to allow main thread to submit the work we'll carry out
			for (int i = 0; i < count; i++) {
				Thread.sleep(TEST_DELAY);
				bmpCtrl.processRequests(TEST_DELAY, bmps);
			}
			return BMP_DONE_TOKEN;
		});
		ready.await();
		return future;
	}

	@Test
	@Timeout(TEST_TIMEOUT)
	public void getSerialNumber() throws Exception {
		var bs = stateCtrl.findId(BOARD).orElseThrow();
		var future = bmpWorker(Set.of(bs.bmpId), 1);

		var serialNumber = stateCtrl.getSerialNumber(bs);

		assertEquals(BMP_DONE_TOKEN, future.get());
		assertEquals("gorp", serialNumber); // Magic value in dummy!
	}

	@Test
	@Timeout(TEST_TIMEOUT)
	public void readBlacklistFromMachine() throws Exception {
		var bs = stateCtrl.findId(BOARD).orElseThrow();
		var future = bmpWorker(Set.of(bs.bmpId), 1);

		var bl = stateCtrl.readBlacklistFromMachine(bs.id, bs.bmpId)
				.orElseThrow();

		assertEquals(BMP_DONE_TOKEN, future.get());
		assertEquals(new Blacklist("chip 5 5 core 5"), bl);
	}

	@Test
	@Timeout(TEST_TIMEOUT)
	public void writeBlacklistToMachine() throws Exception {
		var bs = stateCtrl.findId(BOARD).orElseThrow();
		var future = bmpWorker(Set.of(bs.bmpId), 2);
		assertNotEquals(WRITE_BASELINE, txrxFactory.getCurrentBlacklist());

		stateCtrl.writeBlacklistToMachine(bs.id, bs.bmpId,
				new Blacklist("chip 4 4 core 4,6"));

		assertEquals(BMP_DONE_TOKEN, future.get());
		assertEquals(WRITE_BASELINE, txrxFactory.getCurrentBlacklist());
	}
}
