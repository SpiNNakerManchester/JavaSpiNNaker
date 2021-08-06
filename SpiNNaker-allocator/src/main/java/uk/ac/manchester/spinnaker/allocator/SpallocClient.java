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

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.Version;

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
		/** @return The name of the machine. */
		String getName();

		/** @return The tags of the machine. */
		List<String> getTags();

		/** @return The width of the machine, in triads. */
		int getWidth();

		/** @return The height of the machine, in triads. */
		int getHeight();

		/** @return The (estimated) number of live boards in the machine. */
		int getLiveBoardCount();

		/** @return The dead boards of the machine. */
		List<BoardCoords> getDeadBoards();

		/** @return The dead links of the machine. */
		List<DeadLink> getDeadLinks();

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
	}

	/**
	 * Exception caused by the server sending an error.
	 */
	class Exception extends RuntimeException {
		private static final long serialVersionUID = -1363689283367574333L;

		private final int responseCode;

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

		public int getResponseCode() {
			return responseCode;
		}
	}
}
