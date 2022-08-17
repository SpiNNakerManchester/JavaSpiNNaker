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

abstract class SupportUtils {
	private SupportUtils() {
	}

	private static final long MIN_TIMEOUT = 100;

	private static final long MAX_TIMEOUT = 400;

	static final void assertTimeout(long before, long after) {
		long delta = after - before;
		assertTrue(delta > MIN_TIMEOUT && delta < MAX_TIMEOUT,
				"measured timeout must be between 0.1s and 0.4s (measured "
						+ delta + "ms)");
	}

	/** Overall test timeout. */
	static final Duration OVERALL_TEST_TIMEOUT = Duration.ofSeconds(10);

	/** Requested timeout. */
	static final int TIMEOUT = 101;

	static class Daemon extends Thread {
		Daemon(Runnable r, String name) {
			super(r, name);
			setDaemon(true);
			start();
		}
	}

	static Joinable backgroundAccept(MockServer s) throws Exception {
		var t = new Daemon(() -> {
			try {
				s.connect();
			} catch (IOException e) {
				// Just totally ignore early closing of sockets
				if (!e.getMessage().equals("Socket closed")) {
					e.printStackTrace(System.err);
				}
			} catch (RuntimeException e) {
				e.printStackTrace(System.err);
			}
		}, "background accept");
		return () -> t.join();
	}

	interface Joinable {
		void join() throws InterruptedException;
	}

	interface IServer extends AutoCloseable {
		void send(JSONObject obj);

		default void send(String jsonString) {
			send(new JSONObject(jsonString));
		}

		JSONObject recv() throws JSONException, IOException;

		void advancedEmulationMode(BlockingDeque<String> send,
				BlockingDeque<JSONObject> received,
				BlockingDeque<JSONObject> keepalives, Joinable bgAccept);

		int getPort();
	}

	interface WithConn {
		void act(IServer s, SpallocClient c, Joinable bgAccept)
				throws Exception;
	}

	static void withConnection(WithConn op) throws Exception {
		try (var s = new MockServer();
				var c = new SpallocClient("localhost", s.getPort(), null)) {
			var bgAccept = backgroundAccept(s);
			assertTimeoutPreemptively(OVERALL_TEST_TIMEOUT, () -> {
				op.act(s, c, bgAccept);
			});
			bgAccept.join();
		}
	}
}
