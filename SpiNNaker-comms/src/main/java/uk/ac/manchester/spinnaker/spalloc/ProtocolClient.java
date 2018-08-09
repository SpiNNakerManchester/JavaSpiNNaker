package uk.ac.manchester.spinnaker.spalloc;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE;
import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;

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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.Command;
import uk.ac.manchester.spinnaker.spalloc.messages.CreateJobCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.DestroyJobCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.ExceptionResponse;
import uk.ac.manchester.spinnaker.spalloc.messages.GetBoardAtPositionCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.GetBoardPositionCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.GetJobMachineInfoCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.GetJobStateCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;
import uk.ac.manchester.spinnaker.spalloc.messages.JobKeepAliveCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.JobsChangedNotification;
import uk.ac.manchester.spinnaker.spalloc.messages.ListJobsCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.ListJobsResponse;
import uk.ac.manchester.spinnaker.spalloc.messages.ListMachinesCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.ListMachinesResponse;
import uk.ac.manchester.spinnaker.spalloc.messages.Machine;
import uk.ac.manchester.spinnaker.spalloc.messages.MachinesChangedNotification;
import uk.ac.manchester.spinnaker.spalloc.messages.NoNotifyJobCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.NoNotifyMachineCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.Notification;
import uk.ac.manchester.spinnaker.spalloc.messages.NotifyJobCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.NotifyMachineCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.PowerOffJobBoardsCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.PowerOnJobBoardsCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.Response;
import uk.ac.manchester.spinnaker.spalloc.messages.ReturnResponse;
import uk.ac.manchester.spinnaker.spalloc.messages.VersionCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIsJobChipCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIsMachineBoardLogicalCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIsMachineBoardPhysicalCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIsMachineChipCommand;

/**
 * A simple (blocking) client implementation of the
 * <a href="https://github.com/project-rig/spalloc_server">spalloc-server</a>
 * protocol.
 * <p>
 * This minimal implementation is intended to serve both simple applications and
 * as an example implementation of the protocol for other applications. This
 * implementation simply implements the protocol, presenting an RPC-like
 * interface to the server. For a higher-level interface built on top of this
 * client, see {@link Job}.
 */
public class ProtocolClient implements Closeable {
	/** The default spalloc port. */
	public static final int DEFAULT_PORT = 22244;
	/** The default communication timeout. (This is no timeout at all.) */
	public static final Integer DEFAULT_TIMEOUT = null;

	private final String hostname;
	private final int port;
	private final Integer defaultTimeout;

	private boolean dead;

	/**
	 * Mapping from threads to sockets. Kept because we need to have way to shut
	 * down all sockets at once.
	 */
	private final Map<Thread, TextSocket> socks = new HashMap<>();
	/** The thread-aware socket factory. */
	private final ProtocolThreadLocal local = new ProtocolThreadLocal();
	/** A queue of unprocessed notifications. */
	private final Deque<Notification> notifications = new LinkedList<>();
	private final Object socksLock = new Object();
	private final Object notificationsLock = new Object();
	private static final ObjectMapper MAPPER = new ObjectMapper();

	static {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Response.class, new ResponseBasedDeserializer());
		MAPPER.registerModule(module);
		MAPPER.setPropertyNamingStrategy(SNAKE_CASE);
		MAPPER.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Define a new connection. <b>NB:</b> Does not connect to the server until
	 * {@link #connect()} is called.
	 *
	 * @param hostname
	 *            The hostname of the server.
	 * @param port
	 *            The port to use (default: 22244).
	 * @param timeout
	 *            The default timeout.
	 */
	public ProtocolClient(String hostname, int port, Integer timeout) {
		this.hostname = hostname;
		this.port = port;
		this.dead = true;
		this.defaultTimeout = timeout;
	}

	@SuppressWarnings("serial")
	private static class ResponseBasedDeserializer
			extends PropertyBasedDeserialiser<Response> {
		ResponseBasedDeserializer() {
			super(Response.class);
			register("jobs_changed", JobsChangedNotification.class);
			register("machines_changed", MachinesChangedNotification.class);
			register("return", ReturnResponse.class);
			register("exception", ExceptionResponse.class);
		}
	}

	private static class TextSocket extends Socket {
		private BufferedReader br;
		private PrintWriter pw;
		private static final Charset UTF8 = Charset.forName("UTF-8");

		public PrintWriter getWriter() throws IOException {
			if (pw == null) {
				pw = new PrintWriter(
						new OutputStreamWriter(getOutputStream(), UTF8));
			}
			return pw;
		}

		public BufferedReader getReader() throws IOException {
			if (br == null) {
				br = new BufferedReader(
						new InputStreamReader(getInputStream(), UTF8));
			}
			return br;
		}
	}

	public AutoCloseable withConnection() throws IOException {
		connect();
		return () -> close();
	}

	private TextSocket getConnection(Integer timeout) throws IOException {
		if (dead) {
			throw new EOFException("not connected");
		}
		boolean connectNeeded = false;
		Thread key = currentThread();
		TextSocket sock;
		synchronized (socksLock) {
			sock = local.get();
			if (!socks.containsKey(key)) {
				socks.put(key, sock);
				connectNeeded = true;
			}
		}

		if (connectNeeded) {
			if (timeout != null) {
				sock.setSoTimeout(timeout);
			}
			if (!doConnect(sock)) {
				close(key);
				return getConnection(timeout);
			}
		}

		if (timeout != null) {
			sock.setSoTimeout(timeout);
		}
		return sock;
	}

	private boolean doConnect(Socket sock) throws IOException {
		boolean success = false;
		try {
			sock.connect(new InetSocketAddress(hostname, port));
			success = true;
		} catch (IOException e) {
			if (!e.getMessage().contains("EISCONN")) {
				throw e;
			}
		}
		return success;
	}

	private boolean hasOpenSocket() {
		return !local.get().isClosed();
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
	 * @throws IOException
	 *             If a connection failure occurs.
	 */
	public void connect(Integer timeout) throws IOException {
		// Close any existing connection
		if (hasOpenSocket()) {
			close(null);
		}
		dead = false;
		doConnect(timeout);
	}

	/** Try to (re)connect to the server. */
	private TextSocket doConnect(Integer timeout) throws IOException {
		try {
			return getConnection(timeout);
		} catch (IOException e) {
			// Failure, try again...
			close(null);
			// Pass on the exception
			throw e;
		}
	}

	private void close(Thread key) throws IOException {
		if (key == null) {
			key = currentThread();
		}
		Socket sock;
		synchronized (socksLock) {
			sock = socks.get(key);
			if (sock == null) {
				return;
			}
			socks.remove(key);
		}
		if (key == currentThread()) {
			local.remove();
		}
		sock.close();
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
		Iterable<Thread> keys;
		synchronized (socksLock) {
			keys = new ArrayList<>(socks.keySet());
		}
		for (Thread key : keys) {
			close(key);
		}
		local.remove();
	}

	/**
	 * Receive a line of JSON from the server.
	 *
	 * @param timeout
	 *            The number of milliseconds to wait before timing out or
	 *            <tt>null</tt> if this function should try again forever.
	 * @return The unpacked JSON line received.
	 * @throws ProtocolTimeoutException
	 *             If a timeout occurs.
	 * @throws IOException
	 *             If the socket is unusable or becomes disconnected.
	 */
	protected Response receiveJson(Integer timeout) throws IOException {
		TextSocket sock = getConnection(timeout);

		// Wait for some data to arrive
		try {
			String line = sock.getReader().readLine();
			if (line == null) {
				throw new EOFException("Connection closed");
			}
			return MAPPER.readValue(line, Response.class);
		} catch (SocketTimeoutException e) {
			throw new ProtocolTimeoutException("recv timed out", e);
		}
	}

	/**
	 * Attempt to send a command as a line of JSON to the server.
	 *
	 * @param command
	 *            The command to serialise.
	 * @param timeout
	 *            The number of milliseconds to wait before timing out or
	 *            <tt>null</tt> if this function should try again forever.
	 * @throws ProtocolTimeoutException
	 *             If a timeout occurs.
	 * @throws IOException
	 *             If the socket is unusable or becomes disconnected.
	 */
	protected void sendJson(Command<?> command, Integer timeout)
			throws IOException {
		TextSocket sock = getConnection(timeout);

		// Send the line
		String msg = MAPPER.writeValueAsString(command);
		try {
			sock.getWriter().println(msg);
		} catch (SocketTimeoutException e) {
			throw new ProtocolTimeoutException("send timed out", e);
		}
	}

	/** Convert a timestamp into how long to wait for it. */
	private static Integer timeLeft(Long timestamp) {
		if (timestamp == null) {
			return null;
		}
		return max(0, (int) (timestamp - currentTimeMillis()));
	}

	/** Check if a timestamp has been reached. */
	private static boolean timedOut(Long timestamp) {
		return timestamp != null && timestamp < currentTimeMillis();
	}

	/** Convert a delay (in milliseconds) into a timestamp. */
	private static Long makeTimeout(Integer delay) {
		if (delay == null) {
			return null;
		}
		return currentTimeMillis() + delay;
	}

	/**
	 * Send a command to the server and return the reply.
	 *
	 * @param command
	 *            The command to send.
	 * @param timeout
	 *            The number of milliseconds to wait before timing out or None
	 *            if this function should wait forever.
	 * @return The result string returned by the server.
	 * @throws SpallocServerException
	 *             If the server sends an error.
	 * @throws ProtocolTimeoutException
	 *             If a timeout occurs.
	 * @throws ProtocolException
	 *             If the connection is unavailable or is closed.
	 */
	protected String call(Command<?> command, Integer timeout)
			throws SpallocServerException, ProtocolTimeoutException,
			ProtocolException {
		try {
			Long finishTime = makeTimeout(timeout);

			// Construct and send the command message
			sendJson(command, timeout);

			// Command sent! Attempt to receive the response...
			while (!timedOut(finishTime)) {
				Response r = receiveJson(timeLeft(finishTime));
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
					synchronized (notificationsLock) {
						notifications.push((Notification) r);
					}
				} else {
					throw new ProtocolException(
							"bad response: " + r.getClass());
				}
			}
			throw new ProtocolTimeoutException(
					"timed out while calling " + command.getCommand());
		} catch (ProtocolTimeoutException e) {
			throw e;
		} catch (IOException e) {
			throw new ProtocolException(e);
		}
	}

	/**
	 * Return the next notification to arrive.
	 *
	 * @param timeout
	 *            The number of seconds to wait before timing out or
	 *            <tt>null</tt> if this function should try again forever. If
	 *            negative, only responses already-received will be returned; if
	 *            no responses are available, in this case the function does not
	 *            raise a ProtocolTimeoutError but returns <tt>null</tt>
	 *            instead.
	 * @return The notification sent by the server.
	 * @throws ProtocolTimeoutException
	 *             If a timeout occurs.
	 * @throws ProtocolException
	 *             If the socket is unusable or becomes disconnected.
	 */
	public Notification waitForNotification(Integer timeout)
			throws ProtocolException, ProtocolTimeoutException {
		// If we already have a notification, return it
		synchronized (notificationsLock) {
			if (!notifications.isEmpty()) {
				return notifications.pop();
			}
		}

		// Check for a duff timeout
		if (timeout != null && timeout < 0) {
			return null;
		}

		// Otherwise, wait for a notification to arrive
		try {
			return (Notification) receiveJson(timeout);
		} catch (ProtocolTimeoutException e) {
			throw e;
		} catch (IOException e) {
			throw new ProtocolException(e);
		}
	}

	/*
	 * The bindings of the Spalloc protocol methods themselves.
	 */

	public Version version(Integer timeout)
			throws IOException, SpallocServerException {
		return new Version(call(new VersionCommand(), timeout));
	}

	public int createJob(List<Integer> args, Map<String, Object> kwargs,
			Integer timeout) throws IOException, SpallocServerException {
		// If no owner, don't bother with the call
		if (!kwargs.containsKey("owner")) {
			throw new SpallocServerException(
					"owner must be specified for all jobs.");
		}
		return parseInt(call(new CreateJobCommand(args, kwargs), timeout));
	}

	public void jobKeepAlive(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		call(new JobKeepAliveCommand(jobID), timeout);
	}

	public JobState getJobState(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		return MAPPER.readValue(call(new GetJobStateCommand(jobID), timeout),
				JobState.class);
	}

	public JobMachineInfo getJobMachineInfo(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		return MAPPER.readValue(
				call(new GetJobMachineInfoCommand(jobID), timeout),
				JobMachineInfo.class);
	}

	public void powerOnJobBoards(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		call(new PowerOnJobBoardsCommand(jobID), timeout);
	}

	public void powerOffJobBoards(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		call(new PowerOffJobBoardsCommand(jobID), timeout);
	}

	public void destroyJob(int jobID, String reason, Integer timeout)
			throws IOException, SpallocServerException {
		call(new DestroyJobCommand(jobID, reason), timeout);
	}

	public void notifyJob(Integer jobID, Integer timeout)
			throws IOException, SpallocServerException {
		if (jobID == null) {
			call(new NotifyJobCommand(), timeout);
		} else {
			call(new NotifyJobCommand(jobID), timeout);
		}
	}

	public void noNotifyJob(Integer jobID, Integer timeout)
			throws IOException, SpallocServerException {
		if (jobID == null) {
			call(new NoNotifyJobCommand(), timeout);
		} else {
			call(new NoNotifyJobCommand(jobID), timeout);
		}
	}

	public void notifyMachine(String machineName, Integer timeout)
			throws IOException, SpallocServerException {
		if (machineName == null) {
			call(new NotifyMachineCommand(), timeout);
		} else {
			call(new NotifyMachineCommand(machineName), timeout);
		}
	}

	public void noNotifyMachine(String machineName, Integer timeout)
			throws IOException, SpallocServerException {
		if (machineName == null) {
			call(new NoNotifyMachineCommand(), timeout);
		} else {
			call(new NoNotifyMachineCommand(machineName), timeout);
		}
	}

	public List<JobDescription> listJobs(Integer timeout)
			throws IOException, SpallocServerException {
		return MAPPER.readValue(call(new ListJobsCommand(), timeout),
				ListJobsResponse.class).getJobs();
	}

	public List<Machine> listMachines(Integer timeout)
			throws IOException, SpallocServerException {
		return MAPPER.readValue(call(new ListMachinesCommand(), timeout),
				ListMachinesResponse.class).getMachines();
	}

	public BoardPhysicalCoordinates getBoardPosition(String machineName,
			BoardCoordinates coords, Integer timeout)
			throws IOException, SpallocServerException {
		return MAPPER.readValue(
				call(new GetBoardPositionCommand(machineName, coords),
						timeout),
				BoardPhysicalCoordinates.class);
	}

	public BoardCoordinates getBoardPosition(String machineName,
			BoardPhysicalCoordinates coords, Integer timeout)
			throws IOException, SpallocServerException {
		return MAPPER.readValue(
				call(new GetBoardAtPositionCommand(machineName, coords),
						timeout),
				BoardCoordinates.class);
	}

	public WhereIs whereIs(int jobID, HasChipLocation chip, Integer timeout)
			throws IOException, SpallocServerException {
		return MAPPER.readValue(
				call(new WhereIsJobChipCommand(jobID, chip), timeout),
				WhereIs.class);
	}

	public WhereIs whereIs(String machine, HasChipLocation chip,
			Integer timeout) throws IOException, SpallocServerException {
		return MAPPER.readValue(
				call(new WhereIsMachineChipCommand(machine, chip), timeout),
				WhereIs.class);
	}

	public WhereIs whereIs(String machine, int cabinet, int frame, int board,
			Integer timeout) throws IOException, SpallocServerException {
		return MAPPER
				.readValue(
						call(new WhereIsMachineBoardPhysicalCommand(machine,
								cabinet, frame, board), timeout),
						WhereIs.class);
	}

	public WhereIs whereIs(String machine, BoardCoordinates coords,
			Integer timeout) throws IOException, SpallocServerException {
		return MAPPER.readValue(
				call(new WhereIsMachineBoardLogicalCommand(machine, coords),
						timeout),
				WhereIs.class);
	}

	/**
	 * Subclass of threading.local to ensure that we get sane initialisation of
	 * our state in each thread.
	 */
	static class ProtocolThreadLocal extends ThreadLocal<TextSocket> {
		@Override
		protected TextSocket initialValue() {
			return new TextSocket();
		}
	}
}
