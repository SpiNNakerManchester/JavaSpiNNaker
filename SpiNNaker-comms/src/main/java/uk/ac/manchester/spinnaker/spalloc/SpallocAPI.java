package uk.ac.manchester.spinnaker.spalloc;

/*
 * Disable style check for these imports; we use some extra imports here just
 * to support Javadoc (cross-referencing the notification message classes).
 */
// CHECKSTYLE:OFF
import java.io.IOException;
import java.util.List;
import java.util.Map;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocProtocolException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocProtocolTimeoutException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.JobsChangedNotification;
import uk.ac.manchester.spinnaker.spalloc.messages.Machine;
import uk.ac.manchester.spinnaker.spalloc.messages.MachinesChangedNotification;
import uk.ac.manchester.spinnaker.spalloc.messages.Notification;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;
// CHECKSTYLE:ON

/**
 * The interface exposed by the low-level Spalloc Client.
 *
 * @author Donal Fellows
 */
public interface SpallocAPI {
	/**
	 * Request the version of the spalloc server.
	 *
	 * @return the server's version.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default Version version() throws IOException, SpallocServerException {
		return version(null);
	}

	/**
	 * Request the version of the spalloc server.
	 *
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @return the server's version.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	Version version(Integer timeout) throws IOException,
			SpallocProtocolTimeoutException, SpallocServerException;

	/**
	 * Create a job.
	 *
	 * @param args
	 *            The arguments, describing default (empty), the number of
	 *            boards (one arg), the triad size (two args) or the board
	 *            location (three args).
	 * @param kwargs
	 *            Additional arguments required. Must include the key
	 *            <tt>owner</tt>. Values can be boxed primitive types or
	 *            strings.
	 * @return the ID of the created job.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default int createJob(List<Integer> args, Map<String, Object> kwargs)
			throws IOException, SpallocServerException {
		return createJob(args, kwargs, null);
	}

	/**
	 * Create a job.
	 *
	 * @param args
	 *            The arguments, describing default (empty), the number of
	 *            boards (one arg), the triad size (two args) or the board
	 *            location (three args).
	 * @param kwargs
	 *            Additional arguments required. Must include the key
	 *            <tt>owner</tt>. Values can be boxed primitive types or
	 *            strings.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @return the ID of the created job.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	int createJob(List<Integer> args, Map<String, Object> kwargs,
			Integer timeout) throws IOException,
			SpallocProtocolTimeoutException, SpallocServerException;

	/**
	 * Keep a job alive. Needs to be regularly called.
	 *
	 * @param jobID
	 *            The job to ask about.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default void jobKeepAlive(int jobID)
			throws IOException, SpallocServerException {
		jobKeepAlive(jobID, null);
	}

	/**
	 * Keep a job alive. Needs to be regularly called.
	 *
	 * @param jobID
	 *            The job to ask about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	void jobKeepAlive(int jobID, Integer timeout) throws IOException,
			SpallocProtocolTimeoutException, SpallocServerException;

	/**
	 * Get the state of a job.
	 *
	 * @param jobID
	 *            The job to get the state of.
	 * @return a description of the job's state.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default JobState getJobState(int jobID)
			throws IOException, SpallocServerException {
		return getJobState(jobID, null);
	}

	/**
	 * Get the state of a job.
	 *
	 * @param jobID
	 *            The job to get the state of.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @return a description of the job's state.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	JobState getJobState(int jobID, Integer timeout) throws IOException,
			SpallocProtocolTimeoutException, SpallocServerException;

	/**
	 * Get information about a job's allocated machine.
	 *
	 * @param jobID
	 *            The job whose machine you want to ask about.
	 * @return a description of the machine.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default JobMachineInfo getJobMachineInfo(int jobID)
			throws IOException, SpallocServerException {
		return getJobMachineInfo(jobID, null);
	}

	/**
	 * Get information about a job's allocated machine.
	 *
	 * @param jobID
	 *            The job whose machine you want to ask about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @return a description of the machine.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	JobMachineInfo getJobMachineInfo(int jobID, Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException;

	/**
	 * Turn on a job's allocated boards.
	 *
	 * @param jobID
	 *            The job to request about.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default void powerOnJobBoards(int jobID)
			throws IOException, SpallocServerException {
		powerOnJobBoards(jobID, null);
	}

	/**
	 * Turn on a job's allocated boards.
	 *
	 * @param jobID
	 *            The job to request about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	void powerOnJobBoards(int jobID, Integer timeout) throws IOException,
			SpallocProtocolTimeoutException, SpallocServerException;

	/**
	 * Turn off a job's allocated boards.
	 *
	 * @param jobID
	 *            The job to request about.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default void powerOffJobBoards(int jobID)
			throws IOException, SpallocServerException {
		powerOffJobBoards(jobID, null);
	}

	/**
	 * Turn off a job's allocated boards.
	 *
	 * @param jobID
	 *            The job to request about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	void powerOffJobBoards(int jobID, Integer timeout) throws IOException,
			SpallocProtocolTimeoutException, SpallocServerException;

	/**
	 * Destroy a job.
	 *
	 * @param jobID
	 *            The ID of the job.
	 * @param reason
	 *            Why the job is to be destroyed.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default void destroyJob(int jobID, String reason)
			throws IOException, SpallocServerException {
		destroyJob(jobID, reason, null);
	}

	/**
	 * Destroy a job.
	 *
	 * @param jobID
	 *            The ID of the job.
	 * @param reason
	 *            Why the job is to be destroyed.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	void destroyJob(int jobID, String reason, Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException;

	/**
	 * Enable or disable notifications of changes in job state.
	 *
	 * @param jobID
	 *            The job to request (or cancel requests) about, or
	 *            <tt>null</tt> to be notified/not notified about all jobs.
	 * @param enable
	 *            True to enable notifications, false to disable them.
	 * @see JobsChangedNotification
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default void notifyJob(Integer jobID, boolean enable)
			throws IOException, SpallocServerException {
		notifyJob(jobID, enable, null);
	}

	/**
	 * Enable or disable notifications of changes in job state.
	 *
	 * @param jobID
	 *            The job to request (or cancel requests) about, or
	 *            <tt>null</tt> to be notified/not notified about all jobs.
	 * @param enable
	 *            True to enable notifications, false to disable them.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @see JobsChangedNotification
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	void notifyJob(Integer jobID, boolean enable, Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException;

	/**
	 * Enable or disable notifications of changes in machine state.
	 *
	 * @param machineName
	 *            The machine to request (or cancel requests) about, or
	 *            <tt>null</tt> to be notified/not notified about all machines
	 *            (known to spalloc).
	 * @param enable
	 *            True to enable notifications, false to disable them.
	 * @see MachinesChangedNotification
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default void notifyMachine(String machineName, boolean enable)
			throws IOException, SpallocServerException {
		notifyMachine(machineName, enable, null);
	}

	/**
	 * Enable or disable notifications of changes in machine state.
	 *
	 * @param machineName
	 *            The machine to request (or cancel requests) about, or
	 *            <tt>null</tt> to be notified/not notified about all machines
	 *            (known to spalloc).
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @param enable
	 *            True to enable notifications, false to disable them.
	 * @see MachinesChangedNotification
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	void notifyMachine(String machineName, boolean enable, Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException;

	/**
	 * List all jobs.
	 *
	 * @return A list of allocated/queued jobs in order of creation from oldest
	 *         (first) to newest (last). This is unmodifiable.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default List<JobDescription> listJobs()
			throws IOException, SpallocServerException {
		return listJobs(null);
	}

	/**
	 * List all jobs.
	 *
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @return A list of allocated/queued jobs in order of creation from oldest
	 *         (first) to newest (last). This is unmodifiable.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	List<JobDescription> listJobs(Integer timeout) throws IOException,
			SpallocProtocolTimeoutException, SpallocServerException;

	/**
	 * List all known machines.
	 *
	 * @return The list of machines known to the system in order of priority
	 *         from highest (first) to lowest (last). This is unmodifiable.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default List<Machine> listMachines()
			throws IOException, SpallocServerException {
		return listMachines(null);
	}

	/**
	 * List all known machines.
	 *
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @return The list of machines known to the system in order of priority
	 *         from highest (first) to lowest (last). This is unmodifiable.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	List<Machine> listMachines(Integer timeout) throws IOException,
			SpallocProtocolTimeoutException, SpallocServerException;

	/**
	 * Get the physical location of a board.
	 *
	 * @param machineName
	 *            the name of the machine containing the board.
	 * @param coords
	 *            the logical location of the board.
	 * @return the physical location, or <tt>null</tt> if the logical location
	 *         doesn't map to a real board.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default BoardPhysicalCoordinates getBoardPosition(String machineName,
			BoardCoordinates coords)
			throws IOException, SpallocServerException {
		return getBoardPosition(machineName, coords, null);
	}

	/**
	 * Get the physical location of a board.
	 *
	 * @param machineName
	 *            the name of the machine containing the board.
	 * @param coords
	 *            the logical location of the board.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @return the physical location, or <tt>null</tt> if the logical location
	 *         doesn't map to a real board.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	BoardPhysicalCoordinates getBoardPosition(String machineName,
			BoardCoordinates coords, Integer timeout) throws IOException,
			SpallocProtocolTimeoutException, SpallocServerException;

	/**
	 * Get the logical location of a board.
	 *
	 * @param machineName
	 *            the name of the machine containing the board.
	 * @param coords
	 *            the physical location of the board.
	 * @return the logical location, or <tt>null</tt> if the physical location
	 *         doesn't map to a real board.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default BoardCoordinates getBoardPosition(String machineName,
			BoardPhysicalCoordinates coords)
			throws IOException, SpallocServerException {
		return getBoardPosition(machineName, coords, null);
	}

	/**
	 * Get the logical location of a board.
	 *
	 * @param machineName
	 *            the name of the machine containing the board.
	 * @param coords
	 *            the physical location of the board.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @return the logical location, or <tt>null</tt> if the physical location
	 *         doesn't map to a real board.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	BoardCoordinates getBoardPosition(String machineName,
			BoardPhysicalCoordinates coords, Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException;

	/**
	 * Locate a chip within a job's allocation.
	 *
	 * @param jobID
	 *            The job to ask about.
	 * @param chip
	 *            The coordinates of the chip to ask about.
	 * @return A description of the chip's location, or <tt>null</tt> if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default WhereIs whereIs(int jobID, HasChipLocation chip)
			throws IOException, SpallocServerException {
		return whereIs(jobID, chip, null);
	}

	/**
	 * Locate a chip within a job's allocation.
	 *
	 * @param jobID
	 *            The job to ask about.
	 * @param chip
	 *            The coordinates of the chip to ask about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @return A description of the chip's location, or <tt>null</tt> if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	WhereIs whereIs(int jobID, HasChipLocation chip, Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException;

	/**
	 * Locate a chip within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param chip
	 *            The coordinates of the chip to ask about.
	 * @return A description of the chip's location, or <tt>null</tt> if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default WhereIs whereIs(String machine, HasChipLocation chip)
			throws IOException, SpallocServerException {
		return whereIs(machine, chip, null);
	}

	/**
	 * Locate a chip within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param chip
	 *            The coordinates of the chip to ask about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @return A description of the chip's location, or <tt>null</tt> if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	WhereIs whereIs(String machine, HasChipLocation chip, Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException;

	/**
	 * Locate a board within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The physical coordinates of the board to ask about.
	 * @return A description of the board's location, or <tt>null</tt> if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default WhereIs whereIs(String machine, BoardPhysicalCoordinates coords)
			throws IOException, SpallocServerException {
		return whereIs(machine, coords, null);
	}

	/**
	 * Locate a board within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The physical coordinates of the board to ask about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @return A description of the board's location, or <tt>null</tt> if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	WhereIs whereIs(String machine, BoardPhysicalCoordinates coords,
			Integer timeout) throws IOException,
			SpallocProtocolTimeoutException, SpallocServerException;

	/**
	 * Locate a board within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The logical coordinates of the board to ask about.
	 * @return A description of the board's location, or <tt>null</tt> if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 */
	default WhereIs whereIs(String machine, BoardCoordinates coords)
			throws IOException, SpallocServerException {
		return whereIs(machine, coords, null);
	}

	/**
	 * Locate a board within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The logical coordinates of the board to ask about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or <tt>null</tt> to wait forever.
	 * @return A description of the board's location, or <tt>null</tt> if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 */
	WhereIs whereIs(String machine, BoardCoordinates coords, Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException;

	/**
	 * Return the next notification to arrive.
	 *
	 * @return The notification sent by the server.
	 * @see JobsChangedNotification
	 * @see MachinesChangedNotification
	 * @throws SpallocProtocolException
	 *             If the socket is unusable or becomes disconnected.
	 */
	default Notification waitForNotification() throws SpallocProtocolException {
		try {
			return waitForNotification(null);
		} catch (SpallocProtocolTimeoutException e) {
			throw new RuntimeException("unexpected timeout", e);
		}
	}

	/**
	 * Return the next notification to arrive.
	 *
	 * @param timeout
	 *            The number of seconds to wait before timing out or
	 *            <tt>null</tt> if this function should try again forever. If
	 *            negative, only responses already-received will be returned; if
	 *            no responses are available, in this case the function does not
	 *            raise a ProtocolTimeoutError but returns <tt>null</tt>
	 *            instead.
	 * @return The notification sent by the server.
	 * @see JobsChangedNotification
	 * @see MachinesChangedNotification
	 * @throws SpallocProtocolTimeoutException
	 *             If a timeout occurs.
	 * @throws SpallocProtocolException
	 *             If the socket is unusable or becomes disconnected.
	 */
	Notification waitForNotification(Integer timeout)
			throws SpallocProtocolException, SpallocProtocolTimeoutException;
}
