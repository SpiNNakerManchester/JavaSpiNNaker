/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static java.lang.Integer.parseInt;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.KEEPALIVE_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MACHINE_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_BOARDS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_LINKS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MIN_RATIO_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.PORT_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.REQUIRE_TORUS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.TAGS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.USER_PROPERTY;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocProtocolException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocProtocolTimeoutException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
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
import uk.ac.manchester.spinnaker.spalloc.messages.ListMachinesCommand;
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
public class SpallocClient extends SpallocConnection implements SpallocAPI {
	private static final Logger log = getLogger(SpallocClient.class);

	/** The default communication timeout. (This is no timeout at all.) */
	private static final Integer DEFAULT_TIMEOUT = null;

	private static final Set<String> ALLOWED_KWARGS =
			Set.of(USER_PROPERTY, KEEPALIVE_PROPERTY, MACHINE_PROPERTY,
					TAGS_PROPERTY, MIN_RATIO_PROPERTY, MAX_DEAD_BOARDS_PROPERTY,
					MAX_DEAD_LINKS_PROPERTY, REQUIRE_TORUS_PROPERTY);

	private static final ObjectMapper MAPPER = createMapper();

	/**
	 * Define a new connection using the default spalloc port. <b>NB:</b> Does
	 * not connect to the server until {@link #connect()} is called.
	 *
	 * @param hostname
	 *            The hostname of the server.
	 */
	@MustBeClosed
	public SpallocClient(String hostname) {
		super(hostname, PORT_DEFAULT, DEFAULT_TIMEOUT);
	}

	/**
	 * Define a new connection using the default spalloc port. <b>NB:</b> Does
	 * not connect to the server until {@link #connect()} is called.
	 *
	 * @param hostname
	 *            The hostname of the server.
	 * @param timeout
	 *            The default timeout.
	 */
	@MustBeClosed
	public SpallocClient(String hostname, Integer timeout) {
		super(hostname, PORT_DEFAULT, timeout);
	}

	/**
	 * Define a new connection. <b>NB:</b> Does not connect to the server until
	 * {@link #connect()} is called.
	 *
	 * @param hostname
	 *            The hostname of the server.
	 * @param port
	 *            The port to use.
	 */
	@MustBeClosed
	public SpallocClient(String hostname, int port) {
		super(hostname, port, DEFAULT_TIMEOUT);
	}

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
	public SpallocClient(String hostname, Integer port, Integer timeout) {
		super(hostname, (port == null) ? PORT_DEFAULT : port, timeout);
	}

	/**
	 * Static method to create the object mapper.
	 * <p>
	 * This method makes sure that all JSON unmarshallers use the same Mapper
	 * set up the exact same way.
	 *
	 * @return The Object Mapper used by the Spalloc client,
	 */
	public static ObjectMapper createMapper() {
		var mapper = new ObjectMapper();
		var module = new SimpleModule();
		module.addDeserializer(Response.class, new ResponseDeserializer());
		mapper.registerModule(module);
		mapper.setPropertyNamingStrategy(SNAKE_CASE);
		mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		return mapper;
	}

	@Override
	public Notification waitForNotification(Integer timeout)
			throws SpallocProtocolException, SpallocProtocolTimeoutException,
			InterruptedException {
		// If we already have a notification, return it
		var n = notifications.poll();
		if (n != null) {
			return n;
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
			throws IOException, SpallocServerException, InterruptedException {
		var result = call(new VersionCommand(), timeout);
		if (log.isDebugEnabled()) {
			log.debug("version result: {}", result);
		}
		return new Version(result);
	}

	@Override
	public int createJob(CreateJob builder, Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var result = call(builder.build(), timeout);
		if (log.isDebugEnabled()) {
			log.debug("create result: {}", result);
		}
		return parseInt(result);
	}

	@Deprecated(forRemoval = true) // TODO remove this
	@Override
	public int createJob(List<Integer> args, Map<String, Object> kwargs,
			Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		// If no owner, don't bother with the call
		if (!kwargs.containsKey(USER_PROPERTY)) {
			throw new SpallocServerException(
					USER_PROPERTY + " must be specified for all jobs.");
		}

		// Test for bad kwargs and log them as a problem if present
		var unwanted = new HashSet<>(kwargs.keySet());
		unwanted.removeAll(ALLOWED_KWARGS);
		if (!unwanted.isEmpty()) {
			// Duplicate; original might be unmodifiable
			kwargs = new HashMap<>(kwargs);
			kwargs.keySet().removeAll(unwanted);
			log.warn("removing unsupported keyword arguments ({}) to createJob",
					unwanted);
		}

		var result = call(new CreateJobCommand(args, kwargs), timeout);
		if (log.isDebugEnabled()) {
			log.debug("create result: {}", result);
		}
		return parseInt(result);
	}

	@Override
	public void jobKeepAlive(int jobID, Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var result = call(new JobKeepAliveCommand(jobID), timeout);
		if (log.isDebugEnabled()) {
			log.debug("keepalive result: {}", result);
		}
	}

	@Override
	public JobState getJobState(int jobID, Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var json = call(new GetJobStateCommand(jobID), timeout);
		if (log.isDebugEnabled()) {
			log.debug("get-state result: {}", json);
		}
		return MAPPER.readValue(json, JobState.class);
	}

	@Override
	public JobMachineInfo getJobMachineInfo(int jobID, Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var json = call(new GetJobMachineInfoCommand(jobID), timeout);
		if (log.isDebugEnabled()) {
			log.debug("get-info result: {}", json);
		}
		return MAPPER.readValue(json, JobMachineInfo.class);
	}

	@Override
	public void powerOnJobBoards(int jobID, Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var result = call(new PowerOnJobBoardsCommand(jobID), timeout);
		if (log.isDebugEnabled()) {
			log.debug("power-on result: {}", result);
		}
	}

	@Override
	public void powerOffJobBoards(int jobID, Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var result = call(new PowerOffJobBoardsCommand(jobID), timeout);
		if (log.isDebugEnabled()) {
			log.debug("power-off result: {}", result);
		}
	}

	@Override
	public void destroyJob(int jobID, String reason, Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var result = call(new DestroyJobCommand(jobID, reason), timeout);
		if (log.isDebugEnabled()) {
			log.debug("destroy result: {}", result);
		}
	}

	@Override
	public void notifyJob(Integer jobID, boolean enable, Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		Command<?> c;
		if (enable) {
			if (jobID == null) {
				c = new NotifyJobCommand();
			} else {
				c = new NotifyJobCommand(jobID);
			}
		} else {
			if (jobID == null) {
				c = new NoNotifyJobCommand();
			} else {
				c = new NoNotifyJobCommand(jobID);
			}
		}
		var result = call(c, timeout);
		if (log.isDebugEnabled()) {
			log.debug("notify-job result: {}", result);
		}
	}

	@Override
	public void notifyMachine(String machineName, boolean enable,
			Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		Command<?> c;
		if (enable) {
			if (machineName == null) {
				c = new NotifyMachineCommand();
			} else {
				c = new NotifyMachineCommand(machineName);
			}
		} else {
			if (machineName == null) {
				c = new NoNotifyMachineCommand();
			} else {
				c = new NoNotifyMachineCommand(machineName);
			}
		}
		var result = call(c, timeout);
		if (log.isDebugEnabled()) {
			log.debug("notify-machine result: {}", result);
		}
	}

	@Override
	public List<JobDescription> listJobs(Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var json = call(new ListJobsCommand(), timeout);
		if (log.isDebugEnabled()) {
			log.debug("list-jobs result: {}", json);
		}
		return List.of(MAPPER.readValue(json, JobDescription[].class));
	}

	@Override
	public List<Machine> listMachines(Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var json = call(new ListMachinesCommand(), timeout);
		if (log.isDebugEnabled()) {
			log.debug("list-machines result: {}", json);
		}
		return List.of(MAPPER.readValue(json, Machine[].class));
	}

	@Override
	public BoardPhysicalCoordinates getBoardPosition(String machineName,
			TriadCoords coords, Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var json = call(new GetBoardPositionCommand(machineName, coords),
				timeout);
		if (log.isDebugEnabled()) {
			log.debug("position result: {}", json);
		}
		return MAPPER.readValue(json, BoardPhysicalCoordinates.class);
	}

	@Override
	public BoardCoordinates getBoardPosition(String machineName,
			PhysicalCoords coords, Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var json = call(new GetBoardAtPositionCommand(machineName, coords),
				timeout);
		if (log.isDebugEnabled()) {
			log.debug("position result: {}", json);
		}
		return MAPPER.readValue(json, BoardCoordinates.class);
	}

	@Override
	public WhereIs whereIs(int jobID, HasChipLocation chip, Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var json = call(new WhereIsJobChipCommand(jobID, chip), timeout);
		if (log.isDebugEnabled()) {
			log.debug("where-is result: {}", json);
		}
		return MAPPER.readValue(json, WhereIs.class);
	}

	@Override
	public WhereIs whereIs(String machine, HasChipLocation chip,
			Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var json = call(new WhereIsMachineChipCommand(machine, chip), timeout);
		if (log.isDebugEnabled()) {
			log.debug("where-is result: {}", json);
		}
		return MAPPER.readValue(json, WhereIs.class);
	}

	@Override
	public WhereIs whereIs(String machine, PhysicalCoords coords,
			Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var json = call(new WhereIsMachineBoardPhysicalCommand(machine, coords),
				timeout);
		if (log.isDebugEnabled()) {
			log.debug("where-is result: {}", json);
		}
		return MAPPER.readValue(json, WhereIs.class);
	}

	@Override
	public WhereIs whereIs(String machine, TriadCoords coords, Integer timeout)
			throws IOException, SpallocServerException, InterruptedException {
		var json = call(new WhereIsMachineBoardLogicalCommand(machine, coords),
				timeout);
		if (log.isDebugEnabled()) {
			log.debug("where-is result: {}", json);
		}
		return MAPPER.readValue(json, WhereIs.class);
	}

	private static class ResponseDeserializer
			extends PropertyBasedDeserialiser<Response> {
		// This class should never be serialised
		private static final long serialVersionUID = 1L;

		ResponseDeserializer() {
			super(Response.class);
			register("jobs_changed", JobsChangedNotification.class);
			register("machines_changed", MachinesChangedNotification.class);
			register("return", ReturnResponse.class);
			register("exception", ExceptionResponse.class);
		}
	}

	/**
	 * Format a request as a line of newline-free JSON, suitable for sending to
	 * the spalloc server.
	 */
	@Override
	protected String formatRequest(Command<?> command) throws IOException {
		return MAPPER.writeValueAsString(command);
	}

	/**
	 * Parse a line of response from the spalloc server, which should be a
	 * complete JSON object.
	 */
	@Override
	protected Response parseResponse(String line) throws IOException {
		var r = MAPPER.readValue(line, Response.class);
		if (r == null) {
			throw new SpallocProtocolException("unexpected response: " + line);
		}
		return r;
	}
}
