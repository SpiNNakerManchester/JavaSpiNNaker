package uk.ac.manchester.spinnaker.spalloc;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE;
import static java.lang.Integer.parseInt;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.KEEPALIVE_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MACHINE_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_BOARDS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_LINKS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MIN_RATIO_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.REQUIRE_TORUS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.TAGS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.USER_PROPERTY;
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * <a href="https://github.com/SpiNNakerManchester/spalloc_server">
 * spalloc-server</a> protocol.
 * <p>
 * This minimal implementation is intended to serve both simple applications and
 * as an example implementation of the protocol for other applications. This
 * implementation simply implements the protocol, presenting an RPC-like
 * interface to the server. For a higher-level interface built on top of this
 * client, see {@link SpallocJob}.
 */
public class SpallocClient implements Closeable, SpallocAPI {
	/** The default spalloc port. */
	public static final int DEFAULT_PORT = 22244;
	/** The default communication timeout. (This is no timeout at all.) */
	public static final Integer DEFAULT_TIMEOUT = null;

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
	private final Deque<Notification> notifications = new LinkedList<>();
	/** Lock for access to {@link #notifications}. */
	private final Object notificationsLock = new Object();

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Set<String> ALLOWED_KWARGS = new HashSet<>();

	static {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Response.class, new ResponseBasedDeserializer());
		MAPPER.registerModule(module);
		MAPPER.setPropertyNamingStrategy(SNAKE_CASE);
		MAPPER.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

		ALLOWED_KWARGS.addAll(asList(USER_PROPERTY, KEEPALIVE_PROPERTY,
				MACHINE_PROPERTY, TAGS_PROPERTY, MIN_RATIO_PROPERTY,
				MAX_DEAD_BOARDS_PROPERTY, MAX_DEAD_LINKS_PROPERTY,
				REQUIRE_TORUS_PROPERTY));
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
	public SpallocClient(String hostname, int port, Integer timeout) {
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
	 * Receive a line of JSON from the server.
	 *
	 * @param timeout
	 *            The number of milliseconds to wait before timing out or
	 *            <tt>null</tt> if this function should try again forever.
	 * @return The unpacked JSON line received.
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
			return MAPPER.readValue(line, Response.class);
		} catch (SocketTimeoutException e) {
			throw new SpallocProtocolTimeoutException("recv timed out", e);
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
	 * @throws SpallocProtocolTimeoutException
	 *             If a timeout occurs.
	 * @throws IOException
	 *             If the socket is unusable or becomes disconnected.
	 */
	protected void sendCommand(Command<?> command, Integer timeout)
			throws SpallocProtocolTimeoutException, IOException {
		TextSocket sock = getConnection(timeout);

		// Send the line
		String msg = MAPPER.writeValueAsString(command);
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
					synchronized (notificationsLock) {
						notifications.push((Notification) r);
					}
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

	@Override
	public Notification waitForNotification(Integer timeout)
			throws SpallocProtocolException, SpallocProtocolTimeoutException {
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
			return (Notification) receiveResponse(timeout);
		} catch (SpallocProtocolTimeoutException e) {
			throw e;
		} catch (IOException e) {
			throw new SpallocProtocolException(e);
		}
	}

	/*
	 * The bindings of the Spalloc protocol methods themselves.
	 */

	@Override
	public Version version(Integer timeout)
			throws IOException, SpallocServerException {
		return new Version(call(new VersionCommand(), timeout));
	}

	@Override
	public int createJob(List<Integer> args, Map<String, Object> kwargs,
			Integer timeout) throws IOException, SpallocServerException {
		// If no owner, don't bother with the call
		if (!kwargs.containsKey(USER_PROPERTY)) {
			throw new SpallocServerException(
					USER_PROPERTY + " must be specified for all jobs.");
		}
		kwargs.keySet().retainAll(ALLOWED_KWARGS);
		return parseInt(call(new CreateJobCommand(args, kwargs), timeout));
	}

	@Override
	public void jobKeepAlive(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		call(new JobKeepAliveCommand(jobID), timeout);
	}

	@Override
	public JobState getJobState(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		return MAPPER.readValue(call(new GetJobStateCommand(jobID), timeout),
				JobState.class);
	}

	@Override
	public JobMachineInfo getJobMachineInfo(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		return MAPPER.readValue(
				call(new GetJobMachineInfoCommand(jobID), timeout),
				JobMachineInfo.class);
	}

	@Override
	public void powerOnJobBoards(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		call(new PowerOnJobBoardsCommand(jobID), timeout);
	}

	@Override
	public void powerOffJobBoards(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		call(new PowerOffJobBoardsCommand(jobID), timeout);
	}

	@Override
	public void destroyJob(int jobID, String reason, Integer timeout)
			throws IOException, SpallocServerException {
		call(new DestroyJobCommand(jobID, reason), timeout);
	}

	@Override
	public void notifyJob(Integer jobID, Integer timeout)
			throws IOException, SpallocServerException {
		if (jobID == null) {
			call(new NotifyJobCommand(), timeout);
		} else {
			call(new NotifyJobCommand(jobID), timeout);
		}
	}

	@Override
	public void noNotifyJob(Integer jobID, Integer timeout)
			throws IOException, SpallocServerException {
		if (jobID == null) {
			call(new NoNotifyJobCommand(), timeout);
		} else {
			call(new NoNotifyJobCommand(jobID), timeout);
		}
	}

	@Override
	public void notifyMachine(String machineName, Integer timeout)
			throws IOException, SpallocServerException {
		if (machineName == null) {
			call(new NotifyMachineCommand(), timeout);
		} else {
			call(new NotifyMachineCommand(machineName), timeout);
		}
	}

	@Override
	public void noNotifyMachine(String machineName, Integer timeout)
			throws IOException, SpallocServerException {
		if (machineName == null) {
			call(new NoNotifyMachineCommand(), timeout);
		} else {
			call(new NoNotifyMachineCommand(machineName), timeout);
		}
	}

	@Override
	public List<JobDescription> listJobs(Integer timeout)
			throws IOException, SpallocServerException {
		return MAPPER.readValue(call(new ListJobsCommand(), timeout),
				ListJobsResponse.class).getJobs();
	}

	@Override
	public List<Machine> listMachines(Integer timeout)
			throws IOException, SpallocServerException {
		return MAPPER.readValue(call(new ListMachinesCommand(), timeout),
				ListMachinesResponse.class).getMachines();
	}

	@Override
	public BoardPhysicalCoordinates getBoardPosition(String machineName,
			BoardCoordinates coords, Integer timeout)
			throws IOException, SpallocServerException {
		return MAPPER.readValue(
				call(new GetBoardPositionCommand(machineName, coords), timeout),
				BoardPhysicalCoordinates.class);
	}

	@Override
	public BoardCoordinates getBoardPosition(String machineName,
			BoardPhysicalCoordinates coords, Integer timeout)
			throws IOException, SpallocServerException {
		return MAPPER.readValue(
				call(new GetBoardAtPositionCommand(machineName, coords),
						timeout),
				BoardCoordinates.class);
	}

	@Override
	public WhereIs whereIs(int jobID, HasChipLocation chip, Integer timeout)
			throws IOException, SpallocServerException {
		return MAPPER.readValue(
				call(new WhereIsJobChipCommand(jobID, chip), timeout),
				WhereIs.class);
	}

	@Override
	public WhereIs whereIs(String machine, HasChipLocation chip,
			Integer timeout) throws IOException, SpallocServerException {
		return MAPPER.readValue(
				call(new WhereIsMachineChipCommand(machine, chip), timeout),
				WhereIs.class);
	}

	@Override
	public WhereIs whereIs(String machine, BoardPhysicalCoordinates coords,
			Integer timeout) throws IOException, SpallocServerException {
		return MAPPER.readValue(
				call(new WhereIsMachineBoardPhysicalCommand(machine, coords),
						timeout),
				WhereIs.class);
	}

	@Override
	public WhereIs whereIs(String machine, BoardCoordinates coords,
			Integer timeout) throws IOException, SpallocServerException {
		return MAPPER.readValue(
				call(new WhereIsMachineBoardLogicalCommand(machine, coords),
						timeout),
				WhereIs.class);
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

	/**
	 * Subclass of Socket to encapsulate reading and writing text by lines. This
	 * handles all buffering internally, locking that close to the socket
	 * itself.
	 */
	private static class TextSocket extends Socket {
		private BufferedReader br;
		private PrintWriter pw;
		private static final Charset UTF8 = Charset.forName("UTF-8");

		PrintWriter getWriter() throws IOException {
			if (pw == null) {
				pw = new PrintWriter(
						new OutputStreamWriter(getOutputStream(), UTF8));
			}
			return pw;
		}

		BufferedReader getReader() throws IOException {
			if (br == null) {
				br = new BufferedReader(
						new InputStreamReader(getInputStream(), UTF8));
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
}
