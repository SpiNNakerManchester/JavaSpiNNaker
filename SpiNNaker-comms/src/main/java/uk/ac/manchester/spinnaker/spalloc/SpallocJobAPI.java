package uk.ac.manchester.spinnaker.spalloc;

import java.io.IOException;
import java.util.List;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.spalloc.exceptions.JobDestroyedException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocStateChangeTimeoutException;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.Connection;
import uk.ac.manchester.spinnaker.spalloc.messages.State;

/** The interface supported by a {@linkplain SpallocJob spalloc job}. */
public interface SpallocJobAPI {
	/**
	 * Destroy the job and disconnect from the server.
	 *
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	default void destroy() throws IOException, SpallocServerException {
		destroy(null);
	};

	/**
	 * Destroy the job and disconnect from the server.
	 *
	 * @param reason
	 *            Gives a human-readable explanation for the destruction of the
	 *            job.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	void destroy(String reason) throws IOException, SpallocServerException;

	/**
	 * Turn the boards allocated to the job on or off.
	 * <p>
	 * Does nothing if the job has not yet been allocated any boards.
	 * <p>
	 * The {@link #waitUntilReady(Integer)} method may be used to wait for the
	 * boards to fully turn on or off.
	 *
	 * @param powerOn
	 *            true to power on the boards, false to power off. If the boards
	 *            are already turned on, setting power to true will reset them.
	 *            If <tt>null</tt>, this method does nothing.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	void setPower(Boolean powerOn) throws IOException, SpallocServerException;

	/**
	 * Reset (power-cycle) the boards allocated to the job.
	 * <p>
	 * Does nothing if the job has not been allocated.
	 * <p>
	 * The {@link #waitUntilReady(Integer)} method may be used to wait for the
	 * boards to fully turn on or off.
	 *
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	default void reset() throws IOException, SpallocServerException {
		setPower(true);
	}

	/** @return The ID of the job. */
	int getID();

	/**
	 * @return The current state of the job.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	State getState() throws IOException, SpallocServerException;

	/**
	 * @return The current power state of the job.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	Boolean getPower() throws IOException, SpallocServerException;

	/**
	 * @return The reason for destruction of the job, or <tt>null</tt> if there
	 *         is no reason (perhaps because the job isn't destroyed). Note that
	 *         you should use {@link #getState()} to determine if the job is
	 *         destroyed, not this method.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	String getDestroyReason() throws IOException, SpallocServerException;

	/**
	 * @return The list of connections, where each connection says what the
	 *         hostname is to talk to a particular board's root chip.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	List<Connection> getConnections()
			throws IOException, SpallocServerException;

	/**
	 * @return The name of the host that is the root chip of the whole
	 *         allocation.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	String getHostname() throws IOException, SpallocServerException;

	/**
	 * @return The dimensions of the machine in chips, e.g., for booting.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	MachineDimensions getDimensions()
			throws IOException, SpallocServerException;

	/**
	 * @return The name of the machine the job is allocated on.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	String getMachineName() throws IOException, SpallocServerException;

	/**
	 * @return All the boards allocated to the job.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	List<BoardCoordinates> getBoards()
			throws IOException, SpallocServerException;

	/**
	 * Block until the job's state changes from the supplied state.
	 *
	 * @param oldState
	 *            The current state.
	 * @return The new state.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	default State waitForStateChange(State oldState)
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
	 *             If the spalloc server rejects the operation request.
	 */
	State waitForStateChange(State oldState, Integer timeout)
			throws SpallocServerException;

	/**
	 * Block until the job is allocated and ready.
	 *
	 * @param timeout
	 *            The number of milliseconds to wait before timing out. If None,
	 *            wait forever.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws SpallocStateChangeTimeoutException
	 *             If the timeout expired before the ready state was entered.
	 * @throws JobDestroyedException
	 *             If the job was destroyed before becoming ready.
	 */
	void waitUntilReady(Integer timeout)
			throws JobDestroyedException, IOException, SpallocServerException,
			SpallocStateChangeTimeoutException;

	/**
	 * Locates and returns the physical coordinates containing a given chip in a
	 * machine allocated to this job.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @return the physical coordinates of the board
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	BoardPhysicalCoordinates whereIs(HasChipLocation chip)
			throws IOException, SpallocServerException;
}
