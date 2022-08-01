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

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingDeque;

import org.json.JSONException;
import org.json.JSONObject;

import uk.ac.manchester.spinnaker.spalloc.SupportUtils.Joinable;
import uk.ac.manchester.spinnaker.utils.OneShotEvent;

class MockServer implements SupportUtils.IServer {
	private static final Charset UTF8 = Charset.forName("UTF-8");

	private static final int BUFFER_SIZE = 1024;

	private static final int QUEUE_LENGTH = 1;

	private ServerSocket serverSocket;

	private int port;

	private final OneShotEvent started;

	private Socket sock;

	private PrintWriter out;

	private BufferedReader in;

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
				new OutputStreamWriter(sock.getOutputStream(), UTF8));
		in = new BufferedReader(
				new InputStreamReader(sock.getInputStream(), UTF8),
				BUFFER_SIZE);
		return sock.getInetAddress();
	}

	@Override
	public void close() throws IOException {
		if (serverSocket != null && !serverSocket.isClosed()) {
			serverSocket.close();
		}
		serverSocket = null;
		if (sock != null) {
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
		String line = in.readLine();
		return line == null ? null : new JSONObject(line);
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
					JSONObject r = recv();
					if (r == null) {
						break;
					}
					received.offer(r);
					do {
						send(send.take());
					} while (send.peek().contains("changed"));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, "mock server advanced emulator");
	}

	private static void launchKeepaliveListener(
			BlockingDeque<JSONObject> keepaliveQueue) {
		new SupportUtils.Daemon(() -> {
			try (MockServer s = new MockServer()) {
				s.connect();
				while (true) {
					JSONObject o = s.recv();
					if (o == null) {
						break;
					}
					keepaliveQueue.offer(o);
					s.send("{\"return\": null}");
				}
			} catch (EOFException e) {
				// do nothing
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, "mock server keepalive listener");
	}
}
