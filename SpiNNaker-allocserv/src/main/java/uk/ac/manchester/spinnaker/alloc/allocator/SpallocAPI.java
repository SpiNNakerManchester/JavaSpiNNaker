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

import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.IS_ADMIN;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.access.prepost.PostFilter;

import uk.ac.manchester.spinnaker.alloc.model.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.model.ConnectionInfo;
import uk.ac.manchester.spinnaker.alloc.model.DownLink;
import uk.ac.manchester.spinnaker.alloc.model.JobListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.model.MachineDescription;
import uk.ac.manchester.spinnaker.alloc.model.MachineListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

/**
 * The API of the core service that interacts with the database.
 *
 * @author Donal Fellows
 */
public interface SpallocAPI {
	/**
	 * List the machines.
	 *
	 * @return A mapping from names to machines (which are live objects).
	 * @throws SQLException
	 *             If something goes wrong
	 */
	Map<String, Machine> getMachines() throws SQLException;

	/**
	 * List the machines.
	 *
	 * @return A description of all the machines.
	 * @throws SQLException
	 *             If something goes wrong
	 */
	List<MachineListEntryRecord> listMachines() throws SQLException;

	/**
	 * Get a specific machine.
	 *
	 * @param name
	 *            The name of the machine to get.
	 * @return A machine, on which more operations can be done.
	 * @throws SQLException
	 *             If something goes wrong
	 */
	Optional<Machine> getMachine(String name) throws SQLException;

	/**
	 * Get info about a specific machine.
	 *
	 * @param machine
	 *            The name of the machine to get.
	 * @param currentUser
	 *            Who are we getting machine for.
	 * @param isAdmin
	 *            Does the user have administration authorisation?
	 * @return A machine description model.
	 * @throws SQLException
	 *             If something goes wrong
	 */
	Optional<MachineDescription> getMachineInfo(String machine,
			String currentUser, boolean isAdmin) throws SQLException;

	/**
	 * List the jobs.
	 *
	 * @param deleted
	 *            Whether to include deleted jobs in the list.
	 * @param limit
	 *            Maximum number of jobs in the returned list. Used for paging.
	 * @param start
	 *            How many jobs to skip past. Used for paging.
	 * @return A list of jobs.
	 * @throws SQLException
	 *             If something goes wrong
	 */
	Jobs getJobs(boolean deleted, int limit, int start) throws SQLException;

	/**
	 * List the active jobs.
	 *
	 * @param currentUser
	 *            Who are we listing for.
	 * @param isAdmin
	 *            Does the user have administration authorisation?
	 * @return A description of all the active jobs.
	 * @throws SQLException
	 *             If something goes wrong
	 */
	List<JobListEntryRecord> listJobs(String currentUser, boolean isAdmin)
			throws SQLException;

	/**
	 * Get a specific job. Only owners or admins can see full job details or
	 * manipulate the job.
	 *
	 * @param id
	 *            The identifier of the job.
	 * @return A job object on which more operations can be done, or empty if
	 *         the job isn't there or isn't available to you.
	 * @throws SQLException
	 *             If something goes wrong
	 */
	@PostFilter(IS_ADMIN
			+ " or filterObject.owner.orElse(null) == authentication.name")
	List<Job> getJob(int id) throws SQLException;

	/**
	 * Create a job.
	 *
	 * @param owner
	 *            Who is making this job.
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
	 * @param originalRequest
	 *            The serialized original request, which will be stored in the
	 *            database for later retrieval.
	 * @return Handle to the job.
	 * @throws SQLException
	 *             If anything goes wrong at the database level.
	 */
	Job createJob(String owner, List<Integer> dimensions, String machineName,
			List<String> tags, Duration keepaliveInterval,
			Integer maxDeadBoards, byte[] originalRequest) throws SQLException;

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
		/** @return Job ID */
		int getId();

		/**
		 * Update the keepalive.
		 *
		 * @param keepaliveAddress
		 *            Where was the access from.
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		void access(String keepaliveAddress) throws SQLException;

		/**
		 * Mark the job as destroyed. To do this to an already destroyed job is
		 * a no-op.
		 *
		 * @param reason
		 *            Why the job is being destroyed.
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		void destroy(String reason) throws SQLException;

		/**
		 * @return The state of the job.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		JobState getState() throws SQLException;

		/**
		 * @return When the job started.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Instant getStartTime() throws SQLException;

		/**
		 * @return Host address that issued last keepalive event, if any.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<String> getKeepaliveHost() throws SQLException;

		/**
		 * @return Time of the last keepalive event, if any.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Instant getKeepaliveTimestamp() throws SQLException;

		/**
		 * @return The creator of the job.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<String> getOwner() throws SQLException;

		/**
		 * @return The serialized original request to create the job.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<byte[]> getOriginalRequest() throws SQLException;

		/**
		 * @return When the job finished.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<Instant> getFinishTime() throws SQLException;

		/**
		 * @return Why the job died. Might be {@code null} if this isn't known
		 *         (including if the job is alive).
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<String> getReason() throws SQLException;

		/**
		 * @return The (sub-)machine allocated to the job. {@code null} if no
		 *         resources allocated.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<SubMachine> getMachine() throws SQLException;

		/**
		 * Locate a board within the allocation.
		 *
		 * @param x
		 *            The X coordinate of a chip on the board of interest.
		 * @param y
		 *            The Y coordinate of a chip on the board of interest.
		 * @return The location, if resources allocated and the location maps.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<BoardLocation> whereIs(int x, int y) throws SQLException;

		/**
		 * @return The absolute location of root chip. {@code null} if no
		 *         resources allocated.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<ChipLocation> getRootChip() throws SQLException;

		/**
		 * @return the allocated width of the job's rectangle, or {@code null}
		 *         if not allocated (or not known).
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<Integer> getWidth() throws SQLException;

		/**
		 * @return the allocated height of the job's rectangle, or {@code null}
		 *         if not allocated (or not known).
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<Integer> getHeight() throws SQLException;

		/**
		 * @return the allocated depth of this sub-machine, or {@code null} if
		 *         not allocated (or not known). When supplied, will be 1
		 *         (single board) or 3 (by triad)
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<Integer> getDepth() throws SQLException;
	}

	/**
	 * Describes a list of jobs known to the allocator.
	 *
	 * @author Donal Fellows
	 */
	interface Jobs extends Waitable {
		/**
		 * @return The job IDs.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		List<Integer> ids() throws SQLException;

		/**
		 * @return The jobs. Simplified view only. (No keepalive host or owner
		 *         information.)
		 * @throws SQLException
		 *             If something goes wrong
		 */
		List<Job> jobs() throws SQLException;
	}

	/**
	 * Describes a particular machine known to the allocator. Must implement
	 * equality by ID or name (both are unique).
	 *
	 * @author Donal Fellows
	 */
	interface Machine extends Waitable {
		/** @return The ID of the machine. Unique. */
		int getId();

		/** @return The name of the machine. Unique. */
		String getName();

		/**
		 * @return The tags associated with the machine.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		List<String> getTags() throws SQLException;

		/**
		 * @return The width of the machine.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		int getWidth() throws SQLException;

		/**
		 * @return The height of the machine.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		int getHeight() throws SQLException;

		/**
		 * The IDs of boards marked as dead or otherwise taken out of service.
		 *
		 * @return A list of boards. Not modifiable.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		List<BoardCoords> getDeadBoards() throws SQLException;

		/**
		 * The links within the machine that are marked as dead or otherwise
		 * taken out of service. Note that this does not include links that lead
		 * out of the machine.
		 *
		 * @return A list of links. Not modifiable.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		List<DownLink> getDownLinks() throws SQLException;

		/**
		 * Get a description of the location of a board given the global
		 * coordinates of a chip on it.
		 *
		 * @param x
		 *            Global chip X coordinate.
		 * @param y
		 *            Global chip Y coordinate.
		 * @return Board location description
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<BoardLocation> getBoardByChip(int x, int y)
				throws SQLException;

		/**
		 * Get a description of the location of a board given the physical
		 * coordinates of the board.
		 *
		 * @param cabinet
		 *            Cabinet number
		 * @param frame
		 *            Frame number
		 * @param board
		 *            Board number
		 * @return Board location description
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<BoardLocation> getBoardByPhysicalCoords(int cabinet, int frame,
				int board) throws SQLException;

		/**
		 * Get a description of the location of a board given the triad
		 * coordinates of the board.
		 *
		 * @param x
		 *            Triad X coordinate.
		 * @param y
		 *            Triad Y coordinate.
		 * @param z
		 *            Triad Z coordinate.
		 * @return Board location description
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<BoardLocation> getBoardByLogicalCoords(int x, int y, int z)
				throws SQLException;

		/**
		 * Get a description of the location of a board given the address of its
		 * ethernet chip.
		 *
		 * @param address
		 *            IP address of the board (in {@code 0.0.0.0} form; will be
		 *            matched exactly by the values in the DB).
		 * @return Board location description
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Optional<BoardLocation> getBoardByIPAddress(String address)
				throws SQLException;

		/**
		 * @return The IP address of the BMP of the root board of the machine.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		String getRootBoardBMPAddress() throws SQLException;

		/**
		 * @return The boards supported by the machine.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		List<Integer> getBoardNumbers() throws SQLException;

		/**
		 * @return The IDs of boards currently available to be allocated.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		List<Integer> getAvailableBoards() throws SQLException;
	}

	/**
	 * Describes the locations of boards in a machine. Note that instances of
	 * this class are expected to be fully instantiated; reading from them will
	 * <em>not</em> touch the database.
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

		/**
		 * Where is the board logically within its machine?
		 *
		 * @return a triad location descriptor
		 */
		BoardCoordinates getLogical();

		/**
		 * Where is the board physically in its machine?
		 *
		 * @return a cabinet/frame/board triple
		 */
		BoardPhysicalCoordinates getPhysical();

		/**
		 * Where is the chip of interest? Usually the root chip of the board.
		 *
		 * @return a chip location
		 */
		ChipLocation getChip();

		/**
		 * What job is the board allocated to? May be {@code null} for an
		 * unallocated board.
		 *
		 * @return a limited version of a job.
		 */
		Job getJob();
	}

	interface SubMachine {
		/**
		 * @return The machine that this sub-machine is part of.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		Machine getMachine() throws SQLException;

		/**
		 * @return The root X coordinate of this sub-machine.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		int getRootX() throws SQLException;

		/**
		 * @return The root Y coordinate of this sub-machine.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		int getRootY() throws SQLException;

		/**
		 * @return The root Z coordinate of this sub-machine.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		int getRootZ() throws SQLException;

		/**
		 * @return The width of this sub-machine, in triads.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		int getWidth() throws SQLException;

		/**
		 * @return The height of this sub-machine, in triads.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		int getHeight() throws SQLException;

		/**
		 * @return The depth of this sub-machine. 1 (single board) or 3 (by
		 *         triad)
		 * @throws SQLException
		 *             If something goes wrong
		 */
		int getDepth() throws SQLException;

		/**
		 * @return The connection details of this sub-machine.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		List<ConnectionInfo> getConnections() throws SQLException;

		/**
		 * @return The board locations of this sub-machine.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		List<BoardCoordinates> getBoards() throws SQLException;

		/**
		 * @return Whether this sub-machine is switched on.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		PowerState getPower() throws SQLException;

		/**
		 * Set whether this sub-machine is switched on. Note that actually
		 * changing the power of a sub-machine can take some time.
		 *
		 * @param powerState
		 *            What to set the power state to.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		void setPower(PowerState powerState) throws SQLException;
	}
}
