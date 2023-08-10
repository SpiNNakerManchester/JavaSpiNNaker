/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.compat;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.IOUtils.buffer;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.compat.Utils.parseDec;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.OFF;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.ON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.alloc.model.PowerState;
import uk.ac.manchester.spinnaker.machine.ValidP;
import uk.ac.manchester.spinnaker.machine.ValidX;
import uk.ac.manchester.spinnaker.machine.ValidY;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.machine.board.ValidBoardNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidCabinetNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidFrameNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadHeight;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadWidth;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadX;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadY;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.Machine;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * The core of tasks that handle connections by clients.
 *
 * @author Donal Fellows
 */
public abstract class V1CompatTask extends V1CompatService.Aware {
	private static final Logger log = getLogger(V1CompatTask.class);

	private static final int TRIAD_COORD_COUNT = 3;

	/**
	 * The socket that this task is handling.
	 */
	private final Socket sock;

	/**
	 * How to read from the socket. The protocol expects messages to be UTF-8
	 * lines, with each line being a JSON document.
	 */
	private final BufferedReader in;

	/**
	 * How to write to the socket. The protocol expects messages to be UTF-8
	 * lines, with each line being a JSON document.
	 * <p>
	 * Note that synchronisation will be performed on this object.
	 */
	@GuardedBy("itself")
	private final ExceptionPrintWriter out;

	private class ExceptionPrintWriter extends PrintWriter {

		public ExceptionPrintWriter(Writer out) {
			super(out);
		}

		public void flush() {
			try {
				synchronized (lock) {
					out.flush();
				}
			}
			catch (IOException x) {
				x.printStackTrace();
			}
		}

		public void close() {
			log.info("Closing");
			try {
				synchronized (lock) {
					if (out == null)
						return;
					out.close();
					out = null;
				}
			}
			catch (IOException x) {
				x.printStackTrace();
			}
		}

		public void write(int c) {
			try {
				synchronized (lock) {
					out.write(c);
				}
			}
			catch (InterruptedIOException x) {
				Thread.currentThread().interrupt();
			}
			catch (IOException x) {
				x.printStackTrace();
			}
		}

		public void write(char buf[], int off, int len) {
			try {
				synchronized (lock) {
					out.write(buf, off, len);
				}
			}
			catch (InterruptedIOException x) {
				Thread.currentThread().interrupt();
			}
			catch (IOException x) {
				x.printStackTrace();
			}
		}

		public void write(String s, int off, int len) {
			try {
				synchronized (lock) {
					out.write(s, off, len);
				}
			}
			catch (InterruptedIOException x) {
				Thread.currentThread().interrupt();
			}
			catch (IOException x) {
				x.printStackTrace();
			}
		}
	}

	/**
	 * Make an instance that wraps a socket.
	 *
	 * @param srv
	 *            The overall service, used for looking up shared resources that
	 *            are uncomfortable as beans.
	 * @param sock
	 *            The socket that talks to the client.
	 * @throws IOException
	 *             If access to the socket fails.
	 */
	protected V1CompatTask(V1CompatService srv, Socket sock)
			throws IOException {
		super(srv);
		this.sock = sock;
		sock.setTcpNoDelay(true);
		sock.setSoTimeout((int) getProperties().getReceiveTimeout().toMillis());

		in = buffer(new InputStreamReader(sock.getInputStream(), UTF_8));
		out = new ExceptionPrintWriter(
				new OutputStreamWriter(sock.getOutputStream(), UTF_8));
	}

	/**
	 * Constructor for testing. Makes a task that isn't connected to a socket.
	 *
	 * @param srv
	 *            The overall service, used for looking up shared resources that
	 *            are uncomfortable as beans.
	 * @param in
	 *            Input to the task.
	 * @param out
	 *            Output to the task.
	 */
	protected V1CompatTask(V1CompatService srv, Reader in, Writer out) {
		super(srv);
		this.sock = null;
		this.in = buffer(in);
		this.out = new ExceptionPrintWriter(out);
	}

	final void handleConnection() {
		log.debug("waiting for commands from {}", sock);
		try {
			while (!interrupted()) {
				if (!communicate()) {
					log.debug("Communcation break");
					break;
				}
			}
			if (interrupted()) {
				log.debug("Shutdown on interrupt");
			}
		} catch (UnknownIOException e) {
			/*
			 * Nothing useful to do in this case except close.
			 *
			 * This happens when the problem is detected by a PrintWriter, but
			 * the problem with PrintWriters is they swallow exceptions and
			 * throw the information away. I'm not going to fix that.
			 */
			log.error("Something went wrong in comms", e);
		} catch (InterruptedException | InterruptedIOException interrupted) {
			log.debug("interrupted", interrupted);
		} catch (IOException e) {
			log.error("problem with socket {}", sock, e);
		} finally {
			log.debug("closing down connection from {}", sock);
			closeNotifiers();
			try {
				if (nonNull(sock)) {
					sock.close();
				} else {
					in.close();
					synchronized (out) {
						out.close();
					}
				}
			} catch (IOException e) {
				log.error("problem closing socket {}", sock, e);
			}
		}
	}

	/**
	 * Stop any current running notifiers.
	 */
	protected abstract void closeNotifiers();

	/**
	 * What host is connected to this service instance?
	 *
	 * @return The remote host that this task is serving.
	 */
	public final String host() {
		if (isNull(sock)) {
			return "<NOWHERE>";
		}
		return ((InetSocketAddress) sock.getRemoteSocketAddress()).getAddress()
				.getHostAddress();
	}

	/**
	 * Parse a command from a message.
	 *
	 * @param msg
	 *            The message to parse.
	 * @return The command.
	 * @throws IOException
	 *             If the message doesn't contain a valid command.
	 */
	protected Command parseCommand(String msg) throws IOException {
		return getJsonMapper().readValue(msg, Command.class);
	}

	/**
	 * Parse a command that was saved in the DB.
	 *
	 * @param msg
	 *            The saved command to parse.
	 * @return The command, or {@code null} if the message can't be parsed.
	 */
	protected Command parseCommand(byte[] msg) {
		if (isNull(msg)) {
			return null;
		}
		try {
			return getJsonMapper().readValue(msg, Command.class);
		} catch (IOException e) {
			log.error("unexpected failure parsing JSON", e);
			return null;
		}
	}

	/**
	 * Read a command message from the client. The message will have occupied
	 * one line of text on the input stream from the socket.
	 *
	 * @return The parsed command message, or {@code empty} on end-of-stream.
	 * @throws IOException
	 *             If things go wrong, such as a bad or incomplete message.
	 * @throws InterruptedException
	 *             If interrupted.
	 */
	private Optional<Command> readMessage()
			throws IOException, InterruptedException {
		String line;
		try {
			line = in.readLine();
			log.info("Incoming message: {}", line);
		} catch (SocketException e) {
			/*
			 * Don't know why we get a generic socket exception for some of
			 * these, but it happens when there's been some sort of network drop
			 * or if the connection close happens in a weird order. Treating as
			 * EOF is the right thing.
			 *
			 * I also don't know why there is no nicer way of detecting this
			 * than matching the exception message. You'd think that you'd get
			 * something better, but no...
			 */
			switch (e.getMessage()) {
			case "Connection reset":
			case "Connection timed out (Read failed)":
				return Optional.empty();
			default:
				throw e;
			}
		}
		if (isNull(line) || line.isBlank()) {
			if (currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			return Optional.empty();
		}
		var c = parseCommand(line);
		if (isNull(c) || isNull(c.getCommand())) {
			throw new IOException("message did not specify a command");
		}
		log.info("Command: {}", c);
		return Optional.of(c);
	}

	/**
	 * Basic message send. Synchronised so that only full messages are written.
	 *
	 * @param msg
	 *            The message to send. Must serializable to JSON.
	 * @throws IOException
	 *             If the message can't be written.
	 */
	private void sendMessage(Object msg) throws IOException {
		// We go via a string to avoid early closing issues
		var data = getJsonMapper().writeValueAsString(msg);
		log.info("about to send message: {}", data);
		// Synch so we definitely don't interleave bits of messages
		synchronized (out) {
			out.println(data);
			if (out.checkError()) {
				throw new UnknownIOException();
			}
		}
	}

	private boolean mayWrite() {
		if (isNull(sock)) {
			return true;
		}
		return !sock.isClosed();
	}

	/**
	 * Send a response message.
	 *
	 * @param response
	 *            The body object of the response.
	 * @throws IOException
	 *             If network access fails, or the object isn't serializable as
	 *             JSON or a suitable primitive.
	 */
	protected final void writeResponse(Object response) throws IOException {
		if (mayWrite()) {
			sendMessage(new ReturnResponse(response));
		}
	}

	/**
	 * Send an exception message.
	 *
	 * @param exn
	 *            A description of the exception.
	 * @throws IOException
	 *             If network access fails.
	 */
	protected final void writeException(Throwable exn) throws IOException {
		if (mayWrite()) {
			if (nonNull(exn.getMessage())) {
				sendMessage(new ExceptionResponse(exn.getMessage()));
			} else {
				sendMessage(new ExceptionResponse(exn.toString()));
			}
		}
	}

	/**
	 * Send a notification about a collection of jobs changing.
	 *
	 * @param jobIds
	 *            The jobs that have changed. (Usually <em>all</em> jobs.)
	 * @throws IOException
	 *             If network access fails.
	 */
	protected final void writeJobNotification(List<Integer> jobIds)
			throws IOException {
		if (!jobIds.isEmpty() && mayWrite()) {
			sendMessage(new JobNotifyMessage(jobIds));
		}
	}

	/**
	 * Send a notification about a collection of machines changing.
	 *
	 * @param machineNames
	 *            The machines that have changed. (Usually <em>all</em>
	 *            machines.)
	 * @throws IOException
	 *             If network access fails.
	 */
	protected final void writeMachineNotification(
			List<String> machineNames)
			throws IOException {
		if (!machineNames.isEmpty() && mayWrite()) {
			sendMessage(new MachineNotifyMessage(machineNames));
		}
	}

	/**
	 * Read a message from the client and send a response.
	 *
	 * @return {@code true} if further messages should be processed,
	 *         {@code false} if the connection should be closed.
	 * @throws IOException
	 *             If network access fails.
	 * @throws InterruptedException
	 *             If interrupted (happens on service shutdown).
	 */
	public final boolean communicate()
			throws IOException, InterruptedException {
		Command cmd;
		try {
			var c = readMessage();
			if (!c.isPresent()) {
				log.debug("null message");
				return false;
			}
			cmd = c.orElseThrow();
		} catch (SocketTimeoutException e) {
			log.trace("timeout");
			// Message was not read by time timeout expired
			return !currentThread().isInterrupted();
		} catch (JsonMappingException | JsonParseException e) {
			log.error("Error on message reception", e);
			writeException(e);
			return true;
		}

		Object r;
		try {
			r = callOperation(cmd);
		} catch (Oops | TaskException | IllegalArgumentException e) {
			// Expected exceptions; don't log
			writeException(e);
			return true;
		} catch (Exception e) {
			log.warn("unexpected exception from {} operation",
					cmd.getCommand(), e);
			writeException(e);
			return true;
		}

		log.info("responded with {}", r);
		writeResponse(r);
		return true;
	}

	/**
	 * Decode the command to convert into a method to call.
	 *
	 * @param cmd
	 *            The command.
	 * @return The result of the command. Can be anything (<em>including</em>
	 *         {@code null}) as long as it can be serialised.
	 * @throws Exception
	 *             If things go wrong
	 */
	private Object callOperation(Command cmd) throws Exception {
		log.debug("calling operation '{}'", cmd.getCommand());
		var args = cmd.getArgs();
		var kwargs = cmd.getKwargs();
		switch (cmd.getCommand()) {
		case "create_job":
			// This is three operations really, and an optional parameter.
			byte[] serialCmd = getJsonMapper().writeValueAsBytes(cmd);
			switch (args.size()) {
			case 0:
				return createJobNumBoards(1, kwargs, serialCmd).orElse(null);
			case 1:
				return createJobNumBoards(parseDec(args, 0), kwargs, serialCmd)
						.orElse(null);
			case 2:
				return createJobRectangle(parseDec(args, 0), parseDec(args, 1),
						kwargs, serialCmd).orElse(null);
			case TRIAD_COORD_COUNT:
				return createJobSpecificBoard(new TriadCoords(parseDec(args, 0),
						parseDec(args, 1), parseDec(args, 2)), kwargs,
						serialCmd).orElse(null);
			default:
				throw new Oops(
						"unsupported number of arguments: " + args.size());
			}
		case "destroy_job":
			destroyJob(parseDec(args, 0), (String) kwargs.get("reason"));
			break;
		case "get_board_at_position":
			return requireNonNull(getBoardAtPhysicalPosition(
					(String) kwargs.get("machine_name"), parseDec(kwargs, "x"),
					parseDec(kwargs, "y"), parseDec(kwargs, "z")));
		case "get_board_position":
			return requireNonNull(getBoardAtLogicalPosition(
					(String) kwargs.get("machine_name"), parseDec(kwargs, "x"),
					parseDec(kwargs, "y"), parseDec(kwargs, "z")));
		case "get_job_machine_info":
			return requireNonNull(getJobMachineInfo(parseDec(args, 0)));
		case "get_job_state":
			return requireNonNull(getJobState(parseDec(args, 0)));
		case "job_keepalive":
			jobKeepalive(parseDec(args, 0));
			break;
		case "list_jobs":
			return requireNonNull(listJobs());
		case "list_machines":
			return requireNonNull(listMachines());
		case "no_notify_job":
			notifyJob(optInt(args), false);
			break;
		case "no_notify_machine":
			notifyMachine(optStr(args), false);
			break;
		case "notify_job":
			notifyJob(optInt(args), true);
			break;
		case "notify_machine":
			notifyMachine(optStr(args), true);
			break;
		case "power_off_job_boards":
			powerJobBoards(parseDec(args, 0), OFF);
			break;
		case "power_on_job_boards":
			powerJobBoards(parseDec(args, 0), ON);
			break;
		case "version":
			return requireNonNull(version());
		case "where_is":
			// This is four operations in a trench coat
			if (kwargs.containsKey("job_id")) {
				return requireNonNull(whereIsJobChip(parseDec(kwargs, "job_id"),
						parseDec(kwargs, "chip_x"),
						parseDec(kwargs, "chip_y")));
			} else if (!kwargs.containsKey("machine")) {
				throw new Oops("missing parameter: machine");
			}
			var m = (String) kwargs.get("machine");
			if (kwargs.containsKey("chip_x")) {
				return requireNonNull(
						whereIsMachineChip(m, parseDec(kwargs, "chip_x"),
								parseDec(kwargs, "chip_y")));
			} else if (kwargs.containsKey("x")) {
				return requireNonNull(
						whereIsMachineLogicalBoard(m, parseDec(kwargs, "x"),
								parseDec(kwargs, "y"), parseDec(kwargs, "z")));
			} else if (kwargs.containsKey("cabinet")) {
				return requireNonNull(whereIsMachinePhysicalBoard(m,
						parseDec(kwargs, "cabinet"), parseDec(kwargs, "frame"),
						parseDec(kwargs, "board")));
			} else {
				throw new Oops("missing parameter: chip_x, x, or cabinet");
			}
		case "report_problem":
			var ip = args.get(0).toString();
			Integer x = null, y = null, p = null;
			var desc = "It doesn't work and I don't know why.";
			if (kwargs.containsKey("x")) {
				x = parseDec(kwargs, "x");
				y = parseDec(kwargs, "y");
				if (kwargs.containsKey("p")) {
					p = parseDec(kwargs, "p");
				}
			}
			if (kwargs.containsKey("description")) {
				desc = Objects.toString(kwargs.get("description"));
			}
			reportProblem(ip, x, y, p, desc);
			break;
		case "login":
			throw new Oops("upgrading security is not supported");
		default:
			throw new Oops("unknown command: " + cmd.getCommand());
		}
		return null;
	}

	/**
	 * Create a job asking for a number of boards.
	 *
	 * @param numBoards
	 *            Number of boards.
	 * @param kwargs
	 *            Keyword argument map.
	 * @param cmd
	 *            The actual command, as serialised JSON.
	 * @return Job identifier.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract Optional<Integer> createJobNumBoards(
			@Positive int numBoards,
			Map<@NotBlank String, @NotNull Object> kwargs, byte[] cmd)
			throws TaskException;

	/**
	 * Create a job asking for a rectangle of boards.
	 *
	 * @param width
	 *            Width of rectangle in triads.
	 * @param height
	 *            Height of rectangle in triads.
	 * @param kwargs
	 *            Keyword argument map.
	 * @param cmd
	 *            The actual command, as serialised JSON.
	 * @return Job identifier.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract Optional<Integer> createJobRectangle(
			@ValidTriadWidth int width, @ValidTriadHeight int height,
			Map<@NotBlank String, @NotNull Object> kwargs, byte[] cmd)
			throws TaskException;

	/**
	 * Create a job asking for a specific board.
	 *
	 * @param coords
	 *            Which board, by its logical coordinates.
	 * @param kwargs
	 *            Keyword argument map.
	 * @param cmd
	 *            The actual command, as serialised JSON.
	 * @return Job identifier. Never {@code null}.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract Optional<Integer> createJobSpecificBoard(
			@Valid TriadCoords coords,
			Map<@NotBlank String, @NotNull Object> kwargs, byte[] cmd)
			throws TaskException;

	/**
	 * Destroy a job.
	 *
	 * @param jobId
	 *            Job identifier.
	 * @param reason
	 *            Why the machine is being destroyed.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract void destroyJob(int jobId, String reason)
			throws TaskException;

	/**
	 * Get the coordinates of a board at a physical location.
	 *
	 * @param machineName
	 *            Name of the machine.
	 * @param cabinet
	 *            Cabinet number.
	 * @param frame
	 *            Frame number.
	 * @param board
	 *            Board number.
	 * @return Logical location. Never {@code null}.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract BoardCoordinates getBoardAtPhysicalPosition(
			@NotBlank String machineName, @ValidCabinetNumber int cabinet,
			@ValidFrameNumber int frame, @ValidBoardNumber int board)
			throws TaskException;

	/**
	 * Get the physical location of a board at given coordinates.
	 *
	 * @param machineName
	 *            Name of the machine.
	 * @param x
	 *            Triad X coordinate.
	 * @param y
	 *            Triad Y coordinate.
	 * @param z
	 *            Triad Z coordinate.
	 * @return Physical location. Never {@code null}.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract BoardPhysicalCoordinates getBoardAtLogicalPosition(
			@NotBlank String machineName, @ValidTriadX int x,
			@ValidTriadY int y, @ValidTriadZ int z) throws TaskException;

	/**
	 * Get information about the machine allocated to a job.
	 *
	 * @param jobId
	 *            Job identifier.
	 * @return Description of job's (sub)machine. Never {@code null}.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract JobMachineInfo getJobMachineInfo(int jobId)
			throws TaskException;

	/**
	 * Get the state of a job.
	 *
	 * @param jobId
	 *            Job identifier.
	 * @return State description. Never {@code null}.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract JobState getJobState(int jobId) throws TaskException;

	/**
	 * Mark a job as still being kept alive.
	 *
	 * @param jobId
	 *            Job identifier.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract void jobKeepalive(int jobId) throws TaskException;

	/**
	 * List the jobs.
	 *
	 * @return Descriptions of jobs on all machines. Never {@code null}.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract JobDescription[] listJobs() throws TaskException;

	/**
	 * List the machines.
	 *
	 * @return Descriptions of all machines. Never {@code null}.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract Machine[] listMachines() throws TaskException;

	/**
	 * Request notification of job status changes. Best effort only.
	 *
	 * @param jobId
	 *            Job identifier. May be {@code null} to talk about any job.
	 * @param wantNotify
	 *            Whether to enable or disable these notifications.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract void notifyJob(Integer jobId, boolean wantNotify)
			throws TaskException;

	/**
	 * Request notification of machine status changes. Best effort only.
	 *
	 * @param machineName
	 *            Name of the machine. May be {@code null} to talk about any
	 *            machine.
	 * @param wantNotify
	 *            Whether to enable or disable these notifications.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract void notifyMachine(String machineName,
			boolean wantNotify) throws TaskException;

	/**
	 * Switch on or off a job's boards.
	 *
	 * @param jobId
	 *            Job identifier.
	 * @param switchOn
	 *            Whether to switch on.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract void powerJobBoards(int jobId, PowerState switchOn)
			throws TaskException;

	/**
	 * Report a problem with a board, chip or core. If a whole chip has a
	 * problem, {@code p} will be {@code null}. If a whole board has a problem,
	 * {@code x,y} will be {@code null,null}.
	 *
	 * @param address
	 *            The board's IP address.
	 * @param x
	 *            The chip's X coordinate.
	 * @param y
	 *            The chip's Y coordinate.
	 * @param p
	 *            The core's P coordinate.
	 * @param description
	 *            Optional descriptive text about the problem.
	 */
	protected abstract void reportProblem(@IPAddress String address,
			@ValidX Integer x, @ValidY Integer y, @ValidP Integer p,
			String description);

	/**
	 * Get the service version.
	 *
	 * @return The service version. Never {@code null}.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract String version() throws TaskException;

	/**
	 * Describe where a chip is within a job.
	 *
	 * @param jobId
	 *            Job identifier.
	 * @param x
	 *            Chip X coordinate.
	 * @param y
	 *            Chip Y coordinate.
	 * @return Descriptor. Never {@code null}.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract WhereIs whereIsJobChip(int jobId, @ValidX int x,
			@ValidY int y) throws TaskException;

	/**
	 * Describe where a chip is within a machine.
	 *
	 * @param machineName
	 *            Name of the machine.
	 * @param x
	 *            Chip X coordinate.
	 * @param y
	 *            Chip Y coordinate.
	 * @return Descriptor. Never {@code null}.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract WhereIs whereIsMachineChip(@NotBlank String machineName,
			@ValidX int x, @ValidY int y) throws TaskException;

	/**
	 * Describe where a board is within a machine.
	 *
	 * @param machineName
	 *            Name of the machine.
	 * @param x
	 *            Triad X coordinate.
	 * @param y
	 *            Triad Y coordinate.
	 * @param z
	 *            Triad Z coordinate.
	 * @return Descriptor. Never {@code null}.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract WhereIs whereIsMachineLogicalBoard(
			@NotBlank String machineName, @ValidTriadX int x,
			@ValidTriadY int y, @ValidTriadZ int z) throws TaskException;

	/**
	 * Describe where a board is within a machine.
	 *
	 * @param machineName
	 *            Name of the machine.
	 * @param cabinet
	 *            Cabinet number.
	 * @param frame
	 *            Frame number.
	 * @param board
	 *            Board number.
	 * @return Descriptor. Never {@code null}.
	 * @throws TaskException
	 *             If anything goes wrong.
	 */
	protected abstract WhereIs whereIsMachinePhysicalBoard(String machineName,
			@ValidCabinetNumber int cabinet, @ValidFrameNumber int frame,
			@ValidBoardNumber int board) throws TaskException;

	private static Integer optInt(List<?> args) {
		return args.isEmpty() ? null : parseDec(args, 0);
	}

	private static String optStr(List<?> args) {
		return args.isEmpty() ? null : Objects.toString(args.get(0), null);
	}
}

/** Indicates a failure to parse a command. */
final class Oops extends RuntimeException {
	private static final long serialVersionUID = 1L;

	Oops(String msg) {
		super(msg);
	}
}

final class UnknownIOException extends IOException {
	private static final long serialVersionUID = -852489744228393668L;

	UnknownIOException() {
		super("unknown error writing message");
	}
}
