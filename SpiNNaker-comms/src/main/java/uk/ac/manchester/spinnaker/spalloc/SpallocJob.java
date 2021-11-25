/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.RECONNECT_DELAY_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.Utils.makeTimeout;
import static uk.ac.manchester.spinnaker.spalloc.Utils.timeLeft;
import static uk.ac.manchester.spinnaker.spalloc.Utils.timedOut;
import static uk.ac.manchester.spinnaker.spalloc.messages.State.DESTROYED;
import static uk.ac.manchester.spinnaker.spalloc.messages.State.QUEUED;
import static uk.ac.manchester.spinnaker.spalloc.messages.State.UNKNOWN;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;

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
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;

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
 * try (SpallocJob j = new SpallocJob(new CreateJob(6).owner(me))) {
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
 * SpallocJob j = new SpallocJob(new CreateJob(6).owner(me)));
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
	private JobMachineInfo machineInfoCache;

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
	 */
	public SpallocJob(String hostname, Integer timeout,
			CreateJob builder)
			throws IOException, SpallocServerException {
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
	 */
	public SpallocJob(String hostname, CreateJob builder)
			throws IOException, SpallocServerException {
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
	 */
	public SpallocJob(CreateJob builder)
			throws IOException, SpallocServerException {
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
	 */
	public SpallocJob(SpallocClient client, CreateJob builder)
			throws IOException, SpallocServerException {
		this(client, f2ms(config.getTimeout()), builder);
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
	 * @throws IllegalArgumentException
	 *             If a bad builder is given.
	 */
	public SpallocJob(SpallocClient client, Integer timeout, CreateJob builder)
			throws IOException, SpallocServerException {
		if (builder == null) {
			throw new IllegalArgumentException("a builder must be specified");
		}
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
	 * @throws IllegalArgumentException
	 *             If a bad builder is given.
	 */
	public SpallocJob(String hostname, Integer port, Integer timeout,
			CreateJob builder)
			throws IOException, SpallocServerException {
		if (builder == null) {
			throw new IllegalArgumentException("a builder must be specified");
		}
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
	 */
	public SpallocJob(int id)
			throws IOException, SpallocServerException, JobDestroyedException {
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
	 */
	public SpallocJob(String hostname, int id)
			throws IOException, SpallocServerException, JobDestroyedException {
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
	 */
	public SpallocJob(String hostname, Integer timeout, int id)
			throws IOException, SpallocServerException, JobDestroyedException {
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
	 */
	public SpallocJob(String hostname, int port, Integer timeout, int id)
			throws IOException, SpallocServerException, JobDestroyedException {
		this.client = new SpallocClient(hostname, port, timeout);
		this.timeout = timeout;
		this.id = id;
		client.connect();
		reconnectDelay = f2ms(config.getReconnectDelay());
		/*
		 * If the job no longer exists, we can't get the keepalive interval (and
		 * there's nothing to keepalive) so just bail out.
		 */
		JobState jobState = getStatus();
		if (jobState.getState() == UNKNOWN
				|| jobState.getState() == DESTROYED) {
			if (jobState.getReason() != null) {
				throw new JobDestroyedException(format(
						"SpallocJob %d does not exist: %s: %s", id,
						jobState.getState().name(), jobState.getReason()));
			} else {
				throw new JobDestroyedException(
						format("SpallocJob %d does not exist: %s", id,
								jobState.getState().name()));
			}
		}
		// Snag the keepalive interval from the job
		keepaliveTime = f2ms(jobState.getKeepalive());
		log.info("resumed spalloc job with ID: {}", id);
		launchKeepaliveDaemon();
	}

	private void launchKeepaliveDaemon() {
		log.info("launching keepalive thread for " + id + " with interval "
				+ (keepaliveTime / 2) + "ms");
		if (keepalive != null) {
			log.warn("launching second keepalive thread for " + id);
		}
		keepalive = new Thread(SPALLOC_WORKERS, this::keepalive,
				"keepalive for spalloc job " + id);
		keepalive.setDaemon(true);
		stopping = false;
		keepalive.start();
	}

	private void keepalive() {
		while (!stopping) {
			try {
				client.jobKeepAlive(id, timeout);
				if (!interrupted()) {
					sleep(keepaliveTime / 2);
				}
			} catch (IOException | SpallocServerException e) {
				stopping = true;
			} catch (InterruptedException ignore) {
			}
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
	 *
	 * If reconnection fails, the error is reported as a warning but no
	 * exception is raised.
	 */
	private void reconnect() {
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
	 * @throws IllegalStateException
	 *             If the server is not compatible with this client.
	 */
	protected void assertCompatibleVersion()
			throws IOException, SpallocServerException {
		Version v = client.version(timeout);
		if (MIN_VER.compareTo(v) <= 0 && MAX_VER.compareTo(v) > 0) {
			return;
		}
		client.close();
		throw new IllegalStateException(
				"Server version " + v + " is not compatible with this client");
	}

	@Override
	public void destroy(String reason)
			throws IOException, SpallocServerException {
		try {
			client.destroyJob(id, reason, timeout);
		} finally {
			close();
		}
		purgeStatus();
	}

	@Override
	public void setPower(Boolean powerOn)
			throws IOException, SpallocServerException {
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

	private JobState getStatus() throws IOException, SpallocServerException {
		if (statusCache == null || statusTimestamp < currentTimeMillis()
				- STATUS_CACHE_PERIOD) {
			statusCache = client.getJobState(id, timeout);
			statusTimestamp = currentTimeMillis();
		}
		return statusCache;
	}

	private void purgeStatus() {
		statusCache = null;
	}

	@Override
	public State getState() throws IOException, SpallocServerException {
		return getStatus().getState();
	}

	@Override
	public Boolean getPower() throws IOException, SpallocServerException {
		return getStatus().getPower();
	}

	@Override
	public String getDestroyReason()
			throws IOException, SpallocServerException {
		return getStatus().getReason();
	}

	private void retrieveMachineInfo()
			throws IOException, SpallocServerException, IllegalStateException {
		/*
		 * Check the job is still not QUEUED as then machine info is all nulls
		 * getJobMachineInfo works if the Job is in State.POWER
		 */
		// TODO what about state UNKNOWN and State.DESTROYED
		if (getState() == QUEUED) {
			// Double check very latest state.
			purgeStatus();
			if (getState() == QUEUED) {
				throw new IllegalStateException(
						"Job not Ready. Call waitUntilReady first.");
			}
		}

		machineInfoCache = client.getJobMachineInfo(id, timeout);
	}

	@Override
	public List<Connection> getConnections()
			throws IOException, SpallocServerException, IllegalStateException {
		if (machineInfoCache == null
				|| machineInfoCache.getConnections() == null) {
			retrieveMachineInfo();
		}
		return machineInfoCache.getConnections();
	}

	@Override
	public String getHostname() throws IOException, SpallocServerException {
		for (Connection c : getConnections()) {
			if (c.getChip().onSameChipAs(ZERO_ZERO)) {
				return c.getHostname();
			}
		}
		return null;
	}

	@Override
	public MachineDimensions getDimensions()
			throws IOException, SpallocServerException, IllegalStateException {
		if (machineInfoCache == null || machineInfoCache.getWidth() == 0) {
			retrieveMachineInfo();
		}
		if (machineInfoCache == null || machineInfoCache.getWidth() == 0) {
			return null;
		}
		return new MachineDimensions(machineInfoCache.getWidth(),
				machineInfoCache.getHeight());
	}

	@Override
	public String getMachineName()
			throws IOException, SpallocServerException, IllegalStateException {
		if (machineInfoCache == null
				|| machineInfoCache.getMachineName() == null) {
			retrieveMachineInfo();
		}
		if (machineInfoCache == null) {
			return null;
		}
		return machineInfoCache.getMachineName();
	}

	@Override
	public List<BoardCoordinates> getBoards()
			throws IOException, SpallocServerException, IllegalStateException {
		if (machineInfoCache == null || machineInfoCache.getBoards() == null) {
			retrieveMachineInfo();
		}
		if (machineInfoCache == null || machineInfoCache.getBoards() == null) {
			return null;
		}
		return machineInfoCache.getBoards();
	}

	@Override
	public State waitForStateChange(State oldState, Integer timeout)
			throws SpallocServerException {
		Long finishTime = makeTimeout(timeout);

		// We may get disconnected while waiting so keep listening...
		while (!timedOut(finishTime)) {
			try {
				// Watch for changes in this SpallocJob's state
				client.notifyJob(id, true);

				// Wait for job state to change
				while (!timedOut(finishTime)) {
					// Has the job changed state?
					purgeStatus();
					State newState = getStatus().getState();
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
				} catch (IOException | InterruptedException e1) {
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
	 */
	private boolean doWaitForAChange(Long finishTime)
			throws IOException, SpallocServerException {
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
			SpallocStateChangeTimeoutException {
		State curState = null;
		Long finishTime = makeTimeout(timeout);
		while (!timedOut(finishTime)) {
			if (curState == null) {
				/*
				 * Get initial state (NB: done here such that the command is
				 * never sent if the timeout has already occurred)
				 */
				curState = getStatus().getState();
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
			throws IOException, SpallocServerException {
		WhereIs result = client.whereIs(id, chip, timeout);
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
