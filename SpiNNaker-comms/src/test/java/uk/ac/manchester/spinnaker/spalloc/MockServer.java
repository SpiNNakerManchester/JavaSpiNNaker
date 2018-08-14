package uk.ac.manchester.spinnaker.spalloc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

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
		serverSocket.close();
	}

	public void send(JSONObject obj) {
		out.println(obj.toString());
		out.flush();
	}

	public JSONObject recv() throws JSONException, IOException {
		return new JSONObject(in.readLine());
	}
}
