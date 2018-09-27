package uk.ac.manchester.spinnaker.spalloc;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import javax.xml.ws.Holder;

import uk.ac.manchester.spinnaker.utils.OneShotEvent;

abstract class SupportUtils {
	private SupportUtils() {
	}

	static final void assertTimeout(long before, long after) {
		assertTrue(after - before > 100 && after - before < 400,
				"measured timeout must be between 0.1s and 0.4s (measured"
						+ (after - before) + "ms)");
	}

	static final Duration OVERALL_TEST_TIMEOUT = Duration.ofSeconds(10);
	static final int TIMEOUT = 101;

	static Thread backgroundAccept(MockServer s) throws Exception {
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

	static void withConnection(WithConn op) throws Exception {
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

}
