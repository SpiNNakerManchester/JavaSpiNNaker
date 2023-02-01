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

import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.spalloc.Utils.makeTimeout;
import static uk.ac.manchester.spinnaker.spalloc.Utils.timeLeft;
import static uk.ac.manchester.spinnaker.spalloc.Utils.timedOut;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocProtocolException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocProtocolTimeoutException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.messages.Command;
import uk.ac.manchester.spinnaker.spalloc.messages.ExceptionResponse;
import uk.ac.manchester.spinnaker.spalloc.messages.Notification;
import uk.ac.manchester.spinnaker.spalloc.messages.Response;
import uk.ac.manchester.spinnaker.spalloc.messages.ReturnResponse;

/**
 * The basic communications layer of the spalloc client. This client assumes
 * that the protocol is line-oriented, but does not assume anything about the
 * formatting of the contents of the lines.
 *
 * @author Donal Fellows
 */
public abstract class SpallocConnection implements Closeable {
	private static final Logger log = getLogger(SpallocConnection.class);

	/**
	 * The hostname and port of the spalloc server.
	 */
	private final InetSocketAddress addr;

	private final Integer defaultTimeout;

	/**
	 * Whether this connection is in the dead state. Dead connections have no
	 * underlying connections open.
	 */
	private boolean dead;

	/**
	 * Mapping from threads to sockets. Kept because we need to have way to shut
	 * down all sockets at once.
	 */
	@GuardedBy("socksLock")
	private final Map<Thread, TextSocket> socks = new HashMap<>();

	/** Lock for access to {@link #socks}. */
	private final Object socksLock = new Object();

	/**
	 * The thread-aware socket factory. Every thread gets exactly one socket.
	 */
	@SuppressWarnings("ThreadLocalUsage")
	private final ThreadLocal<TextSocket> threadLocalSocket =
			ThreadLocal.withInitial(TextSocket::new);

	/** A queue of unprocessed notifications. */
	protected final Queue<Notification> notifications =
			new ConcurrentLinkedQueue<>();

	/**
	 * Define a new connection. <b>NB:</b> Does not connect to the server until
	 * {@link #connect()} is called.
	 *
	 * @param hostname
	 *            The hostname of the server.
	 * @param port
	 *            The port to use.
	 * @param timeout
	 *            The default timeout.
	 */
	@MustBeClosed
	protected SpallocConnection(String hostname, int port, Integer timeout) {
		addr = new InetSocketAddress(
				requireNonNull(hostname, "hostname must not be null"), port);
		this.dead = true;
		this.defaultTimeout = timeout;
	}

	/**
	 * Context adapter. Allows this code to be used like this:
	 *
	 * <pre>
	 * try (var c = client.withConnection()) {
	 *     ...
	 * }
	 * </pre>
	 *
	 * @return the auto-closeable context.
	 * @throws IOException
	 *             If the connect to the spalloc server fails.
	 */
	@MustBeClosed
	public AutoCloseable withConnection() throws IOException {
		connect();
		return this::close;
	}

	private TextSocket getConnection(Integer timeout) throws IOException {
		TextSocket sock = null;
		var key = currentThread();
		/*
		 * This loop will keep trying to connect until the socket exists and is
		 * in a connected state.
		 */
		do {
			if (dead) {
				throw new EOFException("not connected");
			}
			sock = getConnectedSocket(key, timeout);
		} while (sock == null);

		if (timeout != null) {
			sock.setSoTimeout(timeout);
		}
		return sock;
	}

	/**
	 * Try to get a connected socket.
	 *
	 * @param key
	 *            The thread that wants the connection (i.e., the current
	 *            thread; cached for efficiency).
	 * @param timeout
	 *            The socket's timeout.
	 * @return The socket, or {@code null} if the connection failed.
	 * @throws IOException
	 *             if something really bad goes wrong.
	 */
	private TextSocket getConnectedSocket(Thread key, Integer timeout)
			throws IOException {
		TextSocket sock;
		boolean connectNeeded = false;
		synchronized (socksLock) {
			sock = threadLocalSocket.get();
			if (!socks.containsKey(key)) {
				socks.put(key, sock);
				connectNeeded = true;
			}
		}

		if (connectNeeded) {
			sock.setSoTimeout(timeout != null ? timeout : 0);
			if (!doConnect(sock)) {
				closeThreadConnection(key);
				return null;
			}
		}
		return sock;
	}

	private boolean doConnect(Socket sock) throws IOException {
		boolean success = false;
		try {
			sock.connect(addr);
			success = true;
		} catch (IOException e) {
			if (!e.getMessage().contains("EISCONN")) {
				throw e;
			}
		}
		return success;
	}

	/**
	 * (Re)connect to the server.
	 *
	 * @throws IOException
	 *             If a connection failure occurs.
	 */
	public void connect() throws IOException {
		connect(defaultTimeout);
	}

	/**
	 * (Re)connect to the server.
	 *
	 * @param timeout
	 *            How long to spend (re)connecting.
	 * @throws IOException
	 *             If a connection failure occurs.
	 */
	public void connect(Integer timeout) throws IOException {
		// Close any existing connection
		var s = threadLocalSocket.get();
		if (s.isClosed()) {
			closeThreadConnection(currentThread());
		} else if (!s.isConnected()) {
			closeThreadConnection(currentThread());
		}
		dead = false;
		try {
			getConnection(timeout);
		} catch (IOException e) {
			// Failure, try again...
			closeThreadConnection(currentThread());
			// Pass on the exception
			throw e;
		}
	}

	@SuppressWarnings("resource")
	private void closeThreadConnection(Thread key) throws IOException {
		Socket sock;
		synchronized (socksLock) {
			sock = socks.remove(key);
		}
		if (sock != null) {
			// Mark the thread local so it will reinitialise
			if (key == currentThread()) {
				threadLocalSocket.remove();
			}
			// Close the socket itself
			sock.close();
		}
	}

	/**
	 * Disconnect from the server.
	 *
	 * @throws IOException
	 *             if anything goes wrong
	 */
	@Override
	public void close() throws IOException {
		dead = true;
		List<Thread> keys;
		synchronized (socksLock) {
			// Copy so we can safely remove asynchronously
			keys = List.copyOf(socks.keySet());
		}
		for (var key : keys) {
			closeThreadConnection(key);
		}
		threadLocalSocket.remove();
	}

	private static String readLine(TextSocket sock)
			throws SpallocProtocolTimeoutException, IOException,
			InterruptedException {
		try {
			var line = sock.getReader().readLine();
			if (line == null) {
				throw new EOFException("Connection closed");
			}
			return line;
		} catch (SocketTimeoutException e) {
			if (Thread.interrupted()) {
				throw new InterruptedException("interrupted in readLine");
			}
			throw new SpallocProtocolTimeoutException("recv timed out", e);
		}
	}

	/**
	 * Receive a line from the server with a response.
	 *
	 * @param timeout
	 *            The number of milliseconds to wait before timing out or
	 *            {@code null} if this function should try again forever.
	 * @return The unpacked response from the line received.
	 * @throws SpallocProtocolTimeoutException
	 *             If a timeout occurs.
	 * @throws SpallocProtocolException
	 *             If the socket gets an empty response.
	 * @throws IOException
	 *             If the socket is unusable or becomes disconnected.
	 * @throws InterruptedException
	 *             If interrupted, eventually
	 */
	protected Response receiveResponse(Integer timeout)
			throws SpallocProtocolTimeoutException, IOException,
			InterruptedException {
		if (timeout == null || timeout < 0) {
			timeout = 0;
		}
		var sock = getConnection(timeout);

		// Wait for some data to arrive
		var line = readLine(sock); // Not null; null case throws
		return parseResponse(line);
	}

	/**
	 * Attempt to send a command as a line to the server.
	 *
	 * @param command
	 *            The command to serialise.
	 * @param timeout
	 *            The number of milliseconds to wait before timing out or
	 *            {@code null} if this function should try again forever.
	 * @throws SpallocProtocolTimeoutException
	 *             If a timeout occurs.
	 * @throws IOException
	 *             If the socket is unusable or becomes disconnected.
	 */
	protected void sendCommand(Command<?> command, Integer timeout)
			throws SpallocProtocolTimeoutException, IOException {
		if (timeout == null || timeout < 0) {
			timeout = 0;
		}
		if (log.isDebugEnabled()) {
			log.debug("sending a {}", command.getClass());
		}
		var sock = getConnection(timeout);

		// Send the line
		var msg = formatRequest(command);
		try {
			var pw = sock.getWriter();
			pw.println(msg);
			if (pw.checkError()) {
				/*
				 * Socket started giving errors; presume EOF since the
				 * PrintWriter won't actually tell us why...
				 */
				throw new EOFException("command write failed");
			}
		} catch (SocketTimeoutException e) {
			throw new SpallocProtocolTimeoutException("send timed out", e);
		}
	}

	/**
	 * Format a request to be ready to go to the server.
	 *
	 * @param command
	 *            The request to format for sending. Not {@code null}.
	 * @return The text to send to the server. Will have a newline added.
	 * @throws IOException
	 *             If formatting goes wrong.
	 */
	protected abstract String formatRequest(Command<?> command)
			throws IOException;

	/**
	 * Parse a response line from the server.
	 *
	 * @param line
	 *            The line to parse. Not {@code null}. Has the terminating
	 *            newline removed.
	 * @return The parsed response.
	 * @throws IOException
	 *             If parsing completely fails.
	 * @throws SpallocProtocolException
	 *             If an unexpected valid JSON message is returned (e.g.,
	 *             {@code null}).
	 */
	protected abstract Response parseResponse(String line) throws IOException;

	/**
	 * Send a command to the server and return the reply.
	 *
	 * @param command
	 *            The command to send.
	 * @param timeout
	 *            The number of milliseconds to wait before timing out or
	 *            {@code null} if this function should wait forever.
	 * @return The result string returned by the server.
	 * @throws SpallocServerException
	 *             If the server sends an error.
	 * @throws SpallocProtocolTimeoutException
	 *             If a timeout occurs.
	 * @throws SpallocProtocolException
	 *             If the connection is unavailable or is closed.
	 * @throws InterruptedException
	 *             If interrupted, eventually
	 */
	protected String call(Command<?> command, Integer timeout)
			throws SpallocServerException, SpallocProtocolTimeoutException,
			SpallocProtocolException, InterruptedException {
		try {
			var finishTime = makeTimeout(timeout);

			// Construct and send the command message
			sendCommand(command, timeout);

			// Command sent! Attempt to receive the response...
			while (!timedOut(finishTime)) {
				var r = receiveResponse(timeLeft(finishTime));
				if (r == null) {
					continue;
				}
				if (r instanceof ReturnResponse success) {
					return success.getReturnValue();
				} else if (r instanceof ExceptionResponse serverError) {
					throw new SpallocServerException(serverError);
				} else if (r instanceof Notification notification) {
					// Got a notification, keep trying...
					notifications.add(notification);
				} else {
					throw new SpallocProtocolException(
							"bad response: " + r.getClass());
				}
			}
			throw new SpallocProtocolTimeoutException(
					"timed out while calling " + command.getCommand());
		} catch (SpallocProtocolTimeoutException e) {
			throw e;
		} catch (IOException e) {
			throw new SpallocProtocolException(e);
		}
	}

	/**
	 * Subclass of Socket to encapsulate reading and writing text by lines. This
	 * handles all buffering internally, locking that close to the socket
	 * itself.
	 */
	private static class TextSocket extends Socket {
		private BufferedReader br;

		private PrintWriter pw;

		PrintWriter getWriter() throws IOException {
			if (pw == null) {
				pw = new PrintWriter(
						new OutputStreamWriter(getOutputStream(), UTF_8));
			}
			return pw;
		}

		BufferedReader getReader() throws IOException {
			if (br == null) {
				br = new BufferedReader(
						new InputStreamReader(getInputStream(), UTF_8));
			}
			return br;
		}
	}

	@Override
	public String toString() {
		return addr + " dead: " + dead + "  " + defaultTimeout;
	}
}
