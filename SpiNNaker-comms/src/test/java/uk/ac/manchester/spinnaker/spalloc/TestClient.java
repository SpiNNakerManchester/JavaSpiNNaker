/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.spalloc.SupportUtils.OVERALL_TEST_TIMEOUT;
import static uk.ac.manchester.spinnaker.spalloc.SupportUtils.TIMEOUT;
import static uk.ac.manchester.spinnaker.spalloc.SupportUtils.assertTimeout;
import static uk.ac.manchester.spinnaker.spalloc.SupportUtils.withConnectedConnection;
import static uk.ac.manchester.spinnaker.spalloc.SupportUtils.withConnection;
import static uk.ac.manchester.spinnaker.spalloc.messages.State.READY;

import java.io.EOFException;
import java.net.ConnectException;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocProtocolTimeoutException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.Command;
import uk.ac.manchester.spinnaker.spalloc.messages.ExceptionResponse;
import uk.ac.manchester.spinnaker.spalloc.messages.JobsChangedNotification;
import uk.ac.manchester.spinnaker.spalloc.messages.MachinesChangedNotification;
import uk.ac.manchester.spinnaker.spalloc.messages.ReturnResponse;
import uk.ac.manchester.spinnaker.spalloc.messages.VersionCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;

class TestClient {
	static class MockCommand extends Command<Integer> {
		MockCommand(String name, int arg, String key, Object val) {
			super(name);
			addArg(arg);
			addKwArg(key, val);
		}
	}

	private static final String MOCK_RECEIVED_MESSAGE =
			"{\"command\": \"foo\", \"args\": [1], \"kwargs\": {\"bar\": 2}}";

	@Test
	void testConnectNoServer() throws Exception {
		withConnection((s, c, bgAccept) -> {
			s.close();
			/*
			 * If the server has gone, we're not going to successfully connect
			 * to it
			 */
			assertThrows(ConnectException.class, () -> c.connect());
			c.close();
		});
	}

	@Test
	void testConnect() throws Exception {
		withConnection((s, c, bgAccept) -> c.connect());
	}

	@Test
	void testConnectContext() throws Exception {
		try (var s = new MockServer()) {
			assertTimeoutPreemptively(OVERALL_TEST_TIMEOUT, () -> {
				try (var c =
						new SpallocClient("localhost", s.getPort(), null)) {
					var t = s.backgroundAccept();
					try (var context = c.withConnection()) {
						return; // do nothing
					} finally {
						t.flushjoin();
					}
				}
			});
		}
	}

	@Test
	@SuppressWarnings("MustBeClosed")
	void testClose() throws Exception {
		withConnection((s, c, bgAccept) -> {
			c.connect();
			c.close();
			c.close();
			c = new SpallocClient("localhost", s.getPort(), null);
			c.close();
		});
	}

	@Test
	void testReceiveJson() throws Exception {
		withConnection((s, c, bgAccept) -> {
			assertThrows(EOFException.class, () -> c.receiveResponse(null));
			c.connect();
			try (var sc = bgAccept.join()) {
				// Test the timeout
				long before = System.currentTimeMillis();
				assertThrows(SpallocProtocolTimeoutException.class,
						() -> c.receiveResponse(TIMEOUT));
				long after = System.currentTimeMillis();
				assertTimeout(before, after);

				// Transfer an actual message
				sc.send("{\"return\": \"bar\"}");
				assertEquals("\"bar\"",
						((ReturnResponse) c.receiveResponse(null))
								.getReturnValue());

				// Return a large message
				var a1 = new JSONArray();
				for (int i = 0; i < 1000; i++) {
					a1.put(i);
				}
				var o = new JSONObject();
				o.put("return", a1);
				sc.send(o);
				var a2 = new JSONArray(
						((ReturnResponse) c.receiveResponse(null))
								.getReturnValue());
				for (int i = 0; i < 1000; i++) {
					assertEquals(a1.get(i), a2.get(i));
				}

				// Test message ordering
				sc.send("{\"return\": \"foo\"}");
				sc.send("{\"return\": \"bar\"}");
				assertEquals("\"foo\"",
						((ReturnResponse) c.receiveResponse(null))
								.getReturnValue());
				assertEquals("\"bar\"",
						((ReturnResponse) c.receiveResponse(null))
								.getReturnValue());

				// Test other message types
				sc.send("{\"exception\": \"bar\"}");
				assertEquals("bar",
						((ExceptionResponse) c.receiveResponse(null))
								.getException());
				sc.send("{\"machines_changed\": [\"foo\",\"bar\"]}");
				assertEquals(List.of("foo", "bar"),
						((MachinesChangedNotification) c.receiveResponse(null))
								.getMachinesChanged());
				sc.send("{\"jobs_changed\": [1, 2]}");
				assertEquals(List.of(1, 2),
						((JobsChangedNotification) c.receiveResponse(null))
								.getJobsChanged());
			}

			// When socket becomes closed should fail
			assertThrows(EOFException.class, () -> c.receiveResponse(null));
		});
	}

	@Test
	void testSendJson() throws Exception {
		withConnection((s, c, bgAccept) -> {
			// Should fail before connecting
			assertThrows(EOFException.class,
					() -> c.sendCommand(new VersionCommand(), 250));

			c.connect();
			try (var sc = bgAccept.join()) {
				// Make sure we can send JSON
				c.sendCommand(new VersionCommand(), 250);
				JSONAssert.assertEquals(
						"{\"command\":\"version\",\"args\":[],\"kwargs\":{}}",
						sc.recv(), true);
			}
		});
	}

	@Test
	void testCall() throws Exception {
		withConnectedConnection((c, s) -> {
			// Basic calls should work
			s.send("{\"return\": \"Woo\"}");
			assertEquals("\"Woo\"",
					c.call(new MockCommand("foo", 1, "bar", 2), null));
			JSONAssert.assertEquals(MOCK_RECEIVED_MESSAGE, s.recv(), true);

			/*
			 * Should be able to cope with notifications arriving before return
			 * value
			 */
			s.send("{\"jobs_changed\": [1]}");
			s.send("{\"jobs_changed\": [2]}");
			s.send("{\"return\": \"Woo\"}");
			assertEquals("\"Woo\"",
					c.call(new MockCommand("foo", 1, "bar", 2), null));
			JSONAssert.assertEquals(MOCK_RECEIVED_MESSAGE, s.recv(), true);
			assertEquals(new JobsChangedNotification(1),
					c.waitForNotification(-1));
			assertEquals(new JobsChangedNotification(2),
					c.waitForNotification(-1));
			// Check we've drained the notification queue
			assertNull(c.waitForNotification(-1));

			// Should be able to timeout immediately
			long before = System.currentTimeMillis();
			assertThrows(SpallocProtocolTimeoutException.class,
					() -> c.call(new MockCommand("foo", 1, "bar", 2), TIMEOUT));
			long after = System.currentTimeMillis();
			JSONAssert.assertEquals(MOCK_RECEIVED_MESSAGE, s.recv(), true);
			assertTimeout(before, after);

			// Should be able to timeout after getting a notification
			s.send("{\"jobs_changed\": [3]}");
			before = System.currentTimeMillis();
			assertThrows(SpallocProtocolTimeoutException.class,
					() -> c.call(new MockCommand("foo", 1, "bar", 2), TIMEOUT));
			after = System.currentTimeMillis();
			JSONAssert.assertEquals(MOCK_RECEIVED_MESSAGE, s.recv(), true);
			assertTimeout(before, after);
			assertEquals(new JobsChangedNotification(3),
					c.waitForNotification(-1));
			assertNull(c.waitForNotification(-1));

			// Exceptions should transfer
			s.send("{\"exception\": \"something informative\"}");
			var t = assertThrows(SpallocServerException.class,
					() -> c.call(new MockCommand("foo", 1, "bar", 2), TIMEOUT));
			assertEquals("something informative", t.getMessage());
		});
	}

	@Test
	void testWaitForNotification() throws Exception {
		withConnectedConnection((c, s) -> {
			// Should be able to timeout
			assertThrows(SpallocProtocolTimeoutException.class,
					() -> c.waitForNotification(TIMEOUT));
			/*
			 * Should return null on negative timeout when no notifications
			 * arrived
			 */
			assertNull(c.waitForNotification(-1));

			// If notifications queued during call, should just return
			// those
			s.send("{\"jobs_changed\": [1]}");
			s.send("{\"jobs_changed\": [2]}");
			s.send("{\"return\": \"Woo\"}");
			assertEquals("\"Woo\"",
					c.call(new MockCommand("foo", 1, "bar", 2), null));
			JSONAssert.assertEquals(MOCK_RECEIVED_MESSAGE, s.recv(), true);
			assertEquals(new JobsChangedNotification(1),
					c.waitForNotification());
			assertEquals(new JobsChangedNotification(2),
					c.waitForNotification());

			// If no notifications queued, should listen for them
			s.send("{\"jobs_changed\": [3]}");
			assertEquals(new JobsChangedNotification(3),
					c.waitForNotification());
		});
	}

	@Test
	@SuppressWarnings({ "deprecation", "removal" })
	void testCommandCreateJob() throws Exception {
		withConnectedConnection((c, s) -> {
			// Old style create_job
			s.send("{\"return\": 123}");
			Map<String, Object> kwargs = Map.of("bar", 2, "owner", "dummy");
			assertEquals(123, c.createJob(List.of(1), kwargs));
			JSONAssert.assertEquals(
					"{\"command\": \"create_job\", \"args\": [1], "
							+ "\"kwargs\": {\"owner\": \"dummy\"}}",
					s.recv(), true);

			// New style create_job
			s.send("{\"return\": 123}");
			assertEquals(123, c.createJob(new CreateJob(1).owner("dummy")));
			JSONAssert.assertEquals(
					"{\"command\": \"create_job\", \"args\": [1], "
							+ "\"kwargs\": {\"owner\": \"dummy\"}}",
					s.recv(), true);
		});
	}

	@Test
	void testCommandListJobs() throws Exception {
		withConnectedConnection((c, s) -> {
			s.send("{\"return\":[{\"job_id\":123},{\"job_id\":99}]}");
			var result = c.listJobs();
			assertEquals(2, result.size());
			assertEquals(123, result.get(0).getJobID());
			assertEquals(99, result.get(1).getJobID());
			JSONAssert.assertEquals("{\"command\": \"list_jobs\", "
					+ "\"args\": [], \"kwargs\": {}}", s.recv(), true);
		});
	}

	@Test
	void testCommandListMachines() throws Exception {
		withConnectedConnection((c, s) -> {
			s.send("{\"return\":[{\"name\":\"foo\"},{\"name\":\"bar\"}]}");
			var result = c.listMachines();
			assertEquals(2, result.size());
			assertEquals("foo", result.get(0).getName());
			assertEquals("bar", result.get(1).getName());
			JSONAssert.assertEquals("{\"command\": \"list_machines\", "
					+ "\"args\": [], \"kwargs\": {}}", s.recv(), true);
		});
	}

	@Test
	void testCommandDestroyJob() throws Exception {
		withConnectedConnection((c, s) -> {
			s.send("{\"return\":null}");
			c.destroyJob(123, "gorp");
			JSONAssert.assertEquals(
					"{\"command\": \"destroy_job\", " + "\"args\": [123], "
							+ "\"kwargs\": {\"reason\":\"gorp\"}}",
					s.recv(), true);
		});
	}

	@Test
	void testCommandGetBoardPosition() throws Exception {
		withConnectedConnection((c, s) -> {
			s.send("{\"return\":[4,5,6]}");
			var pc = c.getBoardPosition("gorp", new BoardCoordinates(1, 2, 3));
			assertEquals(new BoardPhysicalCoordinates(4, 5, 6), pc);
			JSONAssert.assertEquals("{\"command\": \"get_board_position\", "
					+ "\"args\": [], \"kwargs\": "
					+ "{\"machine_name\":\"gorp\",\"x\":1,\"y\":2,\"z\":3}}",
					s.recv(), true);

			s.send("{\"return\":[7,8,9]}");
			var lc = c.getBoardPosition("gorp", pc);
			assertEquals(new BoardCoordinates(7, 8, 9), lc);
			JSONAssert.assertEquals("{\"command\": \"get_board_at_position\", "
					+ "\"args\": [], \"kwargs\": "
					+ "{\"machine_name\":\"gorp\",\"x\":4,\"y\":5,\"z\":6}}",
					s.recv(), true);
		});
	}

	@Test
	void testCommandGetJobMachineInfo() throws Exception {
		withConnectedConnection((c, s) -> {
			s.send("{\"return\":{\"boards\": [[1,2,3]],"
					+ "\"connections\": [[[1,2],\"gorp\"]]}}");
			var result = c.getJobMachineInfo(123);
			var boards = result.getBoards();
			assertEquals(1, boards.size());
			assertEquals(new BoardCoordinates(1, 2, 3), boards.get(0));
			var conns = result.getConnections();
			assertEquals(1, conns.size());
			assertEquals(new ChipLocation(1, 2), conns.get(0).getChip());
			assertEquals("gorp", conns.get(0).getHostname());
			JSONAssert.assertEquals(
					"{\"command\": \"get_job_machine_info\", "
							+ "\"args\": [123], \"kwargs\": {}}",
					s.recv(), true);
		});
	}

	@Test
	void testCommandGetJobState() throws Exception {
		withConnectedConnection((c, s) -> {
			s.send("{\"return\":{\"state\":3,\"power\":true}}");
			var result = c.getJobState(123);
			assertEquals(true, result.getPower());
			assertEquals(READY, result.getState());
			JSONAssert.assertEquals(
					"{\"command\": \"get_job_state\", "
							+ "\"args\": [123], \"kwargs\": {}}",
					s.recv(), true);
		});
	}

	@Test
	void testCommandNotifyJob() throws Exception {
		withConnectedConnection((c, s) -> {
			s.send("{\"return\":null}");
			c.notifyJob(123, false);
			JSONAssert.assertEquals(
					"{\"command\": \"no_notify_job\", "
							+ "\"args\": [123], \"kwargs\": {}}",
					s.recv(), true);
			s.send("{\"return\":null}");
			c.notifyJob(123, true);
			JSONAssert.assertEquals(
					"{\"command\": \"notify_job\", "
							+ "\"args\": [123], \"kwargs\": {}}",
					s.recv(), true);
		});
	}

	@Test
	void testCommandNotifyMachine() throws Exception {
		withConnectedConnection((c, s) -> {
			s.send("{\"return\":null}");
			c.notifyMachine("foo", false);
			JSONAssert.assertEquals(
					"{\"command\": \"no_notify_machine\", "
							+ "\"args\": [\"foo\"], \"kwargs\": {}}",
					s.recv(), true);
			s.send("{\"return\":null}");
			c.notifyMachine("foo", true);
			JSONAssert.assertEquals(
					"{\"command\": \"notify_machine\", "
							+ "\"args\": [\"foo\"], \"kwargs\": {}}",
					s.recv(), true);
		});
	}

	@Test
	void testCommandPower() throws Exception {
		withConnectedConnection((c, s) -> {
			s.send("{\"return\":null}");
			c.powerOffJobBoards(123);
			JSONAssert.assertEquals(
					"{\"command\": \"power_off_job_boards\", "
							+ "\"args\": [123], \"kwargs\": {}}",
					s.recv(), true);
			s.send("{\"return\":null}");
			c.powerOnJobBoards(123);
			JSONAssert.assertEquals(
					"{\"command\": \"power_on_job_boards\", "
							+ "\"args\": [123], \"kwargs\": {}}",
					s.recv(), true);
		});
	}

	@Test
	void testCommandVersion() throws Exception {
		withConnectedConnection((c, s) -> {
			s.send("{\"return\":\"1.2.3\"}");
			var result = c.version();
			assertEquals(1, result.majorVersion);
			assertEquals(2, result.minorVersion);
			assertEquals(3, result.revision);
			JSONAssert.assertEquals(
					"{\"command\": \"version\", \"args\": [], \"kwargs\": {}}",
					s.recv(), true);
		});
	}

	@Test
	void testCommandWhereIs() throws Exception {
		withConnectedConnection((c, s) -> {
			WhereIs result;

			s.send("{\"return\":{\"machine\":\"gorp\",\"logical\":[2,3,4]}}");
			result = c.whereIs(123, new ChipLocation(1, 2));
			assertEquals("gorp", result.getMachine());
			assertEquals(new BoardCoordinates(2, 3, 4), result.getLogical());
			JSONAssert.assertEquals(
					"{\"command\": \"where_is\", \"args\": [], \"kwargs\": {"
							+ "\"chip_x\":1,\"chip_y\":2,\"job_id\":123}}",
					s.recv(), true);

			s.send("{\"return\":{\"physical\":[2,3,4]}}");
			result = c.whereIs("gorp", new BoardCoordinates(1, 2, 3));
			assertEquals(new BoardPhysicalCoordinates(2, 3, 4),
					result.getPhysical());
			JSONAssert.assertEquals(
					"{\"command\": \"where_is\", \"args\": [], \"kwargs\": {"
							+ "\"x\":1,\"y\":2,\"z\":3,\"machine\":\"gorp\""
							+ "}}",
					s.recv(), true);

			s.send("{\"return\":{\"logical\":[2,3,4]}}");
			result = c.whereIs("gorp", new BoardPhysicalCoordinates(1, 2, 3));
			assertEquals(new BoardCoordinates(2, 3, 4), result.getLogical());
			JSONAssert.assertEquals(
					"{\"command\": \"where_is\", \"args\": [], \"kwargs\": {"
							+ "\"cabinet\":1,\"frame\":2,\"board\":3,"
							+ "\"machine\":\"gorp\"}}",
					s.recv(), true);

			s.send("{\"return\":{\"logical\":[2,3,4]}}");
			result = c.whereIs("gorp", new ChipLocation(0, 1));
			assertEquals(new BoardCoordinates(2, 3, 4), result.getLogical());
			JSONAssert.assertEquals(
					"{\"command\": \"where_is\", \"args\": [], \"kwargs\": {"
							+ "\"chip_x\":0,\"chip_y\":1,"
							+ "\"machine\":\"gorp\"}}",
					s.recv(), true);
		});
	}
}
