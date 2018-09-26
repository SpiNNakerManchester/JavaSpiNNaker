package uk.ac.manchester.spinnaker.spalloc;

import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;

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
	private final Map<Thread, TextSocket> socks = new HashMap<>();
	/** Lock for access to {@link #socks}. */
	private final Object socksLock = new Object();
	/**
	 * The thread-aware socket factory. Every thread gets exactly one socket.
	 */
	private final TextSocketFactory local = new TextSocketFactory();
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
	public SpallocConnection(String hostname, int port, Integer timeout) {
		addr = new InetSocketAddress(hostname, port);
		this.dead = true;
		this.defaultTimeout = timeout;
	}

	/**
	 * Context adapter. Allows this code to be used like this:
	 *
	 * <pre>
	 * try (AutoCloseable c = client.withConnection()) {
	 *     ...
	 * }
	 * </pre>
	 *
	 * @return the auto-closeable context.
	 * @throws IOException
	 *             If the connect to the spalloc server fails.
	 */
	public AutoCloseable withConnection() throws IOException {
		connect();
		return this::close;
	}

	private TextSocket getConnection(Integer timeout) throws IOException {
		TextSocket sock = null;
		Thread key = currentThread();
		/*
		 * This loop will keep trying to connect until the socket exists and is
		 * in a connected state. It's labelled just for clarity.
		 */
		while (sock == null) {
			if (dead) {
				throw new EOFException("not connected");
			}
			sock = getConnectedSocket(key, timeout);
		}

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
	 * @return The socket, or <tt>null</tt> if the connection failed.
	 * @throws IOException
	 *             if something really bad goes wrong.
	 */
	private TextSocket getConnectedSocket(Thread key, Integer timeout)
			throws IOException {
		TextSocket sock;
		boolean connectNeeded = false;
		synchronized (socksLock) {
			sock = local.get();
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
		Socket s = local.get();
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

	private void closeThreadConnection(Thread key) throws IOException {
		Socket sock;
		synchronized (socksLock) {
			sock = socks.remove(key);
		}
		if (sock != null) {
			// Mark the thread local so it will reinitialise
			if (key == currentThread()) {
				local.remove();
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
			keys = new ArrayList<>(socks.keySet());
		}
		for (Thread key : keys) {
			closeThreadConnection(key);
		}
		local.remove();
	}

	/**
	 * Receive a line from the server with a response.
	 *
	 * @param timeout
	 *            The number of milliseconds to wait before timing out or
	 *            <tt>null</tt> if this function should try again forever.
	 * @return The unpacked response from the line received.
	 * @throws SpallocProtocolTimeoutException
	 *             If a timeout occurs.
	 * @throws IOException
	 *             If the socket is unusable or becomes disconnected.
	 */
	protected Response receiveResponse(Integer timeout)
			throws SpallocProtocolTimeoutException, IOException {
		TextSocket sock = getConnection(timeout);

		// Wait for some data to arrive
		try {
			String line = sock.getReader().readLine();
			if (line == null) {
				throw new EOFException("Connection closed");
			}
			Response response = parseResponse(line);
			if (response == null) {
				throw new SpallocProtocolException(
						"unexpected response: " + line);
			}
			return response;
		} catch (SocketTimeoutException e) {
			throw new SpallocProtocolTimeoutException("recv timed out", e);
		}
	}

	/**
	 * Attempt to send a command as a line to the server.
	 *
	 * @param command
	 *            The command to serialise.
	 * @param timeout
	 *            The number of milliseconds to wait before timing out or
	 *            <tt>null</tt> if this function should try again forever.
	 * @throws SpallocProtocolTimeoutException
	 *             If a timeout occurs.
	 * @throws IOException
	 *             If the socket is unusable or becomes disconnected.
	 */
	protected void sendCommand(Command<?> command, Integer timeout)
			throws SpallocProtocolTimeoutException, IOException {
		log.debug("sending a {}", command.getClass());
		TextSocket sock = getConnection(timeout);

		// Send the line
		String msg = formatRequest(command);
		try {
			PrintWriter pw = sock.getWriter();
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
	 *            The request to format for sending. Not <tt>null</tt>.
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
	 *            The line to parse. Not <tt>null</tt>. Has the terminating
	 *            newline removed.
	 * @return The parsed response, or <tt>null</tt> for a generic "that's
	 *         unexpected".
	 * @throws IOException
	 *             If parsing completely fails.
	 */
	protected abstract Response parseResponse(String line) throws IOException;

	/**
	 * Send a command to the server and return the reply.
	 *
	 * @param command
	 *            The command to send.
	 * @param timeout
	 *            The number of milliseconds to wait before timing out or
	 *            <tt>null</tt> if this function should wait forever.
	 * @return The result string returned by the server.
	 * @throws SpallocServerException
	 *             If the server sends an error.
	 * @throws SpallocProtocolTimeoutException
	 *             If a timeout occurs.
	 * @throws SpallocProtocolException
	 *             If the connection is unavailable or is closed.
	 */
	protected String call(Command<?> command, Integer timeout)
			throws SpallocServerException, SpallocProtocolTimeoutException,
			SpallocProtocolException {
		try {
			Long finishTime = makeTimeout(timeout);

			// Construct and send the command message
			sendCommand(command, timeout);

			// Command sent! Attempt to receive the response...
			while (!timedOut(finishTime)) {
				Response r = receiveResponse(timeLeft(finishTime));
				if (r == null) {
					continue;
				}
				if (r instanceof ReturnResponse) {
					// Success!
					return ((ReturnResponse) r).getReturnValue();
				} else if (r instanceof ExceptionResponse) {
					// Server error!
					throw new SpallocServerException((ExceptionResponse) r);
				} else if (r instanceof Notification) {
					// Got a notification, keep trying...
					notifications.add((Notification) r);
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

	/**
	 * Subclass of ThreadLocal to ensure that we get sane initialisation of our
	 * state in each thread.
	 */
	private static class TextSocketFactory extends ThreadLocal<TextSocket> {
		@Override
		protected TextSocket initialValue() {
			return new TextSocket();
		}
	}

    @Override
	public String toString() {
        return addr + " dead: " + dead + "  " + defaultTimeout;
    }
}
