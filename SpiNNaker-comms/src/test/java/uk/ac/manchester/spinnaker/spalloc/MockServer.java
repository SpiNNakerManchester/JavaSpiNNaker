/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.spalloc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.KILOBYTE;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.spalloc.SupportUtils.IConnection;
import uk.ac.manchester.spinnaker.utils.Daemon;
import uk.ac.manchester.spinnaker.utils.OneShotEvent;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

class MockServer implements SupportUtils.IServer {
	private static final Logger log = getLogger(MockServer.class);

	private static final int QUEUE_LENGTH = 1;

	private ServerSocket serverSocket;

	private int port;

	private final OneShotEvent started;

	private List<Connection> connections = new ArrayList<>();

	@MustBeClosed
	MockServer() throws IOException {
		started = new OneShotEvent();
		serverSocket = new ServerSocket(0, QUEUE_LENGTH);
		port = serverSocket.getLocalPort();
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public String toString() {
		if (serverSocket != null) {
			return "MockServer(open:" + port + ")";
		}
		return "MockServer(closed:" + port + ")";
	}

	class Connection implements SupportUtils.IConnection {
		private final Socket sock;

		private final PrintWriter out;

		private final BufferedReader in;

		Connection(Socket sock) throws IOException {
			this.sock = sock;
			out = new PrintWriter(
					new OutputStreamWriter(sock.getOutputStream(), UTF_8));
			in = new BufferedReader(
					new InputStreamReader(sock.getInputStream(), UTF_8),
					KILOBYTE);
		}

		@Override
		public void close() throws IOException {
			sock.close();
		}

		@Override
		public void send(JSONObject json) {
			json.write(out);
			out.println();
			out.flush();
		}

		@Override
		public JSONObject recv() throws JSONException, IOException {
			var line = in.readLine();
			return line == null ? null : new JSONObject(line);
		}
	}

	public Connection connect(boolean doClose) throws IOException {
		started.fire();
		var sock = serverSocket.accept();
		if (doClose) {
			serverSocket.close();
			serverSocket = null;
		}
		var c = new Connection(sock);
		synchronized (connections) {
			connections.add(c);
		}
		return c;
	}

	Connection connectQuietly(boolean doClose) {
		try {
			return connect(doClose);
		} catch (IOException e) {
			// Just totally ignore early closing of sockets
			if (!e.getMessage().equals("Socket closed")) {
				log.warn("problem with mock IO", e);
			}
		} catch (RuntimeException e) {
			log.warn("problem with mock IO", e);
		}
		return null;
	}

	Joinable backgroundAccept() throws Exception {
		return backgroundAccept(true);
	}

	Joinable backgroundAccept(boolean closeServerSocket) throws Exception {
		var result = new ValueHolder<IConnection>();
		var t = new Daemon(
				() -> result.setValue(connectQuietly(closeServerSocket)),
				"background accept");
		t.start();
		return () -> {
			t.join();
			return result.getValue();
		};
	}

	interface Joinable {
		IConnection join() throws InterruptedException;

		default void flushjoin() throws InterruptedException, IOException {
			try (var c = join()) {
				if (c != null) {
					log.debug("{} closing", c);
				}
			}
		}
	}

	@Override
	public void close() throws IOException {
		if (serverSocket != null && !serverSocket.isClosed()) {
			serverSocket.close();
		}
		serverSocket = null;
		synchronized (connections) {
			for (var c : connections) {
				c.close();
			}
		}
	}

	/** Message used to stop the server. */
	public static final String STOP = "STOP";

	static class Lock {
		private boolean set;

		synchronized void mark() {
			set = true;
			notifyAll();
		}

		synchronized void waitForMark() {
			while (!set) {
				try {
					wait();
				} catch (InterruptedException e) {
					throw new RuntimeException("interrupted", e);
				}
			}
		}
	}

	static final class InterlockedDaemon extends Thread {
		private final Callable<?> target;

		private final Lock lock;

		InterlockedDaemon(Callable<?> target, String name) {
			super(name);
			this.target = target;
			this.lock = new Lock();
			setDaemon(true);
			start();
			lock.waitForMark();
		}

		@Override
		public void run() {
			lock.mark();
			try {
				target.call();
			} catch (EOFException e) {
				// do nothing
			} catch (Exception e) {
				log.error("exception in {}", getName(), e);
			}
		}
	}

	@Override
	public void advancedEmulationMode(BlockingDeque<String> send,
			BlockingDeque<JSONObject> received,
			BlockingDeque<JSONObject> keepaliveQueue, Joinable bgAccept) {
		new InterlockedDaemon(() -> {
			var sc = bgAccept.join();
			var go = new ValueHolder<Boolean>(true);
			new InterlockedDaemon(() -> {
				var kal = connect(true);
				while (go.getValue()) {
					var o = kal.recv();
					if (o == null) {
						return o;
					}
					keepaliveQueue.offer(o);
					kal.send("{\"return\": null}");
				}
				return null;
			}, "mock server keepalive listener");
			while (true) {
				if (STOP.equals(send.peek())) {
					send.take();
					go.setValue(false);
					break;
				}
				var r = sc.recv();
				if (r == null) {
					break;
				}
				received.offer(r);
				do {
					sc.send(send.take());
				} while (send.peek().contains("changed"));
			}
			return 0;
		}, "mock server advanced emulator");
	}
}
