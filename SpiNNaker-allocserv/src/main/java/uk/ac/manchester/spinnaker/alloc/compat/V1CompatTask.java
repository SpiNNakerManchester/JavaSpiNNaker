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

import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.compat.Utils.parseDec;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.OFF;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.ON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.TriadCoords;
import uk.ac.manchester.spinnaker.alloc.compat.V1CompatService.ExceptionResponse;
import uk.ac.manchester.spinnaker.alloc.compat.V1CompatService.JobNotifyMessage;
import uk.ac.manchester.spinnaker.alloc.compat.V1CompatService.MachineNotifyMessage;
import uk.ac.manchester.spinnaker.alloc.compat.V1CompatService.Oops;
import uk.ac.manchester.spinnaker.alloc.compat.V1CompatService.ReturnResponse;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;

/**
 * The core of tasks that handle connections by clients.
 *
 * @author Donal Fellows
 */
public abstract class V1CompatTask {
	private static final Logger log = getLogger(V1CompatTask.class);

	/**
	 * How long to wait for a message, in milliseconds. Failure to receive in
	 * this time triggers an exception, but it needs to be fairly frequent or
	 * the thread can't be interrupted.
	 */
	private static final int TASK_BASIC_WAIT_TIMEOUT = 2000;

	private static final int TRIAD = 3;

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
	 */
	private final PrintWriter out;

	private final ExecutorService executor;

	private final ObjectMapper mapper;

	/**
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
		this.executor = srv.executor;
		this.mapper = srv.mapper;
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

	/**
	 * @return The executor to use.
	 */
	protected final ExecutorService getExecutor() {
		return executor;
	}

	/**
	 * @return The JSON mapper to use if necessary.
	 */
	protected final ObjectMapper getJsonMapper() {
		return mapper;
	}

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
		Command c = getJsonMapper().readValue(line, Command.class);
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
	 *             If network access fails, or the object isn't serializable as
	 *             JSON or a suitable primitive.
	 */
	protected final void writeResponse(Object response) throws IOException {
		if (!sock.isClosed()) {
			ReturnResponse rr = new ReturnResponse();
			rr.setReturnValue(response);
			out.println(getJsonMapper().writeValueAsString(rr));
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
			out.println(getJsonMapper().writeValueAsString(er));
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
			out.println(getJsonMapper().writeValueAsString(jnm));
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
			out.println(getJsonMapper().writeValueAsString(mnm));
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
	public final boolean communicate()
			throws IOException, InterruptedException {
		Command cmd;
		try {
			cmd = readMessage();
			if (isNull(cmd) || isNull(cmd.getCommand())) {
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
			r = callOperation(cmd);
		} catch (V1TaskImpl.TaskException e) {
			// Expected exception; don't log
			writeException(e);
			return true;
		} catch (Exception e) {
			log.warn("unexpected exception from {} operation", cmd.getCommand(),
					e);
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
	 * @param cmd
	 *            The command.
	 * @return The result of the command.
	 * @throws Exception
	 *             If things go wrong
	 */
	private Object callOperation(Command cmd) throws Exception {
		log.debug("calling operation '{}'", cmd.getCommand());
		List<Object> args = cmd.getArgs();
		Map<String, Object> kwargs = cmd.getKwargs();
		switch (cmd.getCommand()) {
		case "create_job":
			// This is three operations really, and an optional parameter.
			switch (args.size()) {
			case 0:
				return createJobNumBoards(1, kwargs, cmd);
			case 1:
				return createJobNumBoards(parseDec(args, 0), kwargs, cmd);
			case 2:
				return createJobRectangle(parseDec(args, 0), parseDec(args, 1),
						kwargs, cmd);
			case TRIAD:
				return createJobSpecificBoard(new TriadCoords(parseDec(args, 0),
						parseDec(args, 1), parseDec(args, 2)), kwargs, cmd);
			default:
				throw new Oops(
						"unsupported number of arguments: " + args.size());
			}
		case "destroy_job":
			destroyJob(parseDec(args, 0), (String) kwargs.get("reason"));
			break;
		case "get_board_at_position":
			return getBoardAtPhysicalPosition(
					(String) kwargs.get("machine_name"), parseDec(kwargs, "x"),
					parseDec(kwargs, "y"), parseDec(kwargs, "z"));
		case "get_board_position":
			return getBoardAtLogicalPosition(
					(String) kwargs.get("machine_name"), parseDec(kwargs, "x"),
					parseDec(kwargs, "y"), parseDec(kwargs, "z"));
		case "get_job_machine_info":
			return getJobMachineInfo(parseDec(args, 0));
		case "get_job_state":
			return getJobState(parseDec(args, 0));
		case "job_keepalive":
			jobKeepalive(parseDec(args, 0));
			break;
		case "list_jobs":
			return listJobs();
		case "list_machines":
			return listMachines();
		case "no_notify_job":
			notifyJob(V1CompatService.optInt(args), false);
			break;
		case "no_notify_machine":
			notifyMachine(V1CompatService.optStr(args), false);
			break;
		case "notify_job":
			notifyJob(V1CompatService.optInt(args), true);
			break;
		case "notify_machine":
			notifyMachine(V1CompatService.optStr(args), true);
			break;
		case "power_off_job_boards":
			powerJobBoards(parseDec(args, 0), OFF);
			break;
		case "power_on_job_boards":
			powerJobBoards(parseDec(args, 0), ON);
			break;
		case "version":
			return version();
		case "where_is":
			// This is four operations in a trench coat
			if (kwargs.containsKey("job_id")) {
				return whereIsJobChip(parseDec(kwargs, "job_id"),
						parseDec(kwargs, "chip_x"), parseDec(kwargs, "chip_y"));
			} else if (!kwargs.containsKey("machine")) {
				throw new Oops("missing parameter");
			}
			String m = (String) kwargs.get("machine");
			if (kwargs.containsKey("chip_x")) {
				return whereIsMachineChip(m, parseDec(kwargs, "chip_x"),
						parseDec(kwargs, "chip_y"));
			} else if (kwargs.containsKey("x")) {
				return whereIsMachineLogicalBoard(m, parseDec(kwargs, "x"),
						parseDec(kwargs, "y"), parseDec(kwargs, "z"));
			} else if (kwargs.containsKey("cabinet")) {
				return whereIsMachinePhysicalBoard(m,
						parseDec(kwargs, "cabinet"), parseDec(kwargs, "frame"),
						parseDec(kwargs, "board"));
			} else {
				throw new Oops("missing parameter");
			}
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
	protected abstract WhereIs whereIsMachineLogicalBoard(String machineName,
			int x, int y, int z) throws Exception;

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
	protected abstract WhereIs whereIsMachinePhysicalBoard(String machineName,
			int cabinet, int frame, int board) throws Exception;
}
