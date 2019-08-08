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

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.BlockingDeque;

import org.json.JSONException;
import org.json.JSONObject;

import uk.ac.manchester.spinnaker.utils.OneShotEvent;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

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
		ValueHolder<Exception> problem = new ValueHolder<>();
		Thread t = new Thread(() -> {
			try {
				s.listen();
				started.fire();
				s.connect();
			} catch (Exception e) {
				problem.setValue(e);
				main.interrupt();
			}
		});
		t.start();
		try {
			started.await();
		} catch (InterruptedException e) {
			if (problem.getValue() != null) {
				throw problem.getValue();
			}
			throw e;
		}
		return t;
	}

	interface IServer extends AutoCloseable {
		void send(JSONObject obj);
		default void send(String jsonString) {
			send(new JSONObject(jsonString));
		}
		JSONObject recv() throws JSONException, IOException;
		void advancedEmulationMode(BlockingDeque<String> send,
				BlockingDeque<JSONObject> received,
				BlockingDeque<JSONObject> keepalives, Thread bgAccept);
	}

	interface WithConn {
		void act(IServer s, SpallocClient c, Thread bgAccept)
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
