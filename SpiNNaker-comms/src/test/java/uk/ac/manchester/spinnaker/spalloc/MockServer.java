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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingDeque;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.spalloc.SupportUtils.Joinable;
import uk.ac.manchester.spinnaker.utils.OneShotEvent;

class MockServer implements SupportUtils.IServer {
	private static final Logger log = getLogger(MockServer.class);

	private static final int BUFFER_SIZE = 1024;

	private static final int QUEUE_LENGTH = 1;

	private ServerSocket serverSocket;

	private int port;

	private final OneShotEvent started;

	private Socket sock;

	private PrintWriter out;

	private BufferedReader in;

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

	public InetAddress connect() throws IOException {
		started.fire();
		sock = serverSocket.accept();
		serverSocket.close();
		serverSocket = null;
		out = new PrintWriter(
				new OutputStreamWriter(sock.getOutputStream(), UTF_8));
		in = new BufferedReader(
				new InputStreamReader(sock.getInputStream(), UTF_8),
				BUFFER_SIZE);
		return sock.getInetAddress();
	}

	void connectQuietly() {
		try {
			connect();
		} catch (IOException e) {
			// Just totally ignore early closing of sockets
			if (!e.getMessage().equals("Socket closed")) {
				log.warn("problem with mock IO", e);
			}
		} catch (RuntimeException e) {
			log.warn("problem with mock IO", e);
		}
	}

	@Override
	public void close() throws IOException {
		if (nonNull(serverSocket) && !serverSocket.isClosed()) {
			serverSocket.close();
		}
		serverSocket = null;
		if (nonNull(sock)) {
			sock.close();
		}
		sock = null;
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
		return isNull(line) ? null : new JSONObject(line);
	}

	/** Message used to stop the server. */
	public static final String STOP = "STOP";

	@Override
	public void advancedEmulationMode(BlockingDeque<String> send,
			BlockingDeque<JSONObject> received,
			BlockingDeque<JSONObject> keepaliveQueue, Joinable bgAccept) {
		new SupportUtils.Daemon(() -> {
			try {
				bgAccept.join();
				launchKeepaliveListener(keepaliveQueue);
				while (true) {
					if (STOP.equals(send.peek())) {
						send.take();
						break;
					}
					var r = recv();
					if (isNull(r)) {
						break;
					}
					received.offer(r);
					do {
						send(send.take());
					} while (send.peek().contains("changed"));
				}
			} catch (Exception e) {
				log.error("failure in mock server", e);
			}
		}, "mock server advanced emulator");
	}

	private static void launchKeepaliveListener(
			BlockingDeque<JSONObject> keepaliveQueue) {
		new SupportUtils.Daemon(() -> {
			try (var s = new MockServer()) {
				s.connect();
				while (true) {
					var o = s.recv();
					if (isNull(o)) {
						break;
					}
					keepaliveQueue.offer(o);
					s.send("{\"return\": null}");
				}
			} catch (EOFException e) {
				// do nothing
			} catch (Exception e) {
				log.error("failure in keepalive listener", e);
			}
		}, "mock server keepalive listener");
	}
}
