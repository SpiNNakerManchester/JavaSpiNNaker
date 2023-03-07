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

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
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

	interface IConnection extends Closeable {
		default void send(String jsonString) {
			send(new JSONObject(jsonString));
		}

		void send(JSONObject json);

		JSONObject recv() throws JSONException, IOException;
	}

	interface IServer extends AutoCloseable {
		void advancedEmulationMode(BlockingDeque<String> send,
				BlockingDeque<JSONObject> received,
				BlockingDeque<JSONObject> keepalives,
				MockServer.Joinable bgAccept) throws InterruptedException;

		int getPort();
	}

	interface WithConn {
		void act(IServer s, SpallocClient c, MockServer.Joinable bgAccept)
				throws Exception;
	}

	static void withConnection(WithConn op) throws Exception {
		try (var s = new MockServer();
				var c = new SpallocClient("localhost", s.getPort(), null)) {
			var bgAccept = s.backgroundAccept(true);
			assertTimeoutPreemptively(OVERALL_TEST_TIMEOUT, () -> {
				op.act(s, c, bgAccept);
			});
			bgAccept.flushjoin();
		}
	}

	interface WithConnConn {
		void act(SpallocClient client, IConnection serviceSideConnection)
				throws Exception;
	}

	static void withConnectedConnection(WithConnConn op) throws Exception {
		try (var s = new MockServer();
				var c = new SpallocClient("localhost", s.getPort(), null)) {
			var bgAccept = s.backgroundAccept(true);
			assertTimeoutPreemptively(OVERALL_TEST_TIMEOUT, () -> {
				c.connect();
				try (var sc = bgAccept.join()) {
					op.act(c, sc);
				}
			});
			bgAccept.flushjoin();
		}
	}

	interface WithAdvancedConn {
		void act(SpallocClient c) throws Exception;
	}

	static void withAdvancedConnection(BlockingDeque<String> send,
			BlockingDeque<JSONObject> received,
			BlockingDeque<JSONObject> keepalives, WithAdvancedConn op)
			throws Exception {
		try (var s = new MockServer();
				var c = new SpallocClient("localhost", s.getPort(), null)) {
			var bgAccept = s.backgroundAccept(false);
			s.advancedEmulationMode(send, received, keepalives, bgAccept);
			assertTimeoutPreemptively(OVERALL_TEST_TIMEOUT, () -> {
				op.act(c);
			});
			bgAccept.flushjoin();
		}
	}
}
