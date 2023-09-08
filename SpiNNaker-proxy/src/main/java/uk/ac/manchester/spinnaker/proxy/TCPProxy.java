/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A TCP Proxy server that can re-connect if the target goes down.
 * <p>
 * Typically called via:
 *
 * <pre>java -jar spinnaker-proxy.jar LOCAL_PORT REMOTE_HOST REMOTE_PORT</pre>
 *
 * Writes messages to standard error describing connects and disconnects.
 *
 * @author Andrew Rowley
 */
public class TCPProxy {
	/** The maximum size of a read from a TCP socket. */
	private static final int BUFFER_SIZE = 4096;

	private Socket client;

	private final Remote remote;

	TCPProxy(Socket client, String remoteHost, int remotePort) {
		System.err.format("New connection from %s%n",
				client.getRemoteSocketAddress());
		this.client = client;
		this.remote = new Remote(remoteHost, remotePort);

		var clientToRemote = new Thread(this::clientToRemote);
		var remoteToClient = new Thread(this::remoteToClient);
		clientToRemote.start();
		remoteToClient.start();
	}

	/**
	 * Read the client and send to the remote. If remote write fails, reconnect
	 * to remote and resend. If client read fails, stop.
	 */
	private void clientToRemote() {
		try {
			remote.connect();
			var buffer = new byte[BUFFER_SIZE];
			try (var input = client.getInputStream()) {
				while (client.isConnected()) {
					int bytesRead = input.read(buffer);
					if (bytesRead == -1) {
						throw new IOException();
					}
					remote.write(buffer, 0, bytesRead);
				}
			} catch (IOException e) {
				// This is likely caused by the client disconnecting.
			}
		} catch (InterruptedException e) {
			// Exiting
		}
		forceCloseClient();
		remote.close();
	}

	/**
	 * Read from the remote and send to the client. If the remote read fails,
	 * retry until working. If the client write fails, stop.
	 */
	private void remoteToClient() {
		try {
			remote.connect();
			var buffer = new byte[BUFFER_SIZE];
			try (var output = client.getOutputStream()) {
				while (client.isConnected()) {
					int bytesRead = remote.read(buffer);
					if (bytesRead == -1) {
						throw new IOException();
					}
					output.write(buffer, 0, bytesRead);
				}
			} catch (IOException e) {
				// This is likely caused by the client disconnecting.
			}
		} catch (InterruptedException e) {
			// Exiting
		}
		forceCloseClient();
		remote.close();
	}

	/**
	 * Close the client and ignore any errors.
	 */
	private synchronized void forceCloseClient() {
		if (client == null) {
			return;
		}
		System.err.format("Client %s left%n", client.getRemoteSocketAddress());
		try {
			client.close();
		} catch (IOException e) {
			// Ignore
		}
		client = null;
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *            {@code args[0]}: The local port to listen on <br>
	 *            {@code args[1]}: The remote host to proxy <br>
	 *            {@code args[2]}: The remote port to proxy
	 * @throws IOException
	 *             If we can't start the server.
	 */
	public static void main(String[] args) throws IOException {
		int localPort = Integer.parseInt(args[0]);
		var remoteHost = args[1];
		int remotePort = Integer.parseInt(args[2]);

		try (var server = new ServerSocket(localPort)) {
			while (true) {
				var client = server.accept();
				new TCPProxy(client, remoteHost, remotePort);
			}
		}
	}
}
