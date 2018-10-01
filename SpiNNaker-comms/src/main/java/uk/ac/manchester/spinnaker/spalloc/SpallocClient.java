package uk.ac.manchester.spinnaker.spalloc;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.KEEPALIVE_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MACHINE_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_BOARDS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_LINKS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MIN_RATIO_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.REQUIRE_TORUS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.TAGS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.USER_PROPERTY;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
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
	/** The default spalloc port. */
	public static final int DEFAULT_PORT = 22244;
	/** The default communication timeout. (This is no timeout at all.) */
	public static final Integer DEFAULT_TIMEOUT = null;
	private static final Set<String> ALLOWED_KWARGS = new HashSet<>();
	private static final ObjectMapper MAPPER =  createMapper();

	static {
		ALLOWED_KWARGS.addAll(asList(USER_PROPERTY, KEEPALIVE_PROPERTY,
				MACHINE_PROPERTY, TAGS_PROPERTY, MIN_RATIO_PROPERTY,
				MAX_DEAD_BOARDS_PROPERTY, MAX_DEAD_LINKS_PROPERTY,
				REQUIRE_TORUS_PROPERTY));
	}

	/**
	 * Define a new connection using the default spalloc port. <b>NB:</b> Does
	 * not connect to the server until {@link #connect()} is called.
	 *
	 * @param hostname
	 *            The hostname of the server.
	 */
	public SpallocClient(String hostname) {
		super(hostname, DEFAULT_PORT, DEFAULT_TIMEOUT);
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
	public SpallocClient(String hostname, Integer timeout) {
		super(hostname, DEFAULT_PORT, timeout);
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
	public SpallocClient(String hostname, Integer port, Integer timeout) {
        super(hostname, (port == null) ? DEFAULT_PORT : port, timeout);
	}

    /**
     * Static method to create the object mapper.
     *
     * This method makes sure that all json unmarshallers use the same Mapper
     *      set up the exact same way.
     * @return The Object Mapper used by the Spalloc client,
     */
    public static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Response.class, new ResponseBasedDeserializer());
		mapper.registerModule(module);
		mapper.setPropertyNamingStrategy(SNAKE_CASE);
		mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        return mapper;
    }

	@Override
	public Notification waitForNotification(Integer timeout)
			throws SpallocProtocolException, SpallocProtocolTimeoutException {
		// If we already have a notification, return it
		Notification n = notifications.poll();
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
			throws IOException, SpallocServerException {
		String json = call(new VersionCommand(), timeout);
		if (log.isDebugEnabled()) {
			log.debug("version result: {}", json);
		}
		return new Version(json);
	}

	@Override
	public int createJob(List<Integer> args, Map<String, Object> kwargs,
			Integer timeout) throws IOException, SpallocServerException {
		// If no owner, don't bother with the call
		if (!kwargs.containsKey(USER_PROPERTY)) {
			throw new SpallocServerException(
					USER_PROPERTY + " must be specified for all jobs.");
		}

		// Test for bad kwargs and log them as a problem if present
		Set<String> unwanted = new HashSet<>(kwargs.keySet());
		unwanted.removeAll(ALLOWED_KWARGS);
		if (!unwanted.isEmpty()) {
			kwargs.keySet().removeAll(unwanted);
			log.warn("removing unsupported keyword arguments ({}) to createJob",
					unwanted);
		}

		String json = call(new CreateJobCommand(args, kwargs), timeout);
		if (log.isDebugEnabled()) {
			log.debug("create result: {}", json);
		}
		return parseInt(json);
	}

	@Override
	public void jobKeepAlive(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		String json = call(new JobKeepAliveCommand(jobID), timeout);
		if (log.isDebugEnabled()) {
			log.debug("keepalive result: {}", json);
		}
	}

	@Override
	public JobState getJobState(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		String json = call(new GetJobStateCommand(jobID), timeout);
		if (log.isDebugEnabled()) {
			log.debug("get-state result: {}", json);
		}
		return MAPPER.readValue(json, JobState.class);
	}

	@Override
	public JobMachineInfo getJobMachineInfo(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		String json = call(new GetJobMachineInfoCommand(jobID), timeout);
		if (log.isDebugEnabled()) {
			log.debug("get-info result: {}", json);
		}
		return MAPPER.readValue(json, JobMachineInfo.class);
	}

	@Override
	public void powerOnJobBoards(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		String json = call(new PowerOnJobBoardsCommand(jobID), timeout);
		if (log.isDebugEnabled()) {
			log.debug("power-on result: {}", json);
		}
	}

	@Override
	public void powerOffJobBoards(int jobID, Integer timeout)
			throws IOException, SpallocServerException {
		String json = call(new PowerOffJobBoardsCommand(jobID), timeout);
		if (log.isDebugEnabled()) {
			log.debug("power-off result: {}", json);
		}
	}

	@Override
	public void destroyJob(int jobID, String reason, Integer timeout)
			throws IOException, SpallocServerException {
		String json = call(new DestroyJobCommand(jobID, reason), timeout);
		if (log.isDebugEnabled()) {
			log.debug("destroy result: {}", json);
		}
	}

	@Override
	public void notifyJob(Integer jobID, boolean enable, Integer timeout)
			throws IOException, SpallocServerException {
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
		String json = call(c, timeout);
		if (log.isDebugEnabled()) {
			log.debug("notify-job result: {}", json);
		}
	}

	@Override
	public void notifyMachine(String machineName, boolean enable,
			Integer timeout) throws IOException, SpallocServerException {
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
		String json = call(c, timeout);
		if (log.isDebugEnabled()) {
			log.debug("notify-machine result: {}", json);
		}
	}

	/**
	 * Wrap an array into a read-only list.
	 *
	 * @param array
	 *            The array to be wrapped.
	 * @return An unmodifiable list that uses the array for its storage.
	 */
	private static <T> List<T> rolist(T[] array) {
		return unmodifiableList(asList(array));
	}

	@Override
	public List<JobDescription> listJobs(Integer timeout)
			throws IOException, SpallocServerException {
		String json = call(new ListJobsCommand(), timeout);
		if (log.isDebugEnabled()) {
			log.debug("list-jobs result: {}", json);
		}
		return rolist(MAPPER.readValue(json, JobDescription[].class));
	}

	@Override
	public List<Machine> listMachines(Integer timeout)
			throws IOException, SpallocServerException {
		String json = call(new ListMachinesCommand(), timeout);
		if (log.isDebugEnabled()) {
			log.debug("list-machines result: {}", json);
		}
		return rolist(MAPPER.readValue(json, Machine[].class));
	}

	@Override
	public BoardPhysicalCoordinates getBoardPosition(String machineName,
			BoardCoordinates coords, Integer timeout)
			throws IOException, SpallocServerException {
		String json =
				call(new GetBoardPositionCommand(machineName, coords), timeout);
		if (log.isDebugEnabled()) {
			log.debug("position result: {}", json);
		}
		return MAPPER.readValue(json, BoardPhysicalCoordinates.class);
	}

	@Override
	public BoardCoordinates getBoardPosition(String machineName,
			BoardPhysicalCoordinates coords, Integer timeout)
			throws IOException, SpallocServerException {
		String json = call(new GetBoardAtPositionCommand(machineName, coords),
				timeout);
		if (log.isDebugEnabled()) {
			log.debug("position result: {}", json);
		}
		return MAPPER.readValue(json, BoardCoordinates.class);
	}

    @Override
	public WhereIs whereIs(int jobID, HasChipLocation chip, Integer timeout)
			throws IOException, SpallocServerException {
		String json = call(new WhereIsJobChipCommand(jobID, chip), timeout);
		if (log.isDebugEnabled()) {
			log.debug("where-is result: {}", json);
		}
		return MAPPER.readValue(json, WhereIs.class);
	}

	@Override
	public WhereIs whereIs(String machine, HasChipLocation chip,
			Integer timeout) throws IOException, SpallocServerException {
		String json =
				call(new WhereIsMachineChipCommand(machine, chip), timeout);
		if (log.isDebugEnabled()) {
			log.debug("where-is result: {}", json);
		}
		return MAPPER.readValue(json, WhereIs.class);
	}

	@Override
	public WhereIs whereIs(String machine, BoardPhysicalCoordinates coords,
			Integer timeout) throws IOException, SpallocServerException {
		String json =
				call(new WhereIsMachineBoardPhysicalCommand(machine, coords),
						timeout);
		if (log.isDebugEnabled()) {
			log.debug("where-is result: {}", json);
		}
		return MAPPER.readValue(json, WhereIs.class);
	}

	@Override
	public WhereIs whereIs(String machine, BoardCoordinates coords,
			Integer timeout) throws IOException, SpallocServerException {
		String json =
				call(new WhereIsMachineBoardLogicalCommand(machine, coords),
						timeout);
		if (log.isDebugEnabled()) {
			log.debug("where-is result: {}", json);
		}
		return MAPPER.readValue(json, WhereIs.class);
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
		return MAPPER.readValue(line, Response.class);
	}

}
