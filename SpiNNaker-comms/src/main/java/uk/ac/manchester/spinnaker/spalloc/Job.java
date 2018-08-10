package uk.ac.manchester.spinnaker.spalloc;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.spalloc.Utils.makeTimeout;
import static uk.ac.manchester.spinnaker.spalloc.Utils.timeLeft;
import static uk.ac.manchester.spinnaker.spalloc.Utils.timedOut;
import static uk.ac.manchester.spinnaker.spalloc.messages.State.DESTROYED;
import static uk.ac.manchester.spinnaker.spalloc.messages.State.UNKNOWN;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MS_PER_S;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.messages.model.Version;
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
 * Constructing a {@link Job} object connects to a
 * <a href="https://github.com/project-rig/spalloc_server">spalloc-server</a>
 * and requests a number of SpiNNaker boards. The job object may then be used to
 * monitor the state of the request, control the boards allocated and determine
 * their IP addresses.
 * <p>
 * In its simplest form, a {@link Job} can be used as a context manager like
 * so::
 *
 * <pre>
    >>> from spalloc import Job
    >>> with Job(6) as j:
    ...     my_boot(j.hostname, j.width, j.height)
    ...     my_application(j.hostname)
 * </pre>
 *
 * In this example a six-board machine is requested and the <tt>with</tt>
 * context is entered once the allocation has been made and the allocated boards
 * are fully powered on. When control leaves the block, the job is destroyed and
 * the boards shut down by the server ready for another job.
 * <p>
 * For more fine-grained control, the same functionality is available via
 * various methods:
 *
 * <pre>
    >>> from spalloc import Job
    >>> j = Job(6)
    >>> j.wait_until_ready()
    >>> my_boot(j.hostname, j.width, j.height)
    >>> my_application(j.hostname)
    >>> j.destroy()
 * </pre>
 *
 * <b>Note:</b> <blockquote class="note"> More complex applications may wish to
 * log the following properties of their job to support later debugging efforts:
 * <ul>
 * <li><tt>job.id</tt> &mdash; May be used to query the state of the job and
 * find out its fate if cancelled or destroyed. The <i>spalloc-job</i> command
 * can be used to discover the state/fate of the job and <i>spalloc-where-is</i>
 * may be used to find out what boards problem chips reside on.
 * <li><tt>job.machine_name</tt> and <tt>job.boards</tt> together give a
 * complete record of the hardware used by the job. The <i>spalloc-where-is</i>
 * command may be used to find out the physical locations of the boards used.
 * </ul>
 * </blockquote>
 */
public class Job implements AutoCloseable {
	private static final Logger log = getLogger(Job.class);
	private static final int DEFAULT_KEEPALIVE = 30;
	private static final int KEEPALIVE_INTERVAL =
			(int) (DEFAULT_KEEPALIVE * MS_PER_S);
	private static final ChipLocation ROOT = new ChipLocation(0, 0);

	private ProtocolClient client;
	private int id;
	private Integer timeout;
	private Integer keepaliveTime;
	private Thread keepalive;
	private volatile boolean stopping;
	private JobState status;
	private long statusTimestamp;
	private JobMachineInfo machineInfo;
	private int reconnectDelay = (int) (10 * MS_PER_S);

	public Job(String hostname, Integer timeout, List<Integer> args,
			Map<String, Object> kwargs)
			throws IOException, SpallocServerException {
		this(hostname, ProtocolClient.DEFAULT_PORT, timeout, args, kwargs);
	}

	public Job(String hostname, int port, Integer timeout, List<Integer> args,
			Map<String, Object> kwargs)
			throws IOException, SpallocServerException {
		this.client = new ProtocolClient(hostname, port, timeout);
		this.timeout = timeout;
		if (kwargs.containsKey("keepalive")) {
			keepaliveTime =
					(int) (((Number) kwargs.get("keepalive")).doubleValue()
							* MS_PER_S);
		} else {
			keepaliveTime = KEEPALIVE_INTERVAL;
			kwargs.put("keepalive", DEFAULT_KEEPALIVE);
		}
		id = client.createJob(args, kwargs, timeout);
		log.info("created spalloc job with ID: {}", id);
		keepalive = new Thread(() -> keepalive(),
				"keepalive for spalloc job " + id);
		keepalive.setDaemon(true);
		stopping = false;
		keepalive.start();
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
	 * @throws SpallocServerException
	 * @throws JobDestroyedException
	 */
	public Job(String hostname, Integer timeout, int id)
			throws IOException, SpallocServerException, JobDestroyedException {
		this(hostname, ProtocolClient.DEFAULT_PORT, timeout, id);
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
	 * @throws SpallocServerException
	 * @throws JobDestroyedException
	 */
	public Job(String hostname, int port, Integer timeout, int id)
			throws IOException, SpallocServerException, JobDestroyedException {
		this.client = new ProtocolClient(hostname, port, timeout);
		this.timeout = timeout;
		this.id = id;
		/*
		 * If the job no longer exists, we can't get the keepalive interval (and
		 * there's nothing to keepalive) so just bail out.
		 */
		JobState jobState = getStatus();
		if (jobState.getState() == UNKNOWN
				|| jobState.getState() == DESTROYED) {
			if (jobState.getReason() != null) {
				throw new JobDestroyedException(format(
						"Job %d does not exist: %s: %s", id,
						jobState.getState().name(), jobState.getReason()));
			} else {
				throw new JobDestroyedException(
						format("Job %d does not exist: %s", id,
								jobState.getState().name()));
			}
		}
		// Snag the keepalive interval from the job
		keepaliveTime = (int) (jobState.getKeepAlive() * MS_PER_S);
		log.info("resumed spalloc job with ID: {}", id);
		keepalive = new Thread(() -> keepalive(),
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
			} catch (InterruptedException e) {
				continue;
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
			log.warn("Spalloc server is unreachable (%s), will keep trying...",
					e);
			try {
				client.close();
			} catch (IOException inner) {
				// close failed?! Nothing we can do but log and try later
				log.error("problem closing connection", inner);
			}
		}
	}

	private static final Version MIN = new Version(0, 4, 0);
	private static final Version MAX = new Version(2, 0, 0);

	/**
	 * Assert that the server version is compatible. This client supports from
	 * 0.4.0 to 2.0.0 (but not including the latter).
	 *
	 * @throws IOException
	 * @throws SpallocServerException
	 */
	protected void assertCompatibleVersion()
			throws IOException, SpallocServerException {
		Version v = client.version(timeout);
		if (MIN.compareTo(v) <= 0 && MAX.compareTo(v) > 0) {
			return;
		}
		client.close();
		throw new IllegalStateException(
				"Server version " + v + " is not compatible with this client");
	}

	public void destroy() throws IOException, SpallocServerException {
		destroy(null);
	}

	public void destroy(String reason)
			throws IOException, SpallocServerException {
		try {
			client.destroyJob(id, reason, timeout);
		} finally {
			close();
		}
	}

	/**
	 * Turn the boards allocated to the job on or off.
	 * <p>
	 * Does nothing if the job has not yet been allocated any boards.
	 * <p>
	 * The {@link #waitUntilReady(Integer)} method may be used to wait for the boards
	 * to fully turn on or off.
	 *
	 * @param powerOn
	 *            true to power on the boards, false to power off. If the boards
	 *            are already turned on, setting power to true will reset them.
	 *            If <tt>null</tt>, this method does nothing.
	 */
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
	}

	/**
	 * Reset (power-cycle) the boards allocated to the job.
	 * <p>
	 * Does nothing if the job has not been allocated.
	 * <p>
	 * The {@link #waitUntilReady(Integer)} method may be used to wait for the boards
	 * to fully turn on or off.
	 */
	public void reset() throws IOException, SpallocServerException {
		setPower(true);
	}

	/** @return The ID of the job. */
	public int getID() {
		return id;
	}

	private static final int STATUS_CACHE_PERIOD = 500;

	private JobState getStatus() throws IOException, SpallocServerException {
		if (status == null || statusTimestamp < currentTimeMillis()
				- STATUS_CACHE_PERIOD) {
			status = client.getJobState(id, timeout);
			statusTimestamp = currentTimeMillis();
		}
		return status;
	}

	/** @return The current state of the job. */
	public State getState() throws IOException, SpallocServerException {
		return getStatus().getState();
	}

	/** @return The current power state of the job. */
	public Boolean getPower() throws IOException, SpallocServerException {
		return getStatus().getPower();
	}

	/**
	 * @return The reason for destruction of the job, or <tt>null</tt> if there
	 *         is no reason (perhaps because the job isn't destroyed). Note that
	 *         you should use {@link #getState()} to determine if the job is
	 *         destroyed, not this method.
	 */
	public String getDestroyReason()
			throws IOException, SpallocServerException {
		return getStatus().getReason();
	}

	private void retrieveMachineInfo()
			throws IOException, SpallocServerException {
		machineInfo = client.getJobMachineInfo(id, timeout);
	}

	public List<Connection> getConnections()
			throws IOException, SpallocServerException {
		if (machineInfo == null || machineInfo.getConnections() == null) {
			retrieveMachineInfo();
		}
		return machineInfo.getConnections();
	}

	public String getHostname() throws IOException, SpallocServerException {
		for (Connection c : getConnections()) {
			if (c.getChip().onSameChipAs(ROOT)) {
				return c.getHostname();
			}
		}
		return null;
	}

	public MachineDimensions getDimensions()
			throws IOException, SpallocServerException {
		if (machineInfo == null || machineInfo.getWidth() == 0) {
			retrieveMachineInfo();
		}
		if (machineInfo == null || machineInfo.getWidth() == 0) {
			return null;
		}
		return new MachineDimensions(machineInfo.getWidth(),
				machineInfo.getHeight());
	}

	public String getMachineName() throws IOException, SpallocServerException {
		if (machineInfo == null || machineInfo.getMachineName() == null) {
			retrieveMachineInfo();
		}
		if (machineInfo == null) {
			return null;
		}
		return machineInfo.getMachineName();
	}

	public List<BoardCoordinates> getBoards()
			throws IOException, SpallocServerException {
		if (machineInfo == null || machineInfo.getBoards() == null) {
			retrieveMachineInfo();
		}
		if (machineInfo == null || machineInfo.getBoards() == null) {
			return null;
		}
		return machineInfo.getBoards();
	}

	public State waitForStateChange(State oldState)
			throws SpallocServerException {
		return waitForStateChange(oldState, null);
	}

	/**
	 * Block until the job's state changes from the supplied state.
	 *
	 * @param oldState
	 *            The current state.
	 * @param timeout
	 *            The number of seconds to wait for a change before timing out.
	 *            If None, wait forever.
	 * @return The new state, or old state if timed out.
	 * @throws SpallocServerException
	 */
	public State waitForStateChange(State oldState, Integer timeout)
			throws SpallocServerException {
		Long finishTime = makeTimeout(timeout);

		// We may get disconnected while waiting so keep listening...
		while (!timedOut(finishTime)) {
			try {
				// Watch for changes in this Job's state
				client.notifyJob(id);

				// Wait for job state to change
				while (!timedOut(finishTime)) {
					// Has the job changed state?
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
	 *            when our timeout expires, or <tt>null</tt> for never
	 * @return True if the state has changed, or false on timeout
	 * @throws SpallocServerException
	 * @throws IOException
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
					waitTimeout =
							min(keepaliveTime / 2, timeLeft(finishTime));
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
			} catch (ProtocolTimeoutException e) {
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
	 * @throws InterruptedException
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

	/**
	 * Block until the job is allocated and ready.
	 *
	 * @param timeout
	 *            The number of milliseconds to wait before timing out. If None,
	 *            wait forever.
	 * @throws SpallocServerException
	 * @throws IOException
	 * @throws StateChangeTimeoutException
	 *             If the timeout expired before the ready state was entered.
	 * @throws JobDestroyedException
	 *             If the job was destroyed before becoming ready.
	 */
	public void waitUntilReady(Integer timeout) throws JobDestroyedException,
			IOException, SpallocServerException, StateChangeTimeoutException {
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
				// Now in the ready state!
				return;
			case QUEUED:
				log.info("Job has been queued by the spalloc server");
				break;
			case POWER:
				log.info("Waiting for board power commands to complete");
				break;
			case DESTROYED:
				// In a state which can never become ready
				throw new JobDestroyedException(getDestroyReason());
			default: // UNKNOWN
				// Server has forgotten what this job even was...
				throw new JobDestroyedException(
						"Spalloc server no longer recognises job");
			}
			// Wait for a state change...
			curState = waitForStateChange(curState, timeLeft(finishTime));
		}
		// Timed out!
		throw new StateChangeTimeoutException();
	}

	/**
	 * Locates and returns the physical coordinates containing a given chip in a
	 * machine allocated to this job.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @return the physical coordinates of the board
	 */
	public BoardPhysicalCoordinates whereIsOnMachine(HasChipLocation chip)
			throws IOException, SpallocServerException {
		WhereIs result = client.whereIs(id, chip, timeout);
		if (result == null) {
			throw new IllegalStateException(
					"received null instead of machine location");
		}
		return result.getPhysical();
	}
}

/** Thrown when a state change takes too long to occur. */
class StateChangeTimeoutException extends Exception {
	private static final long serialVersionUID = 4879238794331037892L;

}

/** Thrown when the job was destroyed while waiting for it to become ready. */
class JobDestroyedException extends Exception {
	private static final long serialVersionUID = 6082560756316191208L;

	public JobDestroyedException(String destroyReason) {
		super(destroyReason);
	}
}
