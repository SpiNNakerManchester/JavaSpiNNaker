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
import static java.util.Collections.singleton;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

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

import uk.ac.manchester.spinnaker.alloc.SecurityConfig.Permit;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateDimensions;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateNumBoards;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateBoard;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.SubMachine;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardLink;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.Connection;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.State;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;

/**
 * Implementation of the old style Spalloc interface.
 *
 * @author Donal Fellows
 */
@Component
public class Service {
	private static final int BASE_TEN = 10;

	private static final int ONE_MINUTE = 60;

	private static final double NANOFACTOR = 1e9;

	private static final int LOTS = 10000;

	private static final Duration NOTIFIER_WAIT_TIME =
			Duration.ofSeconds(ONE_MINUTE);

	private static final ThreadGroup GROUP =
			new ThreadGroup("spalloc-legacy-service");

	private static final Logger log = getLogger(Service.class);

	@Value("${spalloc.compat.enable:false}")
	private boolean enable;

	@Value("${spalloc.compat.port:22244}")
	private int port;

	@Value("${spalloc.compat.service-user:}")
	private String serviceUser;

	@Autowired
	private SpallocAPI spalloc;

	@Autowired
	private Epochs epochs;

	private ServerSocket serv;

	private Thread servThread;

	private ObjectMapper mapper;

	private Permit serviceUserPermit;

	private ExecutorService executor;

	private Version version;

	private TaskFactory factory;

	public Service(@Value("${version}") String version) {
		this.version = new Version(version.replaceAll("-.*", ""));
		mapper = JsonMapper.builder().propertyNamingStrategy(SNAKE_CASE)
				.build();
		factory = ServiceImpl::new;
	}

	@PostConstruct
	private void open() throws IOException {
		executor = newCachedThreadPool(r -> new Thread(GROUP, r));
		if (enable) {
			serv = new ServerSocket(port);
			servThread = new Thread(GROUP, this::acceptConnections);
			servThread.setName("service-master");
			servThread.start();
		} else {
			serv = null;
		}
		serviceUserPermit = new Permit(serviceUser);
	}

	@PreDestroy
	private void close() throws IOException, InterruptedException {
		if (serv != null) {
			servThread.interrupt();
			serv.close();
			servThread.join();
			executor.shutdown();
			executor.shutdownNow();
			executor.awaitTermination(2, SECONDS);
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
		while (!interrupted()) {
			try {
				Task service = factory.getTask(serv.accept());
				executor.execute(() -> service.handleConnection());
			} catch (IOException e) {
				log.warn("IO error", e);
			}
		}
	}

	/**
	 * The encoded form of a command to the server.
	 */
	public static class Command {
		private String command;

		private List<Object> args = new ArrayList<>();

		private Map<String, Object> kwargs = new HashMap<>();

		/** @return The name of the command. */
		public String getCommand() {
			return command;
		}

		public void setCommand(String command) {
			this.command = command;
		}

		/** @return The positional arguments to the command. */
		public List<Object> getArgs() {
			return args;
		}

		public void setArgs(List<Object> args) {
			this.args = args;
		}

		/** @return The keyword arguments to the command. */
		public Map<String, Object> getKwargs() {
			return kwargs;
		}

		public void setKwargs(Map<String, Object> kwargs) {
			this.kwargs = kwargs;
		}
	}

	private static class ReturnResponse {
		private String returnValue;

		@JsonProperty("return")
		public String getReturnValue() {
			return returnValue;
		}

		public void setReturnValue(String returnValue) {
			this.returnValue =
					returnValue == null ? "" : returnValue.toString();
		}
	}

	private static class ExceptionResponse {
		private String exception;

		@JsonProperty("exception")
		public String getException() {
			return exception;
		}

		public void setException(String exception) {
			this.exception = exception == null ? "" : exception.toString();
		}
	}

	private static class JobNotifyMessage {
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

	private static class MachineNotifyMessage {
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

	private static int parseDec(Object value) {
		return Integer.parseInt((String) value, BASE_TEN);
	}

	private static double timestamp(Instant i) {
		double ts = i.getEpochSecond();
		ts += i.getNano() / NANOFACTOR;
		return ts;
	}

	/**
	 * The core of tasks that handle connections by clients.
	 *
	 * @author Donal Fellows
	 */
	public abstract static class Task {
		private static final int TASK_BASIC_WAIT_TIMEOUT = 2000;

		/**
		 * The socket that this task is handling.
		 */
		protected final Socket sock;

		private final BufferedReader in;

		private final PrintWriter out;

		private final ObjectMapper mapper;

		/**
		 * @param mapper
		 *            Used to convert between JSON and objects.
		 * @param sock
		 *            The socket that talks to the client.
		 * @throws IOException
		 *             If access to the socket fails.
		 */
		protected Task(ObjectMapper mapper, Socket sock) throws IOException {
			this.sock = sock;
			sock.setTcpNoDelay(true);
			sock.setSoTimeout(TASK_BASIC_WAIT_TIMEOUT);
			this.mapper = mapper;
			in = new BufferedReader(
					new InputStreamReader(sock.getInputStream(), UTF_8));
			out = new PrintWriter(
					new OutputStreamWriter(sock.getOutputStream(), UTF_8));
		}

		final void handleConnection() {
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
				closeNotifiers();
				try {
					sock.close();
				} catch (IOException e) {
					log.error("problem closing socket {}", sock, e);
				}
			}
		}

		void closeNotifiers() {
		}

		public final boolean isClosed() {
			return sock.isClosed();
		}

		public final String host() {
			return sock.getRemoteSocketAddress().toString();
		}

		private Command readMessage() throws IOException, InterruptedException {
			String line = in.readLine();
			if (line == null) {
				if (currentThread().isInterrupted()) {
					throw new InterruptedException();
				}
				return null;
			}
			Command c = mapper.readValue(line, Command.class);
			if (c.command == null) {
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
			ReturnResponse rr = new ReturnResponse();
			// Yes, this is ghastly!
			rr.setReturnValue(mapper.writeValueAsString(response));
			out.println(mapper.writeValueAsString(rr));
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
			ExceptionResponse er = new ExceptionResponse();
			er.setException(exn.toString());
			out.println(mapper.writeValueAsString(er));
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
			if (!jobIds.isEmpty()) {
				JobNotifyMessage jnm = new JobNotifyMessage();
				jnm.setJobsChanged(jobIds);
				out.println(mapper.writeValueAsString(jnm));
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
			if (!machineNames.isEmpty()) {
				MachineNotifyMessage mnm = new MachineNotifyMessage();
				mnm.setMachinesChanged(machineNames);
				out.println(mapper.writeValueAsString(mnm));
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
				if (c == null) {
					return false;
				}
			} catch (SocketTimeoutException e) {
				return !currentThread().isInterrupted();
			} catch (JsonMappingException | JsonParseException e) {
				writeException(e);
				return true;
			}
			Object r = null;
			try {
				switch (c.command) {
				case "create_job":
					r = createJob(c.args, c.kwargs, c);
					break;
				case "destroy_job":
					destroyJob(parseDec(c.args.get(0)),
							(String) c.kwargs.get("reason"));
					break;
				case "get_board_at_position":
					r = getBoardAtPhysicalPosition(
							(String) c.kwargs.get("machine_name"),
							parseDec(c.kwargs.get("x")),
							parseDec(c.kwargs.get("y")),
							parseDec(c.kwargs.get("z")));
					break;
				case "get_board_position":
					r = getBoardAtLogicalPosition(
							(String) c.kwargs.get("machine_name"),
							parseDec(c.kwargs.get("x")),
							parseDec(c.kwargs.get("y")),
							parseDec(c.kwargs.get("z")));
					break;
				case "get_job_machine_info":
					r = getJobMachineInfo(parseDec(c.args.get(0)));
					break;
				case "get_job_state":
					r = getJobState(parseDec(c.args.get(0)));
				case "job_keepalive":
					jobKeepalive(parseDec(c.args.get(0)));
					break;
				case "list_jobs":
					r = listJobs();
					break;
				case "list_machines":
					r = listMachines();
					break;
				case "no_notify_job":
					notifyJob(c.args.isEmpty() ? null : parseDec(c.args.get(0)),
							false);
					break;
				case "no_notify_machine":
					notifyMachine(
							c.args.isEmpty() ? null : (String) c.args.get(0),
							false);
					break;
				case "notify_job":
					notifyJob(c.args.isEmpty() ? null : parseDec(c.args.get(0)),
							true);
					break;
				case "notify_machine":
					notifyMachine(
							c.args.isEmpty() ? null : (String) c.args.get(0),
							true);
					break;
				case "power_off_job_boards":
					powerJobBoards(parseDec(c.args.get(0)), PowerState.OFF);
					break;
				case "power_on_job_boards":
					powerJobBoards(parseDec(c.args.get(0)), PowerState.ON);
					break;
				case "version":
					r = version();
					break;
				case "where_is":
					if (c.kwargs.containsKey("job_id")) {
						r = whereIsJobChip(parseDec(c.kwargs.get("job_id")),
								parseDec(c.kwargs.get("chip_x")),
								parseDec(c.kwargs.get("chip_y")));
					} else if (!c.kwargs.containsKey("machine")) {
						writeException("missing parameter");
						return true;
					} else {
						String m = (String) c.kwargs.get("machine");
						if (c.kwargs.containsKey("chip_x")) {
							r = whereIsMachineChip(m,
									parseDec(c.kwargs.get("chip_x")),
									parseDec(c.kwargs.get("chip_y")));
						} else if (c.kwargs.containsKey("x")) {
							r = whereIsMachineLogicalBoard(m,
									parseDec(c.kwargs.get("x")),
									parseDec(c.kwargs.get("y")),
									parseDec(c.kwargs.get("z")));
						} else if (c.kwargs.containsKey("cabinet")) {
							r = whereIsMachinePhysicalBoard(m,
									parseDec(c.kwargs.get("cabinet")),
									parseDec(c.kwargs.get("frame")),
									parseDec(c.kwargs.get("board")));
						} else {
							writeException("missing parameter");
							return true;
						}
					}
					break;
				default:
					writeException("unknown command: " + c.command);
					return true;
				}
			} catch (Exception e) {
				writeException(e);
				return true;
			}
			ReturnResponse rr = new ReturnResponse();
			rr.setReturnValue(mapper.writeValueAsString(r));
			out.println(mapper.writeValueAsString(rr));
			return true;
		}

		/**
		 * Create a job.
		 *
		 * @param args
		 *            Argument list. Should contain numbers.
		 * @param kwargs
		 *            Keyword argument map.
		 * @param cmd
		 *            The actual command, as a serializable object.
		 * @return Job identifier.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		protected abstract Integer createJob(List<Object> args,
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

	/** Concrete implementation. */
	class ServiceImpl extends Task {
		private static final int TRIAD = 3;

		ServiceImpl(Socket sock) throws IOException {
			super(Service.this.mapper, sock);
		}

		private Map<Integer, Future<Void>> jobNotifiers = new HashMap<>();

		private Map<String, Future<Void>> machNotifiers = new HashMap<>();

		@Override
		void closeNotifiers() {
			for (Future<Void> n : jobNotifiers.values()) {
				n.cancel(true);
			}
			for (Future<Void> n : machNotifiers.values()) {
				n.cancel(true);
			}
		}

		@Override
		protected String version() {
			return version.toString();
		}

		@Override
		protected JobMachineInfo getJobMachineInfo(int jobId) throws Exception {
			Job job = getJob(jobId);
			job.access(host());
			SubMachine machine = job.getMachine()
					.orElseThrow(() -> new Exception("boards not allocated"));
			JobMachineInfo jmi = new JobMachineInfo();
			jmi.setMachineName(machine.getMachine().getName());
			jmi.setBoards(machine.getBoards());
			jmi.setConnections(machine.getConnections().stream().map(ci -> {
				Connection c = new Connection();
				c.setChip(ci.getChip());
				c.setHostname(ci.getHostname());
				return c;
			}).collect(toList()));
			jmi.setWidth(job.getWidth().orElse(0));
			jmi.setHeight(job.getHeight().orElse(0));
			return null;
		}

		private State state(Job job) throws SQLException {
			switch (job.getState()) {
			case QUEUED:
				return State.QUEUED;
			case POWER:
				return State.POWER;
			case READY:
				return State.READY;
			case DESTROYED:
				return State.DESTROYED;
			default:
				return State.UNKNOWN;
			}
		}

		@Override
		protected JobState getJobState(int jobId) throws Exception {
			Job job = getJob(jobId);
			job.access(host());
			JobState js = new JobState();
			js.setKeepalive(timestamp(job.getKeepaliveTimestamp()));
			js.setKeepalivehost(job.getKeepaliveHost().orElse(""));
			js.setPower(job.getMachine().map(m -> {
				try {
					return m.getPower().equals(PowerState.ON);
				} catch (SQLException e) {
					log.warn("problem getting job power state", e);
					return false;
				}
			}).orElse(false));
			js.setReason(job.getReason().orElse(""));
			js.setStartTime(timestamp(job.getStartTime()));
			js.setState(state(job));
			return js;
		}

		@Override
		protected void jobKeepalive(int jobId) throws Exception {
			getJob(jobId).access(host());
		}

		@Override
		protected Integer createJob(List<Object> args,
				Map<String, Object> kwargs, Object cmd) throws Exception {
			SpallocAPI.CreateDescriptor create;
			switch (args.size()) {
			case 0:
				create = new CreateNumBoards(1);
				break;
			case 1:
				create = new CreateNumBoards(parseDec(args.get(0)));
				break;
			case 2:
				create = new CreateDimensions(parseDec(args.get(0)),
						parseDec(args.get(1)));
				break;
			case TRIAD:
				create = CreateBoard.triad(parseDec(args.get(0)),
						parseDec(args.get(1)), parseDec(args.get(2)));
				break;
			default:
				throw new Exception("bad number of args");
			}

			Integer maxDead = (Integer) kwargs.get("max_dead_boards");
			Number keepalive = (Number) kwargs.get("keepalive");
			@SuppressWarnings({
				"unchecked", "rawtypes"
			})
			Job job = spalloc.createJob(serviceUser, create,
					(String) kwargs.get("machine"), (List) kwargs.get("tags"),
					Duration.ofSeconds(keepalive == null ? ONE_MINUTE
							: keepalive.intValue()),
					maxDead, mapper.writeValueAsBytes(cmd));
			return job.getId();
		}

		@Override
		protected void destroyJob(int jobId, String reason) throws Exception {
			getJob(jobId).destroy(reason);
		}

		@Override
		protected BoardCoordinates getBoardAtPhysicalPosition(
				String machineName, int cabinet, int frame, int board)
				throws Exception {
			BoardLocation loc = getMachine(machineName)
					.getBoardByPhysicalCoords(cabinet, frame, board)
					.orElseThrow(() -> new Exception("no such board"));
			return loc.getLogical();
		}

		@Override
		protected BoardPhysicalCoordinates getBoardAtLogicalPosition(
				String machineName, int x, int y, int z) throws Exception {
			BoardLocation loc =
					getMachine(machineName).getBoardByLogicalCoords(x, y, z)
							.orElseThrow(() -> new Exception("no such board"));
			return loc.getPhysical();
		}

		@Override
		protected JobDescription[] listJobs() throws Exception {
			List<JobDescription> jds = new ArrayList<>();
			for (Job job : spalloc.getJobs(false, LOTS, 0).jobs()) {
				JobDescription jd = new JobDescription();
				jd.setJobID(job.getId());
				jd.setKeepAlive(timestamp(job.getKeepaliveTimestamp()));
				jd.setKeepAliveHost(job.getKeepaliveHost().orElse(""));
				jd.setReason(job.getReason().orElse(""));
				jd.setStartTime(timestamp(job.getStartTime()));
				jd.setState(state(job));
				Optional<SubMachine> osm = job.getMachine();
				if (osm.isPresent()) {
					SubMachine sm = osm.get();
					jd.setMachine(sm.getMachine().getName());
					jd.setBoards(sm.getBoards());
					jd.setPower(sm.getPower() == PowerState.ON);
				}
				// TODO Fill out args and kwargs? Not a priority though
				jds.add(jd);
			}
			return jds.toArray(new JobDescription[jds.size()]);
		}

		@Override
		protected uk.ac.manchester.spinnaker.spalloc.messages.Machine[]
				listMachines() throws Exception {
			List<uk.ac.manchester.spinnaker.spalloc.messages.Machine> mds =
					new ArrayList<>();
			for (Machine machine : spalloc.getMachines().values()) {
				uk.ac.manchester.spinnaker.spalloc.messages.Machine m = new //
						uk.ac.manchester.spinnaker.spalloc.messages.Machine();
				m.setName(machine.getName());
				m.setTags(machine.getTags());
				m.setWidth(machine.getWidth());
				m.setHeight(machine.getHeight());
				m.setDeadBoards(machine.getDeadBoards().stream().map(c -> {
					BoardCoordinates bc = new BoardCoordinates();
					bc.setX(c.getX());
					bc.setY(c.getY());
					bc.setZ(c.getZ());
					return bc;
				}).collect(toList()));
				m.setDeadLinks(machine.getDownLinks().stream().flatMap(l -> {
					BoardLink bl1 = new BoardLink();
					bl1.setX(l.end1.board.getX());
					bl1.setY(l.end1.board.getY());
					bl1.setZ(l.end1.board.getZ());
					bl1.setLink(l.end1.direction.ordinal());
					BoardLink bl2 = new BoardLink();
					bl2.setX(l.end2.board.getX());
					bl2.setY(l.end2.board.getY());
					bl2.setZ(l.end2.board.getZ());
					bl2.setLink(l.end2.direction.ordinal());
					return Arrays.asList(bl1, bl2).stream();
				}).collect(toList()));
				mds.add(m);
			}
			return mds.toArray(
					new uk.ac.manchester.spinnaker.spalloc.messages.Machine[mds
							.size()]);
		}

		private <T> void manageNotifier(Map<T, Future<Void>> notifiers, T key,
				boolean wantNotify, Notifier notifier) {
			if (wantNotify) {
				if (!notifiers.containsKey(key)) {
					notifiers.put(key, executor.submit(notifier));
				}
			} else {
				Future<Void> n = notifiers.remove(key);
				if (n != null) {
					n.cancel(true);
				}
			}
		}

		@Override
		protected void notifyJob(Integer jobId, boolean wantNotify)
				throws Exception {
			if (jobId != null) {
				Job job = getJob(jobId);
				job.access(host());
			}
			manageNotifier(jobNotifiers, jobId, wantNotify, () -> {
				spalloc.getJobs(false, LOTS, 0)
						.waitForChange(NOTIFIER_WAIT_TIME);
				List<Integer> actual = spalloc.getJobs(false, LOTS, 0).ids();
				if (jobId != null) {
					actual.retainAll(singleton(jobId));
				}
				writeJobNotification(actual);
			});
		}

		@Override
		protected void notifyMachine(String machine, boolean wantNotify)
				throws Exception {
			if (machine != null) {
				// Validate
				getMachine(machine);
			}
			manageNotifier(machNotifiers, machine, wantNotify, () -> {
				epochs.getMachineEpoch().waitForChange(NOTIFIER_WAIT_TIME);
				List<String> actual =
						new ArrayList<String>(spalloc.getMachines().keySet());
				if (machine != null) {
					actual.retainAll(singleton(machine));
				}
				writeMachineNotification(actual);
			});
		}

		@Override
		protected void powerJobBoards(int jobId, PowerState switchOn)
				throws Exception {
			Job job = getJob(jobId);
			job.access(host());
			job.getMachine().orElseThrow(
					() -> new Exception("no boards currently allocated"))
					.setPower(switchOn);
		}

		@Override
		protected WhereIs whereIsJobChip(int jobId, int x, int y)
				throws Exception {
			Job job = getJob(jobId);
			job.access(host());
			return whereis(job.whereIs(x, y).orElseThrow(
					() -> new Exception("no boards currently allocated")));
		}

		private WhereIs whereis(BoardLocation bl) throws SQLException {
			WhereIs wi = new WhereIs();
			wi.setMachine(bl.getMachine());
			wi.setLogical(bl.getLogical());
			wi.setPhysical(bl.getPhysical());
			wi.setChip(bl.getChip());
			wi.setBoardChip(bl.getBoardChip());
			Job j = bl.getJob();
			if (j != null) {
				wi.setJobId(j.getId());
				wi.setJobChip(bl.getChipRelativeTo(j.getRootChip().get()));
			}
			return wi;
		}

		@Override
		protected WhereIs whereIsMachineChip(String machineName, int x, int y)
				throws Exception {
			return whereis(getMachine(machineName).getBoardByChip(x, y)
					.orElseThrow(() -> new Exception("no such board")));
		}

		@Override
		protected WhereIs whereIsMachineLogicalBoard(String machineName, int x,
				int y, int z) throws Exception {
			return whereis(
					getMachine(machineName).getBoardByLogicalCoords(x, y, z)
							.orElseThrow(() -> new Exception("no such board")));
		}

		@Override
		protected WhereIs whereIsMachinePhysicalBoard(String machineName, int c,
				int f, int b) throws Exception {
			return whereis(
					getMachine(machineName).getBoardByPhysicalCoords(c, f, b)
							.orElseThrow(() -> new Exception("no such board")));
		}

		private Job getJob(int jobId) throws Exception {
			return spalloc.getJob(serviceUserPermit, jobId)
					.orElseThrow(() -> new Exception("no such job"));
		}

		private Machine getMachine(String machineName) throws Exception {
			return spalloc.getMachine(machineName)
					.orElseThrow(() -> new Exception("no such machine"));
		}
	}

	@FunctionalInterface
	interface Notifier extends Callable<Void> {
		@Override
		default Void call() {
			try {
				while (!Thread.interrupted()) {
					waitAndNotify();
				}
			} catch (SQLException e) {
				log.error("SQL failure", e);
			} catch (IOException e) {
				log.warn("failed to notify", e);
			} catch (InterruptedException ignored) {
				// Nothing to do
			}
			return null;
		}

		void waitAndNotify()
				throws InterruptedException, SQLException, IOException;
	}
}
