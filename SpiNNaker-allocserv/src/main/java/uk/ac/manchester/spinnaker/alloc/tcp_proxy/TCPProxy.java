package uk.ac.manchester.spinnaker.alloc.tcp_proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A TCP Proxy server that can re-connect if the target goes down.
 */
public class TCPProxy {

	/** The maximum size of a read from a TCP socket. */
	private static final int BUFFER_SIZE = 4096;

	private Socket client;

	final private Remote remote;

	TCPProxy(Socket client, String remoteHost, int remotePort) {
		this.client = client;
		this.remote = new Remote(remoteHost, remotePort);

		Thread clientToRemote = new Thread(this::clientToRemote);
		Thread remoteToClient = new Thread(this::remoteToClient);
		clientToRemote.start();
		remoteToClient.start();
	}

	/**
	 * Read the client and send to the remote.  If remote write fails,
	 * reconnect to remote and resend.  If client read fails, stop.
	 */
	private void clientToRemote() {
		try {
			remote.connect();
			byte[] buffer = new byte[BUFFER_SIZE];
			try (var input = client.getInputStream()) {

				while (client.isConnected()) {
					int bytesRead = input.read(buffer);
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
	 * Read from the remote and send to the client.  If the remote read fails,
	 * retry until working.  If the client write fails, stop.
	 */
	private void remoteToClient() {
		try {
			remote.connect();
			byte[] buffer = new byte[BUFFER_SIZE];
			try (var output = client.getOutputStream()) {
				while (client.isConnected()) {
					int bytesRead = remote.read(buffer);
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
		try {
			client.close();
		} catch (IOException e) {
			// Ignore
		}
		client = null;
	}

	public static void main(String[] args) throws IOException {
		int localPort = Integer.parseInt(args[0]);
		String remoteHost = args[1];
		int remotePort = Integer.parseInt(args[2]);

		try (var server = new ServerSocket(localPort)) {
			while (true) {
				var client = server.accept();
				new TCPProxy(client, remoteHost, remotePort);
			}
		}
	}
}

/**
 * A persistent remote connection.
 */
final class Remote {

	/** How long to wait between retries of the connection. */
	private static final int RETRY_MS = 5000;

	final private String remoteHost;

	final private int remotePort;

	private Socket remote = null;

	private InputStream remoteInput = null;

	private OutputStream remoteOutput = null;

	/**
	 * Create a remote.
	 *
	 * @param remoteHost The host to connect to.
	 * @param remotePort The port to connect to.
	 */
	Remote(String remoteHost, int remotePort) {
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
	}

	/**
	 * Connect to the remote site and don't stop until connected.
	 *
	 * @throws InterruptedException If interrupted.
	 */
	synchronized void connect() throws InterruptedException {
		// Make sure these are closed
		close();
		while (true) {
			try {
				remote = new Socket(remoteHost, remotePort);
				remoteInput = remote.getInputStream();
				remoteOutput = remote.getOutputStream();
				return;
			} catch (IOException e) {
				Thread.sleep(RETRY_MS);
			}
		}
	}

	/**
	 * Write to the remote.  Retry until successful, or interrupted.
	 *
	 * @param buffer The buffer to write.
	 * @param offset The offset of the buffer to start from.
	 * @param length The length of the buffer to write, starting at the offset.
	 * @throws InterruptedException If interrupted.
	 */
	synchronized void write(byte[] buffer, int offset, int length)
			throws InterruptedException {
		while (true) {
			try {
				remoteOutput.write(buffer, 0, length);
				return;
			} catch (IOException e) {
				connect();
			}
		}
	}

	/**
	 * Read from the remote.  Retry until successful, or interrupted.
	 *
	 * @param buffer The buffer to read into.
	 * @return The bytes written.
	 * @throws InterruptedException If interrupted.
	 */
	synchronized int read(byte[] buffer) throws InterruptedException {
		while (true) {
			try {
				return remoteInput.read(buffer);
			} catch (IOException e) {
				connect();
			}
		}
	}

	/**
	 * Close everything.
	 */
	synchronized void close() {
		forceClose(remote);
		forceClose(remoteInput);
		forceClose(remoteOutput);
		remote = null;
		remoteInput = null;
		remoteOutput = null;
	}

	/**
	 * Force closure and ignore any exceptions.
	 * @param closable The thing to close.
	 */
	private void forceClose(AutoCloseable closable) {
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
