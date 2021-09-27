/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.compat;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.compat.Utils.parseDec;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.OFF;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.ON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.CompatibilityProperties;
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.TriadCoords;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;

/**
 * Implementation of the old style Spalloc interface.
 *
 * @author Donal Fellows
 */
@Component("spalloc-v1-compatibility-service")
public class V1CompatService {
	/** In seconds. */
	private static final int SHUTDOWN_TIMEOUT = 3;

	private static final int TRIAD = 3;

	private static final ThreadGroup GROUP =
			new ThreadGroup("spalloc-legacy-service");

	private static final Logger log = getLogger(V1CompatService.class);

	/** The overall service properties. */
	@Autowired
	private SpallocProperties mainProps;

	/** Configuration. */
	CompatibilityProperties props;

	/** The core spalloc service. */
	@Autowired
	SpallocAPI spalloc;

	/** The epoch manager. */
	@Autowired
	Epochs epochs;

	/** The database. */
	@Autowired
	DatabaseEngine db;

	/** The service socket. */
	private ServerSocket serv;

	/** The main network service listener thread. */
	private Thread servThread;

	/** How to serialize and deserialize JSON. */
	final ObjectMapper mapper;

	/** How the majority of threads are launched by the service. */
	ExecutorService executor;

	/** The version of the service. */
	final Version version;

	/** How we make connection handlers. */
	private TaskFactory factory = s -> new ServiceImpl(this, s);

	public V1CompatService(@Value("${version}") String version) {
		this.version = new Version(version.replaceAll("-.*", ""));
		mapper = JsonMapper.builder().propertyNamingStrategy(SNAKE_CASE)
				.build();
	}

	@PostConstruct
	private void open() throws IOException {
		props = mainProps.getCompat();
		if (props.getThreadPoolSize() > 0) {
			executor = newFixedThreadPool(props.getThreadPoolSize(),
					r -> new Thread(GROUP, r));
		} else {
			executor = newCachedThreadPool(r -> new Thread(GROUP, r));
		}

		if (props.isEnable()) {
			InetSocketAddress addr =
					new InetSocketAddress(props.getHost(), props.getPort());
			serv = new ServerSocket();
			serv.bind(addr);
			servThread = new Thread(GROUP, this::acceptConnections);
			servThread.setName("service-master");
			log.info("launching listener thread {} on address {}", servThread,
					addr);
			servThread.start();
		}
	}

	@PreDestroy
	private void close() throws IOException, InterruptedException {
		if (nonNull(serv)) {
			log.info("shutting down listener thread {}", servThread);
			// Shut down the server socket first; no new clients
			servThread.interrupt();
			serv.close();
			servThread.join();

			// Shut down the clients
			executor.shutdown();
			executor.shutdownNow();
			executor.awaitTermination(SHUTDOWN_TIMEOUT, SECONDS);
		}
	}

	/** How to make (the core of) tasks to handle clients. */
	@FunctionalInterface
	public interface TaskFactory {
		/**
		 * Make a task.
		 *
		 * @param clientSocket
		 *            The connected socket.
		 * @return The task.
		 * @throws IOException
		 *             If access to the socket fails (unexpected).
		 */
		Task getTask(Socket clientSocket) throws IOException;
	}

	private void acceptConnections() {
		try {
			while (!interrupted()) {
				try {
					Task service = factory.getTask(serv.accept());
					executor.execute(() -> service.handleConnection());
				} catch (SocketException e) {
					if (interrupted()) {
						return;
					}
					if (serv.isClosed()) {
						return;
					}
					log.warn("IO error", e);
				} catch (IOException e) {
					log.warn("IO error", e);
				}
			}
		} finally {
			try {
				serv.close();
			} catch (IOException e) {
				log.warn("IO error", e);
			}
		}
	}

	private static Integer optInt(List<Object> args) {
		return args.isEmpty() ? null : parseDec(args, 0);
	}

	private static String optStr(List<Object> args) {
		return args.isEmpty() ? null : args.get(0).toString();
	}

	/**
	 * The core of tasks that handle connections by clients.
	 *
	 * @author Donal Fellows
	 */
	public abstract class Task {
		/**
		 * How long to wait for a message, in milliseconds. Failure to receive
		 * in this time triggers an exception, but it needs to be fairly
		 * frequent or the thread can't be interrupted.
		 */
		private static final int TASK_BASIC_WAIT_TIMEOUT = 2000;

		/**
		 * The socket that this task is handling.
		 */
		private final Socket sock;

		/**
		 * How to read from the socket. The protocol expects messages to be
		 * UTF-8 lines, with each line being a JSON document.
		 */
		private final BufferedReader in;

		/**
		 * How to write to the socket. The protocol expects messages to be UTF-8
		 * lines, with each line being a JSON document.
		 */
		private final PrintWriter out;

		/**
		 * @param sock
		 *            The socket that talks to the client.
		 * @throws IOException
		 *             If access to the socket fails.
		 */
		protected Task(Socket sock) throws IOException {
			this.sock = sock;
			sock.setTcpNoDelay(true);
			sock.setSoTimeout(TASK_BASIC_WAIT_TIMEOUT);
			in = new BufferedReader(
					new InputStreamReader(sock.getInputStream(), UTF_8));
			out = new PrintWriter(
					new OutputStreamWriter(sock.getOutputStream(), UTF_8));
		}

		final void handleConnection() {
			log.info("waiting for commands from {}", sock);
			try {
				while (!interrupted()) {
					if (!communicate()) {
						break;
					}
				}
			} catch (IOException e) {
				log.error("problem with socket {}", sock, e);
			} catch (InterruptedException e) {
				// ignored
			} finally {
				log.info("closing down connection from {}", sock);
				closeNotifiers();
				try {
					sock.close();
				} catch (IOException e) {
					log.error("problem closing socket {}", sock, e);
				}
			}
		}

		/**
		 * Stop any current running notifiers.
		 */
		protected abstract void closeNotifiers();

		public final String host() {
			return sock.getRemoteSocketAddress().toString();
		}

		private Command readMessage() throws IOException, InterruptedException {
			String line = in.readLine();
			if (isNull(line)) {
				if (currentThread().isInterrupted()) {
					throw new InterruptedException();
				}
				return null;
			}
			Command c = mapper.readValue(line, Command.class);
			if (isNull(c.getCommand())) {
				throw new IOException("message did not specify a command");
			}
			return c;
		}

		/**
		 * Send a response message.
		 *
		 * @param response
		 *            The body object of the response.
		 * @throws IOException
		 *             If network access fails, or the object isn't serializable
		 *             as JSON or a suitable primitive.
		 */
		protected final void writeResponse(Object response) throws IOException {
			if (!sock.isClosed()) {
				ReturnResponse rr = new ReturnResponse();
				// Yes, this is ghastly!
				rr.setReturnValue(mapper.writeValueAsString(response));
				out.println(mapper.writeValueAsString(rr));
				out.flush();
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
		protected final void writeException(Object exn) throws IOException {
			if (!sock.isClosed()) {
				ExceptionResponse er = new ExceptionResponse();
				er.setException(exn.toString());
				out.println(mapper.writeValueAsString(er));
				out.flush();
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
			if (!jobIds.isEmpty() && !sock.isClosed()) {
				JobNotifyMessage jnm = new JobNotifyMessage();
				jnm.setJobsChanged(jobIds);
				out.println(mapper.writeValueAsString(jnm));
				out.flush();
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
		protected final void writeMachineNotification(List<String> machineNames)
				throws IOException {
			if (!machineNames.isEmpty() && !sock.isClosed()) {
				MachineNotifyMessage mnm = new MachineNotifyMessage();
				mnm.setMachinesChanged(machineNames);
				out.println(mapper.writeValueAsString(mnm));
				out.flush();
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
		public boolean communicate() throws IOException, InterruptedException {
			Command c;
			try {
				c = readMessage();
				if (isNull(c)) {
					log.debug("null message");
					return false;
				}
			} catch (SocketTimeoutException e) {
				log.debug("timeout");
				// Message was not read by time timeout expired
				return !currentThread().isInterrupted();
			} catch (JsonMappingException | JsonParseException e) {
				writeException(e);
				return true;
			}

			Object r = null;
			try {
				r = callOperation(c);
			} catch (Exception e) {
				log.debug("responded with {}", e);
				writeException(e);
				return true;
			}

			log.debug("responded with {}", r);
			writeResponse(r);
			return true;
		}

		/**
		 * Decode the command to convert into a method to call.
		 *
		 * @param c
		 *            The command.
		 * @return The result of the command.
		 * @throws Exception
		 *             If things go wrong
		 */
		private Object callOperation(Command c) throws Exception {
			log.debug("calling operation '{}'", c.getCommand());
			switch (c.getCommand()) {
			case "create_job":
				// This is three operations really
				switch (c.getArgs().size()) {
				case 0:
					return createJobNumBoards(1, c.getKwargs(), c);
				case 1:
					return createJobNumBoards(parseDec(c.getArgs(), 0),
							c.getKwargs(), c);
				case 2:
					return createJobRectangle(parseDec(c.getArgs(), 0),
							parseDec(c.getArgs(), 1), c.getKwargs(), c);
				case TRIAD:
					return createJobSpecificBoard(new TriadCoords(
							parseDec(c.getArgs(), 0), parseDec(c.getArgs(), 1),
							parseDec(c.getArgs(), 2)), c.getKwargs(), c);
				default:
					throw new Oops("unsupported number of arguments: "
							+ c.getArgs().size());
				}
			case "destroy_job":
				destroyJob(parseDec(c.getArgs(), 0),
						(String) c.getKwargs().get("reason"));
				break;
			case "get_board_at_position":
				return getBoardAtPhysicalPosition(
						(String) c.getKwargs().get("machine_name"),
						parseDec(c.getKwargs(), "x"),
						parseDec(c.getKwargs(), "y"),
						parseDec(c.getKwargs(), "z"));
			case "get_board_position":
				return getBoardAtLogicalPosition(
						(String) c.getKwargs().get("machine_name"),
						parseDec(c.getKwargs(), "x"),
						parseDec(c.getKwargs(), "y"),
						parseDec(c.getKwargs(), "z"));
			case "get_job_machine_info":
				return getJobMachineInfo(parseDec(c.getArgs(), 0));
			case "get_job_state":
				return getJobState(parseDec(c.getArgs(), 0));
			case "job_keepalive":
				jobKeepalive(parseDec(c.getArgs(), 0));
				break;
			case "list_jobs":
				return listJobs();
			case "list_machines":
				return listMachines();
			case "no_notify_job":
				notifyJob(optInt(c.getArgs()), false);
				break;
			case "no_notify_machine":
				notifyMachine(optStr(c.getArgs()), false);
				break;
			case "notify_job":
				notifyJob(optInt(c.getArgs()), true);
				break;
			case "notify_machine":
				notifyMachine(optStr(c.getArgs()), true);
				break;
			case "power_off_job_boards":
				powerJobBoards(parseDec(c.getArgs(), 0), OFF);
				break;
			case "power_on_job_boards":
				powerJobBoards(parseDec(c.getArgs(), 0), ON);
				break;
			case "version":
				return version();
			case "where_is":
				// This is four operations in a trench coat
				if (c.getKwargs().containsKey("job_id")) {
					return whereIsJobChip(parseDec(c.getKwargs(), "job_id"),
							parseDec(c.getKwargs(), "chip_x"),
							parseDec(c.getKwargs(), "chip_y"));
				} else if (!c.getKwargs().containsKey("machine")) {
					throw new Oops("missing parameter");
				}
				String m = (String) c.getKwargs().get("machine");
				if (c.getKwargs().containsKey("chip_x")) {
					return whereIsMachineChip(m,
							parseDec(c.getKwargs(), "chip_x"),
							parseDec(c.getKwargs(), "chip_y"));
				} else if (c.getKwargs().containsKey("x")) {
					return whereIsMachineLogicalBoard(m,
							parseDec(c.getKwargs(), "x"),
							parseDec(c.getKwargs(), "y"),
							parseDec(c.getKwargs(), "z"));
				} else if (c.getKwargs().containsKey("cabinet")) {
					return whereIsMachinePhysicalBoard(m,
							parseDec(c.getKwargs(), "cabinet"),
							parseDec(c.getKwargs(), "frame"),
							parseDec(c.getKwargs(), "board"));
				} else {
					throw new Oops("missing parameter");
				}
			default:
				throw new Oops("unknown command: " + c.getCommand());
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
		 *            The actual command, as a serializable object.
		 * @return Job identifier.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract Integer createJobNumBoards(int numBoards,
				Map<String, Object> kwargs, Object cmd) throws Exception;

		/**
		 * Create a job asking for a rectangle of boards.
		 *
		 * @param width
		 *            Width of rectangle in boards.
		 * @param height
		 *            Height of rectangle in boards.
		 * @param kwargs
		 *            Keyword argument map.
		 * @param cmd
		 *            The actual command, as a serializable object.
		 * @return Job identifier.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract Integer createJobRectangle(int width, int height,
				Map<String, Object> kwargs, Object cmd) throws Exception;

		/**
		 * Create a job asking for a specific board.
		 *
		 * @param coords
		 *            Which board, by its logical coordinates.
		 * @param kwargs
		 *            Keyword argument map.
		 * @param cmd
		 *            The actual command, as a serializable object.
		 * @return Job identifier.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract Integer createJobSpecificBoard(TriadCoords coords,
				Map<String, Object> kwargs, Object cmd) throws Exception;

		/**
		 * Destroy a job.
		 *
		 * @param jobId
		 *            Job identifier.
		 * @param reason
		 *            Why the machine is being destroyed.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract void destroyJob(int jobId, String reason)
				throws Exception;

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
		 * @return Logical location.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract BoardCoordinates getBoardAtPhysicalPosition(
				String machineName, int cabinet, int frame, int board)
				throws Exception;

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
		 * @return Physical location.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract BoardPhysicalCoordinates getBoardAtLogicalPosition(
				String machineName, int x, int y, int z) throws Exception;

		/**
		 * Get information about the machine allocated to a job.
		 *
		 * @param jobId
		 *            Job identifier.
		 * @return Description of job's (sub)machine.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract JobMachineInfo getJobMachineInfo(int jobId)
				throws Exception;

		/**
		 * Get the state of a job.
		 *
		 * @param jobId
		 *            Job identifier.
		 * @return State description.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract JobState getJobState(int jobId) throws Exception;

		/**
		 * Mark a job as still being kept alive.
		 *
		 * @param jobId
		 *            Job identifier.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract void jobKeepalive(int jobId) throws Exception;

		/**
		 * List the jobs.
		 *
		 * @return Descriptions of jobs on all machines.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract JobDescription[] listJobs() throws Exception;

		/**
		 * List the machines.
		 *
		 * @return Descriptions of all machines.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract uk.ac.manchester.spinnaker.spalloc.messages.Machine[]
				listMachines() throws Exception;

		/**
		 * Request notification of job status changes. Best effort only.
		 *
		 * @param jobId
		 *            Job identifier.
		 * @param wantNotify
		 *            Whether to enable or disable these notifications.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract void notifyJob(Integer jobId, boolean wantNotify)
				throws Exception;

		/**
		 * Request notification of machine status changes. Best effort only.
		 *
		 * @param machineName
		 *            Name of the machine.
		 * @param wantNotify
		 *            Whether to enable or disable these notifications.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract void notifyMachine(String machineName,
				boolean wantNotify) throws Exception;

		/**
		 * Switch on or off a job's boards.
		 *
		 * @param jobId
		 *            Job identifier.
		 * @param switchOn
		 *            Whether to switch on.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract void powerJobBoards(int jobId, PowerState switchOn)
				throws Exception;

		/**
		 * @return The service version.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract String version() throws Exception;

		/**
		 * Describe where a chip is within a job.
		 *
		 * @param jobId
		 *            Job identifier.
		 * @param x
		 *            Chip X coordinate.
		 * @param y
		 *            Chip Y coordinate.
		 * @return Descriptor.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract WhereIs whereIsJobChip(int jobId, int x, int y)
				throws Exception;

		/**
		 * Describe where a chip is within a machine.
		 *
		 * @param machineName
		 *            Name of the machine.
		 * @param x
		 *            Chip X coordinate.
		 * @param y
		 *            Chip Y coordinate.
		 * @return Descriptor.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract WhereIs whereIsMachineChip(String machineName, int x,
				int y) throws Exception;

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
		 * @return Descriptor.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract WhereIs whereIsMachineLogicalBoard(
				String machineName, int x, int y, int z) throws Exception;

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
		 * @return Descriptor.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract WhereIs whereIsMachinePhysicalBoard(
				String machineName, int cabinet, int frame, int board)
				throws Exception;
	}

	/** Indicates a failure to parse a command. */
	private static final class Oops extends RuntimeException {
		private static final long serialVersionUID = 1L;

		Oops(String msg) {
			super(msg);
		}
	}

	private static final class ReturnResponse {
		private String returnValue;

		@JsonProperty("return")
		public String getReturnValue() {
			return returnValue;
		}

		public void setReturnValue(String returnValue) {
			this.returnValue =
					isNull(returnValue) ? "" : returnValue.toString();
		}
	}

	private static final class ExceptionResponse {
		private String exception;

		@JsonProperty("exception")
		public String getException() {
			return exception;
		}

		public void setException(String exception) {
			this.exception = isNull(exception) ? "" : exception.toString();
		}
	}

	private static final class JobNotifyMessage {
		private List<Integer> jobsChanged;

		/**
		 * @return the jobs changed
		 */
		@JsonProperty("jobs_changed")
		public List<Integer> getJobsChanged() {
			return jobsChanged;
		}

		public void setJobsChanged(List<Integer> jobsChanged) {
			this.jobsChanged = jobsChanged;
		}
	}

	private static final class MachineNotifyMessage {
		private List<String> machinesChanged;

		/**
		 * @return the machines changed
		 */
		@JsonProperty("machines_changed")
		public List<String> getMachinesChanged() {
			return machinesChanged;
		}

		public void setMachinesChanged(List<String> machinesChanged) {
			this.machinesChanged = machinesChanged;
		}
	}
}
