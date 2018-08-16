package uk.ac.manchester.spinnaker.spalloc;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.spalloc.SupportUtils.withConnection;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.State;

class TestJob {
	void coreTestFlow() throws Exception {
		int id;
		List<BoardCoordinates> boards;
		State state;

		try (SpallocJob j = new SpallocJob("localhost", Arrays.asList(1, 2, 3),
				Collections.singletonMap("keepalive", 1.0))) {
			id = j.getID();
			Thread.sleep(1200);
			j.setPower(true);
			j.waitUntilReady(5000);
			boards = j.getBoards();
			state = j.getState();
			j.destroy("abc");
		}

		assertEquals(123, id);
		assertEquals(Arrays.asList(new BoardCoordinates(4, 5, 6),
				new BoardCoordinates(7, 8, 9)), boards);
		assertEquals(State.READY, state);
	}

	@Test
	void testCoreJobFlow() throws Exception {
		LinkedBlockingDeque<String> send = new LinkedBlockingDeque<>();
		LinkedBlockingDeque<JSONObject> received = new LinkedBlockingDeque<>();
		LinkedBlockingDeque<JSONObject> keepaliveQueue =
				new LinkedBlockingDeque<>();

		withConnection((s, c, bgAccept) -> {
			s.advancedEmulationMode(send, received, keepaliveQueue, bgAccept);

			send.offer("{\"return\": 123}");
			send.offer("{\"return\": null}");
			send.offer("{\"return\": {\"state\": 2}}");
			send.offer("{\"return\": null}");
			send.offer("{\"jobs_changed\": [123]}");
			send.offer("{\"return\": {\"state\": 3}}");
			send.offer("{\"return\": {\"boards\": [[4,5,6], [7,8,9]]}}");
			send.offer("{\"return\": null}");
			send.offer("STOP");

			coreTestFlow();

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
			JSONAssert.assertEquals(
					"{\"command\": \"notify_job\", \"args\": [123], "
							+ "\"kwargs\": {}}",
					received.take(), true);
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
			JSONAssert.assertEquals(
					"{\"command\": \"job_keepalive\", "
							+ "\"args\": [123], \"kwargs\": {}}",
					keepaliveQueue.peek(), true);
			// Number of keepalive requests
			assertEquals(3, keepaliveQueue.size());
			JSONObject first = keepaliveQueue.take();
			while (!keepaliveQueue.isEmpty()) {
				JSONAssert.assertEquals(first, keepaliveQueue.take(), true);
			}
		});

	}

}
