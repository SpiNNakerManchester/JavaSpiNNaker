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
package uk.ac.manchester.spinnaker.allocator;

import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.readLines;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import uk.ac.manchester.spinnaker.connections.EIEIOConnection;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * An API for talking to the Spalloc service.
 *
 * @see SpallocClientFactory
 * @author Donal Fellows
 */
public interface SpallocClient {
	/**
	 * Get the server version ID.
	 *
	 * @return A version ID.
	 * @throws IOException
	 *             If things go wrong.
	 */
	Version getVersion() throws IOException;

	/**
	 * Get a list of all Spalloc machines.
	 *
	 * @return The list of machines.
	 * @throws IOException
	 *             If things go wrong.
	 */
	List<Machine> listMachines() throws IOException;

	/**
	 * List the existing non-terminated jobs.
	 *
	 * @param waitForChange
	 *            If {@code true}, will wait until the list of jobs may have
	 *            changed. (Best-effort only; waiting time is bounded at 30
	 *            seconds.)
	 * @return A list of jobs.
	 * @throws IOException
	 *             If things go wrong.
	 */
	List<Job> listJobs(boolean waitForChange) throws IOException;

	/**
	 * List the jobs, including the deleted ones.
	 * <p>
	 * <strong>NB:</strong> Care should be taken; this may produce a lot of
	 * output.
	 *
	 * @param waitForChange
	 *            If {@code true}, will wait until the list of jobs may have
	 *            changed. (Best-effort only; waiting time is bounded at 30
	 *            seconds.)
	 * @return A stream of jobs.
	 * @throws IOException
	 *             If things go wrong.
	 */
	Stream<Job> listJobsWithDeleted(boolean waitForChange) throws IOException;

	/**
	 * List the existing non-terminated jobs.
	 *
	 * @return A list of jobs.
	 * @throws IOException
	 *             If things go wrong.
	 */
	default List<Job> listJobs() throws IOException {
		return listJobs(false);
	}

	/**
	 * List the jobs, including the deleted ones.
	 * <p>
	 * <strong>NB:</strong> Care should be taken; this may produce a lot of
	 * output.
	 *
	 * @return A stream of jobs.
	 * @throws IOException
	 *             If things go wrong.
	 */
	default Stream<Job> listJobsWithDeleted() throws IOException {
		return listJobsWithDeleted(false);
	}

	/**
	 * Create a job.
	 *
	 * @param createInstructions
	 *            Describes the job to create.
	 * @return A handle to the created job.
	 * @throws IOException
	 *             If job creation fails.
	 */
	Job createJob(CreateJob createInstructions) throws IOException;

	/** The services offered relating to a Spalloc machine. */
	interface Machine {
		/**
		 * The machine's name. Never empty, never {@code null}.
		 *
		 * @return The name of the machine.
		 */
		String getName();

		/**
		 * The tags of the machine. If this includes "{@code default}", this is
		 * the machine that jobs will usually go to.
		 *
		 * @return The tags of the machine.
		 */
		List<String> getTags();

		/** The width of the machine.
		 *  @return The width of the machine, in triads. */
		int getWidth();

		/** The height of the machine.
		 *  @return The height of the machine, in triads. */
		int getHeight();

		/** The number of live boards.
		 *  @return The (estimated) number of live boards in the machine. */
		int getLiveBoardCount();

		/**
		 * What boards in the machine are dead?
		 *
		 * @return The dead boards of the machine.
		 */
		List<BoardCoords> getDeadBoards();

		/**
		 * What links in the machine are dead? Only links where both boards are
		 * alive.
		 *
		 * @return The dead links of the machine.
		 */
		List<DeadLink> getDeadLinks();

		/**
		 * Wait (for up to 30 seconds) until the state of the machine might have
		 * changed.
		 *
		 * @throws IOException
		 *             If communication with the server fails
		 */
		void waitForChange() throws IOException;

		/**
		 * Given logical triad coordinates, return more info about a board.
		 *
		 * @param x
		 *            Triad X coordinate
		 * @param y
		 *            Triad Y coordinate
		 * @param z
		 *            Triad Z coordinate
		 * @return Board information
		 * @throws FileNotFoundException
		 *             If the board doesn't exist.
		 * @throws IOException
		 *             If communication with the server fails
		 */
		WhereIs getBoardByTriad(int x, int y, int z)
				throws FileNotFoundException, IOException;

		/**
		 * Given physical coordinates, return more info about a board.
		 *
		 * @param cabinet
		 *            Cabinet number; cabinets contain frames.
		 * @param frame
		 *            Frame number; frames contain boards.
		 * @param board
		 *            Board number
		 * @return Board information
		 * @throws FileNotFoundException
		 *             If the board doesn't exist.
		 * @throws IOException
		 *             If communication with the server fails
		 */
		WhereIs getBoardByPhysicalCoords(int cabinet, int frame, int board)
				throws FileNotFoundException, IOException;

		/**
		 * Given a <em>global</em> chip location, return more info about the
		 * board that contains it.
		 *
		 * @param chip
		 *            The chip location
		 * @return Board information
		 * @throws FileNotFoundException
		 *             If the board doesn't exist.
		 * @throws IOException
		 *             If communication with the server fails
		 */
		WhereIs getBoardByChip(HasChipLocation chip)
				throws FileNotFoundException, IOException;

		/**
		 * Given an IP address, return more info about a board.
		 *
		 * @param address
		 *            Board IP address
		 * @return Board information
		 * @throws FileNotFoundException
		 *             If the board doesn't exist.
		 * @throws IOException
		 *             If communication with the server fails
		 */
		WhereIs getBoardByIPAddress(String address)
				throws FileNotFoundException, IOException;
	}

	/**
	 * The services offered relating to a Spalloc job. Jobs run on
	 * {@linkplain Machine machines}, and have boards allocated to them while
	 * they do so. Those boards (which will be connected) are a fundamental
	 * resource that allows SpiNNaker programs to be run.
	 */
	interface Job {
		/**
		 * Get a description of a job. Includes the state of the job.
		 *
		 * @param waitForChange
		 *            If {@code true}, will wait until the jobs may have
		 *            changed. (Best-effort only; waiting time is bounded at 30
		 *            seconds.)
		 * @return The job description &amp; state. Check the state to see
		 *         whether the job has had resources allocated yet.
		 * @throws IOException
		 *             If communication fails.
		 */
		JobDescription describe(boolean waitForChange) throws IOException;

		/**
		 * Get a description of a job. Includes the state of the job.
		 *
		 * @return The job description &amp; state. Check the state to see
		 *         whether the job has had resources allocated yet.
		 * @throws IOException
		 *             If communication fails.
		 */
		default JobDescription describe() throws IOException {
			return describe(false);
		}

		/**
		 * Must be periodically called to prevent the service watchdog from
		 * culling the job.
		 *
		 * @throws IOException
		 *             If communication fails.
		 */
		void keepalive() throws IOException;

		/**
		 * Mark a job as deleted.
		 *
		 * @param reason
		 *            Why the job is to be deleted.
		 * @throws IOException
		 *             If communication fails.
		 */
		void delete(String reason) throws IOException;

		/**
		 * Get a description of what's been allocated to the job.
		 *
		 * @return a description of the allocated resources
		 * @throws IOException
		 *             If communication fails, the resources have not yet been
		 *             allocated, or the job is deleted.
		 */
		AllocatedMachine machine() throws IOException;

		/**
		 * Get whether the boards of the machine are all switched on.
		 *
		 * @return {@code true} iff the boards are all on.
		 * @throws IOException
		 *             If communication fails, the resources have not yet been
		 *             allocated, or the job is deleted.
		 */
		boolean getPower() throws IOException;

		/**
		 * Set the power state of the boards of the machine. Note that actually
		 * changing the power state of the boards may take some time.
		 *
		 * @param switchOn
		 *            {@code true} to switch the boards on, {@code false} to
		 *            switch them off.
		 * @return {@code true} iff the boards are all on.
		 * @throws IOException
		 *             If communication fails, the resources have not yet been
		 *             allocated, or the job is deleted.
		 */
		boolean setPower(boolean switchOn) throws IOException;

		/**
		 * Given the location of a chip within an allocation, return more info
		 * about a board.
		 *
		 * @param chip
		 *            Chip location (relative to the root of the allocation).
		 * @return Board information
		 * @throws FileNotFoundException
		 *             If the board doesn't exist or no boards are allocated to
		 *             the job currently.
		 * @throws IOException
		 *             If communication fails or the job is deleted.
		 */
		WhereIs whereIs(HasChipLocation chip)
				throws FileNotFoundException, IOException;

		/**
		 * Create a proxied transceiver for talking to the boards of the job.
		 * The transceiver will be configured so that it can talk to all the
		 * boards of the job and nothing else.
		 *
		 * @return The transceiver.
		 * @throws IOException
		 *             If communication fails or the job is deleted.
		 * @throws InterruptedException
		 *             If interrupted waiting for the connection to be set up.
		 * @throws SpinnmanException
		 *             If transceiver construction fails.
		 */
		TransceiverInterface getTransceiver()
				throws IOException, InterruptedException, SpinnmanException;

		/**
		 * Create a proxied SCP connection to a specific Ethernet chip of the
		 * job.
		 *
		 * @param chip
		 *            Which Ethernet chip to connect to.
		 * @return The connection. It is the responsibility of the caller to
		 *         close this connection at the right time.
		 * @throws IOException
		 *             If communication fails or the job is deleted.
		 * @throws InterruptedException
		 *             If interrupted waiting for the connection to be set up.
		 */
		SCPConnection getConnection(HasChipLocation chip)
				throws IOException, InterruptedException;


		/**
		 * Create an <em>unconnected</em> EIEIO connection that can only talk
		 * the boards of the job.
		 *
		 * @return The connection. It is the responsibility of the caller to
		 *         close this connection at the right time.
		 * @throws IOException
		 *             If communication fails or the job is deleted.
		 * @throws InterruptedException
		 *             If interrupted waiting for the connection to be set up.
		 */
		EIEIOConnection getEIEIOConnection()
				throws IOException, InterruptedException;
	}

	/**
	 * Exception caused by the server sending an error.
	 */
	class Exception extends RuntimeException {
		private static final long serialVersionUID = -1363689283367574333L;

		/** The HTTP response code that triggered the exception. */
		private final int responseCode;

		/**
		 * Create an instance.
		 *
		 * @param message
		 *            The message body of the error.
		 * @param responseCode
		 *            The HTTP response code that triggered the exception.
		 */
		public Exception(String message, int responseCode) {
			super(message);
			this.responseCode = responseCode;
		}

		Exception(InputStream stream, int responseCode) throws IOException {
			super(consume(stream));
			this.responseCode = responseCode;
		}

		private static String consume(InputStream stream) throws IOException {
			try {
				return join("\n", readLines(stream, UTF_8));
			} finally {
				stream.close();
			}
		}

		/**
		 * @return The HTTP response code that triggered the exception.
		 */
		public int getResponseCode() {
			return responseCode;
		}
	}
}
