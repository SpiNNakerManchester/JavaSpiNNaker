package uk.ac.manchester.spinnaker.spalloc;

import static org.junit.jupiter.api.Assertions.*;

import java.io.EOFException;
import java.net.ConnectException;
import java.util.Arrays;

import javax.xml.ws.Holder;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.spalloc.messages.ExceptionResponse;
import uk.ac.manchester.spinnaker.spalloc.messages.JobsChangedNotification;
import uk.ac.manchester.spinnaker.spalloc.messages.MachinesChangedNotification;
import uk.ac.manchester.spinnaker.spalloc.messages.ReturnResponse;
import uk.ac.manchester.spinnaker.utils.OneShotEvent;

class TestClient {
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
		void act(MockServer s, SpallocClient c, Thread t) throws Exception;
	}

	void withConnection(WithConn op) throws Exception {
		MockServer s = new MockServer();
		try {
			SpallocClient c = new SpallocClient("localhost", 22244, null);
			Thread t = backgroundAccept(s);
			op.act(s, c, t);
			t.join();
			c.close();
		} finally {
			s.close();
		}
	}

	@Test
	void testConnectNoServer() throws Exception {
		SpallocClient c = new SpallocClient("localhost", 22244, null);
		assertThrows(ConnectException.class, () -> c.connect());
		c.close();
	}

	@Test
	void testConnect() throws Exception {
		withConnection((s, c, t) -> c.connect());
	}

	@Test
	void testConnectContext() throws Exception {
		MockServer s = new MockServer();
		try (SpallocClient c = new SpallocClient("localhost", 22244, null)) {
			Thread t = backgroundAccept(s);
			try (AutoCloseable context = c.withConnection()) {
				// do nothing
			}
			t.join();
		}
		s.close();
	}

	@Test
	void testClose() throws Exception {
		withConnection((s,c,t)->{
			c.connect();
			c.close();
			c.close();
			c = new SpallocClient("localhost", 22244, null);
			c.close();
		});
	}

	@Test void testRecvJson() throws Exception {
		withConnection((s,c,t)->{
			assertThrows(EOFException.class, () -> c.receiveResponse(null));
			c.connect();
			t.join();

			// Test the timeout
			long before = System.currentTimeMillis();
			assertThrows(SpallocProtocolTimeoutException.class,
					() -> c.receiveResponse(100));
			long after = System.currentTimeMillis();
			assertTrue(after - before > 100, "must take at least 100ms");
			assertTrue(after - before < 200, "must take no more than 200ms");

			// Transfer an actual message
			s.send(new JSONObject("{\"return\": \"bar\"}"));
			assertEquals("\"bar\"", ((ReturnResponse) c.receiveResponse(null))
					.getReturnValue());

			// Return a large message
			JSONArray a = new JSONArray();
			for (int i = 0; i < 1000; i++) {
				a.put(i);
			}
			JSONObject o = new JSONObject();
			o.put("return", a);
			s.send(o);
			JSONArray a2 =
					new JSONArray(((ReturnResponse) c.receiveResponse(null))
							.getReturnValue());
			for (int i = 0; i < 1000; i++) {
				assertEquals(a.get(i), a2.get(i));
			}

			// Test message ordering
			s.send(new JSONObject("{\"return\": \"foo\"}"));
			s.send(new JSONObject("{\"return\": \"bar\"}"));
			assertEquals("\"foo\"", ((ReturnResponse) c.receiveResponse(null))
					.getReturnValue());
			assertEquals("\"bar\"", ((ReturnResponse) c.receiveResponse(null))
					.getReturnValue());

			// Test other message types
			s.send(new JSONObject("{\"exception\": \"bar\"}"));
			assertEquals("bar", ((ExceptionResponse) c.receiveResponse(null))
					.getException());
			s.send(new JSONObject("{\"machines_changed\": [\"foo\",\"bar\"]}"));
			assertEquals(Arrays.asList("foo", "bar"),
					((MachinesChangedNotification) c.receiveResponse(null))
							.getMachinesChanged());
			s.send(new JSONObject("{\"jobs_changed\": [1, 2]}"));
			assertEquals(Arrays.asList(1, 2),
					((JobsChangedNotification) c.receiveResponse(null))
							.getJobsChanged());

			// When socket becomes closed should fail
			s.close();
			assertThrows(EOFException.class, () -> c.receiveResponse(null));
		});
	}
}
