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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.RECONNECT_DELAY_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.Utils.makeTimeout;
import static uk.ac.manchester.spinnaker.spalloc.Utils.timeLeft;
import static uk.ac.manchester.spinnaker.spalloc.Utils.timedOut;
import static uk.ac.manchester.spinnaker.spalloc.messages.State.QUEUED;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.spalloc.exceptions.JobDestroyedException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocProtocolTimeoutException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocStateChangeTimeoutException;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.Connection;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.State;
import uk.ac.manchester.spinnaker.utils.Daemon;

/**
 * A high-level interface for requesting and managing allocations of SpiNNaker
 * boards.
 * <p>
 * Constructing a {@link SpallocJob} object connects to a <a href=
 * "https://github.com/SpiNNakerManchester/spalloc_server">spalloc-server</a>
 * and requests a number of SpiNNaker boards. The job object may then be used to
 * monitor the state of the request, control the boards allocated and determine
 * their IP addresses.
 * <p>
 * In its simplest form, a {@link SpallocJob} can be used as a context manager
 * like so:
 *
 * <pre>
 * try (var j = new SpallocJob(new CreateJob(6).owner(me))) {
 *     myApplication.boot(j.getHostname(), j.getDimensions());
 *     myApplication.run(j.getHostname());
 * }
 * </pre>
 *
 * In this example a six-board machine is requested and the
 * {@code try}-with-resources context is entered once the allocation has been
 * made and the allocated boards are fully powered on. When control leaves the
 * block, the job is destroyed and the boards shut down by the server ready for
 * another job.
 * <p>
 * For more fine-grained control, the same functionality is available via
 * various methods:
 *
 * <pre>
 * var j = new SpallocJob(new CreateJob(6).owner(me)));
 * j.waitUntilReady();
 * myApplication.boot(j.getHostname(), j.getDimensions());
 * myApplication.run(j.getHostname());
 * j.destroy();
 * </pre>
 *
 * <b>Note:</b> <blockquote class="note"> More complex applications may wish to
 * log the following properties of their job to support later debugging efforts:
 * <ul>
 * <li>{@code ID} &mdash; May be used to query the state of the job and find out
 * its fate if cancelled or destroyed. The <i>spalloc-job</i> command can be
 * used to discover the state/fate of the job and <i>spalloc-where-is</i> may be
 * used to find out what boards problem chips reside on.
 * <li>{@code machineName} and {@code boards} together give a complete record of
 * the hardware used by the job. The <i>spalloc-where-is</i> command may be used
 * to find out the physical locations of the boards used.
 * </ul>
 * </blockquote>
 *
 * @see CreateJob {@code CreateJob}: How to describe the job to create.
 */
public class SpallocJob implements AutoCloseable, SpallocJobAPI {
	private static final Logger log = getLogger(SpallocJob.class);

	/** Minimum supported server version. */
	private static final Version MIN_VER = new Version(0, 4, 0);

	/** Maximum supported server version. */
	private static final Version MAX_VER = new Version(2, 0, 0);

	private static final int STATUS_CACHE_PERIOD = 500;

	private SpallocClient client;

	private int id;

	private Integer timeout;

	private Integer keepaliveTime;

	/** The keepalive thread. */
	private Thread keepalive;

	/** Used to signal that the keepalive thread should stop. */
	private volatile boolean stopping;

	/**
	 * Cache of information about a job's state. This information can change,
	 * but not usually extremely rapidly; it has a caching period, implemented
	 * using the {@link #statusTimestamp} field.
	 */
	private JobState statusCache;

	/**
	 * The time when the information in {@link #statusCache} was last collected.
	 */
	private long statusTimestamp;

	/**
	 * Cache of information about a machine. This is information which doesn't
	 * change once it is assigned, so there is no expiry mechanism.
	 */
	private JobMachineInfo machine;

	/** The status cache period, in ms. Non-constant for tests. */
	int statusCachePeriod = STATUS_CACHE_PERIOD;

	private int reconnectDelay = f2ms(RECONNECT_DELAY_DEFAULT);

	private static final ThreadGroup SPALLOC_WORKERS =
			new ThreadGroup("spalloc worker threads");

	private static Configuration config;

	/**
	 * Set up where configuration settings come from. By default, this is from a
	 * file called {@value #DEFAULT_CONFIGURATION_SOURCE}; this method allows
	 * you to override that (e.g., for testing).
	 *
	 * @param filename
	 *            the base filename (without a path) to load the configuration
	 *            from. This is expected to be a {@code .ini} file.
	 * @see Configuration
	 */
	public static void setConfigurationSource(String filename) {
		config = new Configuration(filename);
	}

	/**
	 * The name of the default file to load the configuration from.
	 */
	public static final String DEFAULT_CONFIGURATION_SOURCE = "spalloc.ini";

	static {
		setConfigurationSource(DEFAULT_CONFIGURATION_SOURCE);
	}

	/**
	 * Create a spalloc job that requests a SpiNNaker machine.
	 *
	 * @param hostname
	 *            The spalloc server host
	 * @param timeout
	 *            The communications timeout
	 * @param builder
	 *            The job-creation request builder.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	@MustBeClosed
	public SpallocJob(String hostname, Integer timeout, CreateJob builder)
			throws IOException, SpallocServerException, InterruptedException {
		this(hostname, config.getPort(), timeout, builder);
	}

	/**
	 * Create a spalloc job that requests a SpiNNaker machine.
	 *
	 * @param hostname
	 *            The spalloc server host
	 * @param builder
	 *            The job-creation request builder.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	@MustBeClosed
	public SpallocJob(String hostname, CreateJob builder)
			throws IOException, SpallocServerException, InterruptedException {
		this(hostname, config.getPort(), f2ms(config.getTimeout()), builder);
	}

	/**
	 * Create a spalloc job that requests a SpiNNaker machine.
	 *
	 * @param builder
	 *            The job-creation request builder.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	@MustBeClosed
	public SpallocJob(CreateJob builder)
			throws IOException, SpallocServerException, InterruptedException {
		this(config.getHost(), config.getPort(), f2ms(config.getTimeout()),
				builder);
	}

	/**
	 * Create a spalloc job that requests a SpiNNaker machine.
	 *
	 * @param client
	 *            The spalloc client
	 * @param builder
	 *            The job-creation request builder.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws IllegalArgumentException
	 *             If a bad builder is given.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	public SpallocJob(SpallocClient client, CreateJob builder)
			throws IOException, SpallocServerException, InterruptedException {
		this(client, f2ms(config.getTimeout()), builder);
	}

	private static void validateBuilder(CreateJob builder) {
		if (requireNonNull(builder, "a builder must be specified")
				.isTargetDefined()) {
			return;
		}
		var machine = config.getMachine();
		var tags = config.getTags();
		if (nonNull(machine)) {
			builder.machine(machine);
		} else if (nonNull(tags)) {
			builder.tags(tags);
		} else {
			throw new IllegalArgumentException(
					"must have either machine or tags specified or able "
							+ "to be looked up from the configuration");
		}
	}

	/**
	 * Create a spalloc job that requests a SpiNNaker machine.
	 *
	 * @param client
	 *            The spalloc client
	 * @param timeout
	 *            The communications timeout
	 * @param builder
	 *            The job-creation request builder.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	public SpallocJob(SpallocClient client, Integer timeout, CreateJob builder)
			throws IOException, SpallocServerException, InterruptedException {
		validateBuilder(builder);
		this.client = client;
		this.timeout = timeout;
		client.connect();
		reconnectDelay = f2ms(config.getReconnectDelay());
		id = client.createJob(builder, timeout);
		/*
		 * We also need the keepalive configuration so we know when to send
		 * keepalive messages.
		 */
		keepaliveTime = f2ms(builder.getKeepAlive());
		log.info("created spalloc job with ID: {}", id);
		launchKeepaliveDaemon();
	}

	/**
	 * Create a spalloc job that requests a SpiNNaker machine.
	 *
	 * @param hostname
	 *            The spalloc server host
	 * @param port
	 *            The spalloc server port
	 * @param timeout
	 *            The communications timeout
	 * @param builder
	 *            The job-creation request builder.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 * @throws IllegalArgumentException
	 *             If a bad builder is given.
	 */
	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	public SpallocJob(String hostname, Integer port, Integer timeout,
			CreateJob builder)
			throws IOException, SpallocServerException, InterruptedException {
		validateBuilder(builder);
		this.client = new SpallocClient(hostname, port, timeout);
		this.timeout = timeout;
		client.connect();
		reconnectDelay = f2ms(config.getReconnectDelay());
		id = client.createJob(builder, timeout);
		/*
		 * We also need the keepalive configuration so we know when to send
		 * keepalive messages.
		 */
		keepaliveTime = f2ms(builder.getKeepAlive());
		log.info("created spalloc job with ID: {}", id);
		launchKeepaliveDaemon();
	}

	/**
	 * Create a job client that resumes an existing job given its ID.
	 *
	 * @param id
	 *            The job ID
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws JobDestroyedException
	 *             If the job doesn't exist (any more).
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	@MustBeClosed
	public SpallocJob(int id) throws IOException, SpallocServerException,
			JobDestroyedException, InterruptedException {
		this(config.getHost(), config.getPort(), f2ms(config.getTimeout()), id);
	}

	/**
	 * Create a job client that resumes an existing job given its ID.
	 *
	 * @param hostname
	 *            The spalloc server host
	 * @param id
	 *            The job ID
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws JobDestroyedException
	 *             If the job doesn't exist (any more).
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	@MustBeClosed
	public SpallocJob(String hostname, int id)
			throws IOException, SpallocServerException, JobDestroyedException,
			InterruptedException {
		this(hostname, config.getPort(), f2ms(config.getTimeout()), id);
	}

	/**
	 * Create a job client that resumes an existing job given its ID.
	 *
	 * @param hostname
	 *            The spalloc server host
	 * @param timeout
	 *            The communications timeout
	 * @param id
	 *            The job ID
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws JobDestroyedException
	 *             If the job doesn't exist (any more).
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	@MustBeClosed
	public SpallocJob(String hostname, Integer timeout, int id)
			throws IOException, SpallocServerException, JobDestroyedException,
			InterruptedException {
		this(hostname, config.getPort(), timeout, id);
	}

	/**
	 * Converts a "float" number of seconds to milliseconds.
	 *
	 * @param obj
	 *            The number of seconds as a {@link Number} but up-casted to an
	 *            object for convenience.
	 * @return The number of milliseconds, suitable for use with Java timing
	 *         operations.
	 */
	private static Integer f2ms(Object obj) {
		if (obj == null) {
			return null;
		}
		return (int) (((Number) obj).doubleValue() * MSEC_PER_SEC);
	}

	/**
	 * Create a job client that resumes an existing job given its ID.
	 *
	 * @param hostname
	 *            The spalloc server host
	 * @param port
	 *            The TCP port
	 * @param timeout
	 *            The communications timeout
	 * @param id
	 *            The job ID
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws JobDestroyedException
	 *             If the job doesn't exist (any more).
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	public SpallocJob(String hostname, int port, Integer timeout, int id)
			throws IOException, SpallocServerException, JobDestroyedException,
			InterruptedException {
		client = new SpallocClient(hostname, port, timeout);
		this.timeout = timeout;
		this.id = id;
		client.connect();
		reconnectDelay = f2ms(config.getReconnectDelay());
		/*
		 * If the job no longer exists, we can't get the keepalive interval (and
		 * there's nothing to keepalive) so just bail out.
		 */
		var jobState = getStatus();
		switch (jobState.getState()) {
		case DESTROYED:
			if (nonNull(jobState.getReason())) {
				throw new JobDestroyedException(format(
						"SpallocJob %d does not exist: %s: %s", id,
						jobState.getState().name(), jobState.getReason()));
			}
			// fall through
		case UNKNOWN:
			throw new JobDestroyedException(
					format("SpallocJob %d does not exist: %s", id,
							jobState.getState().name()));
		default:
			// do nothing
		}
		// Snag the keepalive interval from the job
		keepaliveTime = f2ms(jobState.getKeepalive());
		log.info("resumed spalloc job with ID: {}", id);
		launchKeepaliveDaemon();
	}

	private void launchKeepaliveDaemon() {
		log.info("launching keepalive thread for {} with interval {}ms", id,
				keepaliveTime / 2);
		if (keepalive != null) {
			log.warn("launching second keepalive thread for {}", id);
		}
		stopping = false;
		keepalive = new Daemon(SPALLOC_WORKERS, this::keepalive,
				"keepalive for spalloc job " + id);
		keepalive.setUncaughtExceptionHandler((th, e) -> {
			log.warn("unexpected exception in {}", th, e);
		});
		keepalive.start();
	}

	private void keepalive() {
		try {
			while (!stopping) {
				client.jobKeepAlive(id, timeout);
				if (!interrupted()) {
					sleep(keepaliveTime / 2);
				}
			}
		} catch (IOException | SpallocServerException e) {
			log.debug("exception in keepalive; terminating", e);
		} catch (InterruptedException e) {
			log.trace("interrupted in keepalive", e);
		}
	}

	@Override
	public void close() throws IOException {
		stopping = true;
		keepalive.interrupt();
		client.close();
	}

	/**
	 * Reconnect to the server and check version.
	 * <p>
	 * If reconnection fails, the error is reported as a warning but no
	 * exception is raised.
	 *
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	private void reconnect() throws InterruptedException {
		try {
			client.connect(timeout);
			assertCompatibleVersion();
			log.info("Reconnected to spalloc server successfully");
		} catch (SpallocServerException | IOException e) {
			/*
			 * Connect/version command failed... Leave the socket clearly broken
			 * so that we retry again
			 */
			log.warn("Spalloc server is unreachable ({}), will keep trying...",
					e.getMessage());
			try {
				client.close();
			} catch (IOException inner) {
				// close failed?! Nothing we can do but log and try later
				log.error("problem closing connection", inner);
			}
		}
	}

	/**
	 * Assert that the server version is compatible. This client supports from
	 * 0.4.0 to 2.0.0 (but not including the latter).
	 *
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 * @throws IllegalStateException
	 *             If the server is not compatible with this client.
	 */
	protected void assertCompatibleVersion()
			throws IOException, SpallocServerException, InterruptedException {
		var v = client.version(timeout);
		if (MIN_VER.compareTo(v) <= 0 && MAX_VER.compareTo(v) > 0) {
			return;
		}
		client.close();
		throw new IllegalStateException(
				"Server version " + v + " is not compatible with this client");
	}

	@Override
	public void destroy(String reason)
			throws IOException, SpallocServerException, InterruptedException {
		try {
			stopping = true; // Don't need a keepalive any more
			client.destroyJob(id, reason, timeout);
		} finally {
			close();
		}
		purgeStatus();
	}

	@Override
	public void setPower(Boolean powerOn)
			throws IOException, SpallocServerException, InterruptedException {
		if (powerOn == null) {
			return;
		}
		if (powerOn) {
			client.powerOnJobBoards(id, timeout);
		} else {
			client.powerOffJobBoards(id, timeout);
		}
		purgeStatus();
	}

	@Override
	public int getID() {
		return id;
	}

	private JobState getStatus()
			throws IOException, SpallocServerException, InterruptedException {
		if (statusCache == null || statusTimestamp < currentTimeMillis()
				- statusCachePeriod) {
			statusCache = client.getJobState(id, timeout);
			statusTimestamp = currentTimeMillis();
		}
		return statusCache;
	}

	private void purgeStatus() {
		statusCache = null;
	}

	@Override
	public State getState()
			throws IOException, SpallocServerException, InterruptedException {
		return getStatus().getState();
	}

	@Override
	public Boolean getPower()
			throws IOException, SpallocServerException, InterruptedException {
		return getStatus().getPower();
	}

	@Override
	public String getDestroyReason()
			throws IOException, SpallocServerException, InterruptedException {
		return getStatus().getReason();
	}

	private void retrieveMachineInfo()
			throws IOException, SpallocServerException, IllegalStateException,
			InterruptedException {
		/*
		 * Check the job is still not QUEUED as then machine info is all nulls
		 * getJobMachineInfo works if the Job is in State.POWER
		 */
		switch (getState()) {
		case DESTROYED:
			// Nothing to do; the job's dead
			return;
		case UNKNOWN: // Shouldn't be possible, but recheck...
		case QUEUED:
			// Double check very latest state.
			purgeStatus();
			if (getState() == QUEUED) {
				throw new IllegalStateException(
						"Job not Ready. Call waitUntilReady first.");
			}
			// fall through
		default:
			machine = client.getJobMachineInfo(id, timeout);
		}

	}

	private boolean isMachineInfoInvalid() {
		return machine == null || machine.getWidth() == 0;
	}

	@Override
	public List<Connection> getConnections()
			throws IOException, SpallocServerException, InterruptedException {
		if (isMachineInfoInvalid()) {
			retrieveMachineInfo();
		}
		if (isMachineInfoInvalid()) {
			return null;
		}
		return machine.getConnections();
	}

	@Override
	public String getHostname()
			throws IOException, SpallocServerException, InterruptedException {
		for (Connection c : getConnections()) {
			if (c.getChip().onSameChipAs(ZERO_ZERO)) {
				return c.getHostname();
			}
		}
		return null;
	}

	@Override
	public MachineDimensions getDimensions()
			throws IOException, SpallocServerException,
			InterruptedException {
		if (isMachineInfoInvalid()) {
			retrieveMachineInfo();
		}
		if (isMachineInfoInvalid()) {
			return null;
		}
		return new MachineDimensions(machine.getWidth(), machine.getHeight());
	}

	@Override
	public String getMachineName() throws IOException, SpallocServerException,
			InterruptedException {
		if (isMachineInfoInvalid()) {
			retrieveMachineInfo();
		}
		if (isMachineInfoInvalid()) {
			return null;
		}
		return machine.getMachineName();
	}

	@Override
	public List<BoardCoordinates> getBoards()
			throws IOException, SpallocServerException, IllegalStateException,
			InterruptedException {
		if (isMachineInfoInvalid()) {
			retrieveMachineInfo();
		}
		if (isMachineInfoInvalid()) {
			return null;
		}
		return machine.getBoards();
	}

	@Override
	public State waitForStateChange(State oldState, Integer timeout)
			throws SpallocServerException, InterruptedException {
		var finishTime = makeTimeout(timeout);

		// We may get disconnected while waiting so keep listening...
		while (!timedOut(finishTime)) {
			try {
				// Watch for changes in this SpallocJob's state
				client.notifyJob(id, true);

				// Wait for job state to change
				while (!timedOut(finishTime)) {
					// Has the job changed state?
					purgeStatus();
					var newState = getState();
					if (newState != oldState) {
						return newState;
					}

					// Wait for a state change and keep the job alive
					if (!doWaitForAChange(finishTime)) {
						/*
						 * The user's timeout expired while waiting for a state
						 * change, return the old state and give up.
						 */
						return oldState;
					}
				}
			} catch (IOException e) {
				/*
				 * Something went wrong while communicating with the server,
				 * reconnect after the reconnection delay (or timeout, whichever
				 * came first).
				 */
				try {
					doReconnect(finishTime);
				} catch (IOException e1) {
					log.error("problem when reconnecting after disconnect", e1);
				}
			}
		}

		/*
		 * If we get here, the timeout expired without a state change, just
		 * return the old state
		 */
		return oldState;
	}

	/**
	 * Wait for a state change and keep the job alive.
	 *
	 * @param finishTime
	 *            when our timeout expires, or {@code null} for never
	 * @return True if the state has changed, or false on timeout
	 * @throws SpallocServerException
	 *             If the server throws an exception.
	 * @throws IOException
	 *             If communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	private boolean doWaitForAChange(Long finishTime)
			throws IOException, SpallocServerException, InterruptedException {
		/*
		 * Since we're about to block holding the client lock, we must be
		 * responsible for keeping everything alive.
		 */
		while (!timedOut(finishTime)) {
			client.jobKeepAlive(id, timeout);

			// Wait for the job to change
			try {
				/*
				 * Block waiting for the job to change no-longer than the
				 * user-specified timeout or half the keepalive interval.
				 */
				Integer waitTimeout;
				if (finishTime != null && keepaliveTime != null) {
					waitTimeout = min(keepaliveTime / 2, timeLeft(finishTime));
				} else if (finishTime == null) {
					waitTimeout =
							(keepalive == null) ? null : keepaliveTime / 2;
				} else {
					waitTimeout = timeLeft(finishTime);
				}
				if (waitTimeout == null || waitTimeout >= 0) {
					client.waitForNotification(waitTimeout);
					return true;
				}
			} catch (SpallocProtocolTimeoutException e) {
				/*
				 * Its been a while, send a keep-alive since we're still holding
				 * the lock
				 */
			}
		}
		// The user's timeout expired while waiting for a state change
		return false;
	}

	/**
	 * Reconnect after the reconnection delay (or timeout, whichever came
	 * first).
	 *
	 * @throws IOException
	 *             If communications fail.
	 * @throws InterruptedException
	 *             If the wait is interrupted.
	 */
	private void doReconnect(Long finishTime)
			throws IOException, InterruptedException {
		client.close();
		int delay;
		if (finishTime != null) {
			delay = min(timeLeft(finishTime), reconnectDelay);
		} else {
			delay = reconnectDelay;
		}
		sleep(max(0, delay));
		reconnect();
	}

	@Override
	public void waitUntilReady(Integer timeout)
			throws JobDestroyedException, IOException, SpallocServerException,
			SpallocStateChangeTimeoutException, InterruptedException {
		State curState = null;
		var finishTime = makeTimeout(timeout);
		while (!timedOut(finishTime)) {
			if (curState == null) {
				/*
				 * Get initial state (NB: done here such that the command is
				 * never sent if the timeout has already occurred)
				 */
				curState = getState();
			}

			// Are we ready yet?
			switch (curState) {
			case READY:
				log.info("job:{} is now ready", id);
				// Now in the ready state!
				return;
			case QUEUED:
				log.info("job:{} has been queued by the spalloc server", id);
				break;
			case POWER:
				log.info("waiting for board power commands to "
						+ "complete for job:{}", id);
				break;
			case DESTROYED:
				// In a state which can never become ready
				throw new JobDestroyedException(getDestroyReason());
			default: // UNKNOWN
				// Server has forgotten what this job even was...
				throw new JobDestroyedException(
						"Spalloc server no longer recognises job:" + id);
			}
			// Wait for a state change...
			curState = waitForStateChange(curState, timeLeft(finishTime));
		}
		// Timed out!
		throw new SpallocStateChangeTimeoutException();
	}

	@Override
	public BoardPhysicalCoordinates whereIs(HasChipLocation chip)
			throws IOException, SpallocServerException, InterruptedException {
		var result = client.whereIs(id, chip, timeout);
		if (result == null) {
			throw new IllegalStateException(
					"received null instead of machine location");
		}
		return result.getPhysical();
	}

	/**
	 * @return The underlying client, allowing access to non-job-related
	 *         operations.
	 */
	public SpallocClient getClient() {
		return client;
	}
}
