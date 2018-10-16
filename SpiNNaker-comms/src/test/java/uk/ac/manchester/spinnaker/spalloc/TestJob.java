package uk.ac.manchester.spinnaker.spalloc;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.spalloc.MockServer.STOP;
import static uk.ac.manchester.spinnaker.spalloc.SpallocJob.DEFAULT_CONFIGURATION_SOURCE;
import static uk.ac.manchester.spinnaker.spalloc.SpallocJob.setConfigurationSource;
import static uk.ac.manchester.spinnaker.spalloc.SupportUtils.withConnection;
import static uk.ac.manchester.spinnaker.spalloc.messages.State.READY;

import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.State;

class TestJob {
	@BeforeAll
	private static void setupConfiguration() {
		setConfigurationSource("spalloc-test.ini");
	}

	@AfterAll
	private static void resetConfiguration() {
		setConfigurationSource(DEFAULT_CONFIGURATION_SOURCE);
	}

    // TODO test need fixing as calls to cliente are time dependant
    // so this test sometimes failes.
    // For example j.getState() may or may not call the client
    // depending on when the previous state was requested.
	// @Test
	void testCoreJobFlow() throws Exception {
		LinkedBlockingDeque<String> send = new LinkedBlockingDeque<>();
		LinkedBlockingDeque<JSONObject> received = new LinkedBlockingDeque<>();
		LinkedBlockingDeque<JSONObject> keepalives =
				new LinkedBlockingDeque<>();

		// Set the sequence of messages that the server will send
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

		// Run the core of test
		withConnection((s, c, bgAccept) -> {
			int id;
			List<BoardCoordinates> boards;
			State state;
			Boolean power;

			// Get the mock server to do message interchange interception
			s.advancedEmulationMode(send, received, keepalives, bgAccept);

			// The actual flow that we'd expect from normal usage
			try (SpallocJob j = new SpallocJob("localhost", asList(1, 2, 3),
					singletonMap("keepalive", 1.0))) {
				id = j.getID();
				sleep(1200);
				j.setPower(true);
				j.waitUntilReady(5000);
				boards = j.getBoards();
				state = j.getState();
				// Check that the cache is caching
				power = j.getPower();
				j.destroy("abc");
			}
			// Make sure that the keepalive has been shut down too
			sleep(1200);

			assertEquals(123, id);
			assertEquals(asList(new BoardCoordinates(4, 5, 6),
					new BoardCoordinates(7, 8, 9)), boards);
			assertEquals(READY, state);
			assertEquals(true, power);
		});

		// Check that the messages sent were the ones we expected
		JSONAssert.assertEquals("{\"command\": \"create_job\", "
				+ "\"args\": [1, 2, 3], \"kwargs\": {"
				+ "\"keepalive\": 1, \"max_dead_boards\": 0, "
				+ "\"min_ratio\": 0.333, \"owner\": \"dummy\", "
				+ "\"require_torus\": false}}", received.take(), true);
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
		assertTrue(send.isEmpty(), "must have sent all expected responses");

		// Check the number of keepalives
		assertEquals(3, keepalives.size());
		// All should have the same message sent
		JSONObject first = keepalives.take();
		assertNotNull(first, "null in keepalive queue!");
		JSONAssert.assertEquals(
				"{\"command\": \"job_keepalive\", "
						+ "\"args\": [123], \"kwargs\": {}}",
				first, true);
		while (!keepalives.isEmpty()) {
			JSONAssert.assertEquals(first, keepalives.take(), true);
		}
	}

}
