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
package uk.ac.manchester.spinnaker.alloc.allocator;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.JobsEpoch;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

public interface SpallocAPI {
	/** List the machines. */
	Map<String, Machine> getMachines() throws SQLException;

	/** Get a specific machine. */
	Machine getMachine(String name) throws SQLException;

	/** List the jobs. */
	Jobs getJobs(boolean deleted, int limit, int start) throws SQLException;

	/** Get a specific job. */
	Job getJob(int id) throws SQLException;

	/**
	 * Create a job.
	 *
	 * @param owner
	 *            Who is making this job. Note that this is a self-asserted
	 *            identity.
	 * @param dimensions
	 *            List of dimensions. At least one element. No more than three.
	 *            No negative elements.
	 * @param machineName
	 *            The name of the machine the user wants to allocate on, or
	 *            {@code null} if they want to select by tags.
	 * @param tags
	 *            The tags of the machine the user wants to allocate on, or
	 *            {@code null} if they want to select by name.
	 * @param keepaliveInterval
	 *            The maximum interval between keepalive requests or the job
	 *            becomes eligible for automated deletion.
	 * @param maxDeadBoards
	 *            The maximum number of dead boards tolerated in the allocation.
	 *            Ignored when asking for a single board.
	 * @return Handle to the job.
	 * @throws SQLException
	 *             If anything goes wrong at the database level.
	 */
	Job createJob(String owner, List<Integer> dimensions, String machineName,
			List<String> tags, Duration keepaliveInterval,
			Integer maxDeadBoards) throws SQLException;

	/**
	 * A thing that may be waited upon.
	 *
	 * @author Donal Fellows
	 */
	interface Waitable {
		/**
		 * Wait for the object to (maybe) change, or for the timeout to expire.
		 * This is a best-effort method.
		 * <p>
		 * This method does <em>not</em> throw {@link InterruptedException}; on
		 * interruption, it simply returns early (but the
		 * {@linkplain Thread#interrupted() interrupted status} of the thread is
		 * set).
		 *
		 * @param timeout
		 *            How long to wait (in milliseconds).
		 */
		void waitForChange(long timeout);
	}

	/**
	 * Describes a particular job known to the allocator.
	 *
	 * @author Donal Fellows
	 */
	interface Job extends Waitable {
		void access(String keepaliveAddress) throws SQLException;

		void destroy(String reason) throws SQLException;

		/** @return Job ID */
		int getId();

		/** @return The state of the job. */
		JobState getState() throws SQLException;

		/** @return When the job started. */
		Instant getStartTime() throws SQLException;

		/** @return When the job finished. */
		Instant getFinishTime();

		/**
		 * @return Why the job died. Might be {@code null} if this isn't known
		 *         (including if the job is alive).
		 */
		String getReason() throws SQLException;

		/** @return Host address that issued last keepalive event, if any. */
		String getKeepaliveHost() throws SQLException;

		/** @return Time of the last keepalive event, if any. */
		Instant getKeepaliveTimestamp() throws SQLException;

		/**
		 * @return The (sub-)machine allocated to the job. {@code null} if no
		 *         resources allocated.
		 */
		SubMachine getMachine() throws SQLException;

		/**
		 * Locate a board within the allocation.
		 *
		 * @return The location, or {@code null} if no resources allocated.
		 */
		BoardLocation whereIs(int x, int y) throws SQLException;

		/**
		 * @return The absolute location of root chip. {@code null} if no
		 *         resources allocated.
		 */
		ChipLocation getRootChip() throws SQLException;

		/** @return The creator of the job. */
		String getOwner() throws SQLException;

		/**
		 * @return the allocated width of the job's rectangle, or {@code null}
		 *         if not allocated (or not known).
		 */
		Integer getWidth() throws SQLException;

		/**
		 * @return the allocated height of the job's rectangle, or {@code null}
		 *         if not allocated (or not known).
		 */
		Integer getHeight() throws SQLException;

		/**
		 * @return the allocated depth of this sub-machine, or {@code null} if
		 *         not allocated (or not known). When suppplied, will be 1
		 *         (single board) or 3 (by triad)
		 */
		Integer getDepth() throws SQLException;
	}

	/**
	 * Describes a list of jobs known to the allocator.
	 *
	 * @author Donal Fellows
	 */
	interface Jobs extends Waitable {
		/** The job IDs. */
		List<Integer> ids() throws SQLException;

		/** The jobs. Simplified view only */
		List<Job> jobs() throws SQLException;
	}

	/**
	 * Describes a particular machine known to the allocator. Must implement
	 * equality by ID or name (both are unique).
	 *
	 * @author Donal Fellows
	 */
	interface Machine extends Waitable {
		/** The ID of the machine. Unique. */
		int getId();

		/** The name of the machine. Unique. */
		String getName();

		/** The tags associated with the machine. */
		List<String> getTags() throws SQLException;

		/** The width of the machine. */
		int getWidth() throws SQLException;

		/** The height of the machine. */
		int getHeight() throws SQLException;

		/**
		 * The IDs of boards marked as dead or otherwise taken out of service.
		 *
		 * @throws SQLException
		 *             If something goes wrong
		 */
		List<Integer> getDeadBoards() throws SQLException;

		// TODO: dead links

		Optional<BoardLocation> getBoardByChip(int x, int y, JobsEpoch je)
				throws SQLException;

		Optional<BoardLocation> getBoardByPhysicalCoords(int cabinet, int frame,
				int board, JobsEpoch je) throws SQLException;

		Optional<BoardLocation> getBoardByLogicalCoords(int x, int y, int z,
				JobsEpoch je) throws SQLException;

		String getRootBoardBMPAddress() throws SQLException;

		/**
		 * @return The boards supported by the machine.
		 *
		 * @throws SQLException
		 *             If something goes wrong
		 */
		List<Integer> getBoardNumbers() throws SQLException;

		/**
		 * The IDs of boards currently available to be allocated.
		 *
		 * @throws SQLException
		 *             If something goes wrong
		 */
		List<Integer> getAvailableBoards() throws SQLException;
	}

	/**
	 * Describes the locations of boards in a machine.
	 *
	 * @author Donal Fellows
	 */
	interface BoardLocation {
		ChipLocation getBoardChip();

		ChipLocation getChipRelativeTo(ChipLocation rootChip);

		/**
		 * What machine is the board on?
		 *
		 * @return name of machine
		 */
		String getMachine();

		/** Where is the board logically within its machine? */
		BoardCoordinates getLogical();

		/** Where is the board physically in its machine? */
		BoardPhysicalCoordinates getPhysical();

		/**
		 * Where is the chip of interest? Usually the root chip of the board.
		 */
		ChipLocation getChip();

		/**
		 * What job is the board allocated to? May be {@code null} for an
		 * unallocated board.
		 */
		Job getJob();
	}

	interface SubMachine {
		/**
		 * @return The machine that this sub-machine is part of.
		 */
		Machine getMachine() throws SQLException;

		/**
		 * @return The root X coordinate of this sub-machine.
		 */
		int getRootX() throws SQLException;

		/**
		 * @return The root Y coordinate of this sub-machine.
		 */
		int getRootY() throws SQLException;

		/**
		 * @return The root Z coordinate of this sub-machine.
		 */
		int getRootZ() throws SQLException;

		/**
		 * @return The width of this sub-machine, in triads.
		 */
		int getWidth() throws SQLException;

		/**
		 * @return The height of this sub-machine, in triads.
		 */
		int getHeight() throws SQLException;

		/**
		 * @return The depth of this sub-machine. 1 (single board) or 3 (by
		 *         triad)
		 */
		int getDepth() throws SQLException;

		/**
		 * @return The connection details of this sub-machine.
		 */
		List<ConnectionInfo> getConnections() throws SQLException;

		/**
		 * @return The board locations of this sub-machine.
		 */
		List<BoardCoordinates> getBoards() throws SQLException;

		/** @return Whether this sub-machine is switched on. */
		PowerState getPower() throws SQLException;

		/**
		 * Set whether this sub-machine is switched on. Note that actually changing
		 * the power of a sub-machine can take some time.
		 *
		 * @param ps
		 *            What to set the power state to.
		 */
		void setPower(PowerState ps) throws SQLException;
	}
}
