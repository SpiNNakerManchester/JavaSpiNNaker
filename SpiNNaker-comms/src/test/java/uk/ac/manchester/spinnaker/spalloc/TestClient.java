package uk.ac.manchester.spinnaker.spalloc;

import static org.junit.jupiter.api.Assertions.*;

import java.io.EOFException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.Arrays;

import javax.xml.ws.Holder;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import uk.ac.manchester.spinnaker.spalloc.messages.Command;
import uk.ac.manchester.spinnaker.spalloc.messages.ExceptionResponse;
import uk.ac.manchester.spinnaker.spalloc.messages.JobsChangedNotification;
import uk.ac.manchester.spinnaker.spalloc.messages.MachinesChangedNotification;
import uk.ac.manchester.spinnaker.spalloc.messages.ReturnResponse;
import uk.ac.manchester.spinnaker.spalloc.messages.VersionCommand;
import uk.ac.manchester.spinnaker.utils.OneShotEvent;

class TestClient {
	private static final void assertTimeout(long before, long after) {
		assertTrue(after - before > 100 && after - before < 200,
				"measured timeout must be between 0.1s and 0.2s (measured"
						+ (after - before) + "ms)");
	}

	private static final Duration OVERALL_TEST_TIMEOUT = Duration.ofSeconds(10);
	private static final int TIMEOUT = 101;

	private Thread backgroundAccept(MockServer s) throws Exception {
		OneShotEvent started = new OneShotEvent();
		Thread main = Thread.currentThread();
		Holder<Exception> problem = new Holder<>();
		Thread t = new Thread(() -> {
			try {
				s.listen();
				started.fire();
				s.connect();
			} catch (Exception e) {
				problem.value = e;
				main.interrupt();
			}
		});
		t.start();
		try {
			started.await();
		} catch (InterruptedException e) {
			if (problem.value != null) {
				throw problem.value;
			}
			throw e;
		}
		return t;
	}

	interface WithConn {
		void act(MockServer s, SpallocClient c, Thread bgAccept)
				throws Exception;
	}

	static class MockCommand extends Command<Integer> {
		MockCommand(String name, int arg, String key, Object val) {
			super(name);
			addArg(arg);
			addKwArg(key, val);
		}
	}

	private static final String MOCK_RECEIVED_MESSAGE =
			"{\"command\": \"foo\", \"args\": [1], \"kwargs\": {\"bar\": 2}}";

	void withConnection(WithConn op) throws Exception {
		assertTimeoutPreemptively(OVERALL_TEST_TIMEOUT, () -> {
			MockServer s = new MockServer();
			try {
				SpallocClient c = new SpallocClient("localhost", 22244, null);
				Thread bgAccept = backgroundAccept(s);
				op.act(s, c, bgAccept);
				bgAccept.join();
				c.close();
			} finally {
				s.close();
			}
		});
	}

	@Test
	void testConnectNoServer() throws Exception {
		assertTimeoutPreemptively(OVERALL_TEST_TIMEOUT, () -> {
			SpallocClient c = new SpallocClient("localhost", 22244, null);
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
		assertTimeoutPreemptively(OVERALL_TEST_TIMEOUT, () -> {
			MockServer s = new MockServer();
			try (SpallocClient c =
					new SpallocClient("localhost", 22244, null)) {
				Thread t = backgroundAccept(s);
				try (AutoCloseable context = c.withConnection()) {
					// do nothing
				}
				t.join();
			}
			s.close();
		});
	}

	@Test
	void testClose() throws Exception {
		withConnection((s, c, bgAccept) -> {
			c.connect();
			c.close();
			c.close();
			c = new SpallocClient("localhost", 22244, null);
			c.close();
		});
	}

	@Test
	void testReceiveJson() throws Exception {
		withConnection((s, c, bgAccept) -> {
			assertThrows(EOFException.class, () -> c.receiveResponse(null));
			c.connect();
			bgAccept.join();

			// Test the timeout
			long before = System.currentTimeMillis();
			assertThrows(SpallocProtocolTimeoutException.class,
					() -> c.receiveResponse(TIMEOUT));
			long after = System.currentTimeMillis();
			assertTimeout(before, after);

			// Transfer an actual message
			s.send("{\"return\": \"bar\"}");
			assertEquals("\"bar\"", ((ReturnResponse) c.receiveResponse(null))
					.getReturnValue());

			// Return a large message
			JSONArray a1 = new JSONArray();
			for (int i = 0; i < 1000; i++) {
				a1.put(i);
			}
			JSONObject o = new JSONObject();
			o.put("return", a1);
			s.send(o);
			JSONArray a2 =
					new JSONArray(((ReturnResponse) c.receiveResponse(null))
							.getReturnValue());
			for (int i = 0; i < 1000; i++) {
				assertEquals(a1.get(i), a2.get(i));
			}

			// Test message ordering
			s.send("{\"return\": \"foo\"}");
			s.send("{\"return\": \"bar\"}");
			assertEquals("\"foo\"", ((ReturnResponse) c.receiveResponse(null))
					.getReturnValue());
			assertEquals("\"bar\"", ((ReturnResponse) c.receiveResponse(null))
					.getReturnValue());

			// Test other message types
			s.send("{\"exception\": \"bar\"}");
			assertEquals("bar", ((ExceptionResponse) c.receiveResponse(null))
					.getException());
			s.send("{\"machines_changed\": [\"foo\",\"bar\"]}");
			assertEquals(Arrays.asList("foo", "bar"),
					((MachinesChangedNotification) c.receiveResponse(null))
							.getMachinesChanged());
			s.send("{\"jobs_changed\": [1, 2]}");
			assertEquals(Arrays.asList(1, 2),
					((JobsChangedNotification) c.receiveResponse(null))
							.getJobsChanged());

			// When socket becomes closed should fail
			s.close();
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
			bgAccept.join();

			// Make sure we can send JSON
			c.sendCommand(new VersionCommand(), 250);
			JSONAssert.assertEquals(
					"{\"command\":\"version\",\"args\":[],\"kwargs\":{}}",
					s.recv(), true);
		});
	}

	@Test
	void testCall() throws Exception {
		withConnection((s, c, bgAccept) -> {
			c.connect();
			bgAccept.join();

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
			Throwable t = assertThrows(SpallocServerException.class,
					() -> c.call(new MockCommand("foo", 1, "bar", 2), TIMEOUT));
			assertEquals("something informative", t.getMessage());
		});
	}

	@Test
	void testWaitForNotification() throws Exception {
		withConnection((s, c, bgAccept) -> {
			c.connect();
			bgAccept.join();

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
}
