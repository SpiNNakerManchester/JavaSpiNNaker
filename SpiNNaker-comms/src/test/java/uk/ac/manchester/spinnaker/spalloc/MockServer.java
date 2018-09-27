package uk.ac.manchester.spinnaker.spalloc;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingDeque;

import org.json.JSONException;
import org.json.JSONObject;

import uk.ac.manchester.spinnaker.utils.OneShotEvent;

class MockServer implements AutoCloseable {
	static final Charset UTF8 = Charset.forName("UTF-8");
	static final int PORT = 22244;
	static final int BUFFER_SIZE = 1024;
	static final int QUEUE_LENGTH = 1;

	final ServerSocket serverSocket;
	final OneShotEvent started;
	Socket sock;
	PrintWriter out;
	BufferedReader in;

	public MockServer() throws IOException {
		serverSocket = new ServerSocket();
		started = new OneShotEvent();
	}

	public void listen() throws IOException {
		serverSocket.bind(new InetSocketAddress(PORT), QUEUE_LENGTH);
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

	public void send(JSONObject obj) {
		out.println(obj.toString());
		out.flush();
	}

	public void send(String obj) {
		send(new JSONObject(obj));
	}

	public JSONObject recv() throws JSONException, IOException {
		String line = in.readLine();
		return line == null ? null : new JSONObject(line);
	}

	public static final String STOP = "STOP";

	public void advancedEmulationMode(LinkedBlockingDeque<String> send,
			LinkedBlockingDeque<JSONObject> received,
			LinkedBlockingDeque<JSONObject> keepaliveQueue, Thread bgAccept) {
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
			LinkedBlockingDeque<JSONObject> keepaliveQueue) {
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
