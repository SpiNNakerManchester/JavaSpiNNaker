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

import static uk.ac.manchester.spinnaker.proxy.TCPProxy.log;

import java.io.IOException;
import java.net.Socket;

/**
 * A persistent remote connection.
 *
 * @author Andrew Rowley
 */
final class Remote implements AutoCloseable {
	/** How long to wait between retries of the connection. */
	private static final int RETRY_MS = 5000;

	/** How long to wait between writes on a connection. */
	private static final int WRITE_RETRY_MS = 1000;

	private final String remoteHost;

	private final int remotePort;

	private Socket remote = null;

	private boolean closed = false;

	/**
	 * Create a remote.
	 *
	 * @param remoteHost
	 *            The host to connect to.
	 * @param remotePort
	 *            The port to connect to.
	 */
	Remote(String remoteHost, int remotePort) {
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
	}

	/**
	 * Connect to the remote site and don't stop until connected.
	 *
	 * @throws InterruptedException
	 *             If interrupted.
	 */
	synchronized void connect() throws InterruptedException {
		if (remote != null || closed) {
			return;
		}
		log.info("Connecting to {}:{}", remoteHost, remotePort);
		while (true) {
			try {
				remote = new Socket(remoteHost, remotePort);
				log.info("Connected to {}", remote.getRemoteSocketAddress());
				return;
			} catch (IOException e) {
				Thread.sleep(RETRY_MS);
			}
		}
	}

	/**
	 * Write to the remote. Retry until successful, or interrupted.
	 *
	 * @param buffer
	 *            The buffer to write.
	 * @param offset
	 *            The offset of the buffer to start from.
	 * @param length
	 *            The length of the buffer to write, starting at the offset.
	 * @throws InterruptedException
	 *             If interrupted.
	 */
	void write(byte[] buffer, int offset, int length)
			throws InterruptedException {
		while (true) {
			while (remote == null) {
				Thread.sleep(WRITE_RETRY_MS);
			}
			try {
				var remoteOutput = remote.getOutputStream();
				remoteOutput.write(buffer, 0, length);
				return;
			} catch (IOException e) {
				if (!closed) {
					closeConnections();
					connect();
				}
			}
		}
	}

	/**
	 * Read from the remote. Retry until successful, or interrupted.
	 *
	 * @param buffer
	 *            The buffer to read into.
	 * @return The bytes written.
	 * @throws InterruptedException
	 *             If interrupted.
	 */
	int read(byte[] buffer) throws InterruptedException {
		while (true) {
			if (remote == null) {
				return -1;
			}
			try {
				var remoteInput = remote.getInputStream();
				int bytesRead = remoteInput.read(buffer);
				if (bytesRead == -1) {
					throw new IOException();
				}
				return bytesRead;
			} catch (IOException e) {
				if (!closed) {
					closeConnections();
					connect();
				}
			}
		}
	}

	/**
	 * Close the connection never to be reopened.
	 */
	@Override
	public void close() {
		closed = true;
		closeConnections();
	}

	/**
	 * Close the connections.
	 */
	synchronized void closeConnections() {
		if (remote == null) {
			return;
		}
		forceClose(remote);
		remote = null;
	}

	/**
	 * Force closure and ignore any exceptions.
	 *
	 * @param closable
	 *            The thing to close.
	 */
	private static void forceClose(Socket closable) {
		if (closable == null) {
			return;
		}
		try {
			closable.close();
		} catch (Exception e) {
			// Ignore the exception
		}
	}
}
