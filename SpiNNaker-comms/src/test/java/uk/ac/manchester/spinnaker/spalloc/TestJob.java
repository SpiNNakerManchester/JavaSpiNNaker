/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc;

import static java.lang.Math.abs;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;
import static testconfig.BoardTestConfiguration.OWNER;
import static uk.ac.manchester.spinnaker.spalloc.MockServer.STOP;
import static uk.ac.manchester.spinnaker.spalloc.SpallocJob.DEFAULT_CONFIGURATION_SOURCE;
import static uk.ac.manchester.spinnaker.spalloc.SpallocJob.setConfigurationSource;
import static uk.ac.manchester.spinnaker.spalloc.SupportUtils.withAdvancedConnection;
import static uk.ac.manchester.spinnaker.spalloc.messages.State.READY;

import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;

import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.State;

class TestJob {
	@BeforeAll
	static void setupConfiguration() {
		setConfigurationSource("spalloc-test.ini");
	}

	@AfterAll
	static void resetConfiguration() {
		setConfigurationSource(DEFAULT_CONFIGURATION_SOURCE);
	}

	/**
	 * What the server will send (except for keepalive responses).
	 *
	 * @return The sequence of messages that the mock server will send.
	 */
	private static BlockingDeque<String> mockServerMessagesToSend() {
		var send = new LinkedBlockingDeque<String>();
		send.offer("{\"return\": 123}");
		send.offer("{\"return\": null}");
		send.offer("{\"return\": {\"state\": 2, \"power\": false}}");
		send.offer("{\"return\": null}");
		send.offer("{\"jobs_changed\": [123]}");
		send.offer("{\"return\": {\"state\": 3, \"power\": true}}");
		send.offer("{\"return\": {\"connections\":[[[0,0],\"10.11.223.33\"]],"
				+ "\"width\":8,"
				+ "\"machine_name\":\"Spin24b-223\","
				+ "\"boards\":[[4,5,6], [7,8,9]],"
				+ "\"height\":8}}");
		send.offer("{\"return\": null}");
		send.offer(STOP);
		return send;
	}

	/**
	 * Check that the sequence of non-keepalive messages is as expected.
	 *
	 * @param received
	 *            The sequence of messages the mock server has received.
	 */
	private static void assertMockServerReceived(
			BlockingDeque<JSONObject> received)
			throws JSONException, InterruptedException {
		JSONAssert.assertEquals("{\"command\": \"create_job\", "
				+ "\"args\": [1, 2, 3], \"kwargs\": {"
				+ "\"keepalive\": 1, \"owner\": \"java test harness\", "
				+ "\"tags\": [\"default\"]}}", received.take(), true);
		JSONAssert.assertEquals(
				"{\"command\": \"power_on_job_boards\", \"args\": [123], "
						+ "\"kwargs\": {}}",
				received.take(), true);
		JSONAssert.assertEquals(
				"{\"command\": \"get_job_state\", \"args\": [123], "
						+ "\"kwargs\": {}}",
				received.take(), true);
		JSONAssert
				.assertEquals("{\"command\": \"notify_job\", \"args\": [123], "
						+ "\"kwargs\": {}}", received.take(), true);
		JSONAssert.assertEquals(
				"{\"command\": \"get_job_state\", \"args\": [123], "
						+ "\"kwargs\": {}}",
				received.take(), true);
		JSONAssert.assertEquals(
				"{\"command\": \"get_job_machine_info\", \"args\": [123], "
						+ "\"kwargs\": {}}",
				received.take(), true);
		JSONAssert.assertEquals(
				"{\"command\": \"destroy_job\", \"args\": [123], "
						+ "\"kwargs\": {\"reason\": \"abc\"}}",
				received.take(), true);
		assertTrue(received.isEmpty(),
				"must have checked all received messages");
	}

	/**
	 * Check that the number of keepalives is sane, and that they're all
	 * identical.
	 *
	 * @param keepalives
	 *            The received keepalives
	 * @param keepaliveTarget
	 *            How many we expect
	 */
	private static void assertMockReceivedKeepalivesInRange(
			BlockingDeque<JSONObject> keepalives, int keepaliveTarget)
			throws InterruptedException {
		// Check the number of keepalives
		assertTrue(abs(keepaliveTarget - keepalives.size()) <= 1, () -> {
			return "number of keepalives needs to be close to "
					+ keepaliveTarget + ", but was " + keepalives.size();
		});
		// All should have the same message sent
		var first = keepalives.take();
		assertNotNull(first, "null in keepalive queue!");
		JSONAssert.assertEquals("{\"command\": \"job_keepalive\", "
				+ "\"args\": [123], \"kwargs\": {}}", first, true);
		while (!keepalives.isEmpty()) {
			JSONAssert.assertEquals(first, keepalives.take(), true);
		}
	}

	@ParameterizedTest
	//@formatter:off
	@CsvSource({
		"0,0,0,0,0,0,3",
		"250,0,0,0,0,0,3",
		"0,250,0,0,0,0,3",
		"0,0,250,0,0,0,3",
		"0,0,0,250,0,0,3",
		"0,0,0,0,250,0,3",
		"0,0,0,0,0,250,3",
		"250,250,250,250,250,250,5"
	})
	//@formatter:on
	void testCoreJobFlow(int delay1, int delay2, int delay3, int delay4,
			int delay5, int delay6, int keepaliveTarget) throws Exception {
		var send = mockServerMessagesToSend();
		var received = new LinkedBlockingDeque<JSONObject>();
		var keepalives = new LinkedBlockingDeque<JSONObject>();

		// Run the core of test
		withAdvancedConnection(send, received, keepalives, c -> {
			final int id;
			final List<BoardCoordinates> boards;
			final State state;
			final Boolean power;

			// The actual flow that we'd expect from normal usage
			try (var j = new SpallocJob(c, new CreateJob(1, 2, 3)
					.tags("default").owner(OWNER).keepAlive(1))) {
				/*
				 * Only non-standard bit in this part; it makes data caches in
				 * the job effectively never expire through timeout. The
				 * machinery under test will purge as necessary.
				 */
				j.statusCachePeriod *= 100;
				sleep(delay1);
				id = j.getID();
				sleep(1200);
				j.setPower(true);
				sleep(delay2);
				j.waitUntilReady(5000);
				sleep(delay3);
				state = j.getState();
				sleep(delay4);
				boards = j.getBoards();
				sleep(delay5);
				// Check that the cache is caching
				power = j.getPower();
				sleep(delay6);
				j.destroy("abc");
			}
			// Make sure that the keepalive has been shut down too
			sleep(500);

			assertEquals(123, id);
			assertEquals(List.of(new BoardCoordinates(4, 5, 6),
					new BoardCoordinates(7, 8, 9)), boards);
			assertEquals(READY, state);
			assertEquals(true, power);
		});

		// Check that the messages are as expected
		assertMockServerReceived(received);
		assertTrue(send.isEmpty(), "must have sent all expected responses");
		assertMockReceivedKeepalivesInRange(keepalives, keepaliveTarget);
	}

}
