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

import static java.lang.Thread.sleep;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingDeque;

import org.json.JSONException;
import org.json.JSONObject;

import uk.ac.manchester.spinnaker.utils.OneShotEvent;

class MockServer implements SupportUtils.IServer {
	static final Charset UTF8 = Charset.forName("UTF-8");
	static final int PORT = 22244;
	static final int BUFFER_SIZE = 1024;
	static final int QUEUE_LENGTH = 1;
	static final int INTER_BIND_DELAY = 50;

	final ServerSocket serverSocket;
	final OneShotEvent started;
	Socket sock;
	PrintWriter out;
	BufferedReader in;

	public MockServer() throws IOException {
		serverSocket = new ServerSocket();
		started = new OneShotEvent();
	}

	public void listen() throws IOException, InterruptedException {
		while (true) {
			try {
				serverSocket.bind(new InetSocketAddress(PORT), QUEUE_LENGTH);
				return;
			} catch (BindException ignored) {
				// try again after a delay
				sleep(INTER_BIND_DELAY);
			}
		}
	}

	public InetAddress connect() throws IOException {
		started.fire();
		sock = serverSocket.accept();
		serverSocket.close();
		out = new PrintWriter(
				new OutputStreamWriter(sock.getOutputStream(), UTF8));
		in = new BufferedReader(
				new InputStreamReader(sock.getInputStream(), UTF8),
				BUFFER_SIZE);
		return sock.getInetAddress();
	}

	@Override
	public void close() throws IOException {
		if (sock != null) {
			sock.close();
		}
		sock = null;
		if (!serverSocket.isClosed()) {
			serverSocket.close();
		}
	}

	@Override
	public void send(JSONObject json) {
		json.write(out);
		out.flush();
	}

	@Override
	public JSONObject recv() throws JSONException, IOException {
		String line = in.readLine();
		return line == null ? null : new JSONObject(line);
	}

	public static final String STOP = "STOP";

	@Override
	public void advancedEmulationMode(BlockingDeque<String> send,
			BlockingDeque<JSONObject> received,
			BlockingDeque<JSONObject> keepaliveQueue, Thread bgAccept) {
		new Thread(() -> {
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
		}).start();
	}

	private static void launchKeepaliveListener(
			BlockingDeque<JSONObject> keepaliveQueue) {
		Thread t = new Thread(() -> {
			try {
				MockServer s = new MockServer();
				s.listen();
				s.connect();
				while (true) {
					JSONObject o = s.recv();
					if (o == null) {
						break;
					}
					keepaliveQueue.offer(o);
					s.send("{\"return\": null}");
				}
				s.close();
			} catch (EOFException e) {
				// do nothing
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		t.setDaemon(true);
		t.start();
	}
}
