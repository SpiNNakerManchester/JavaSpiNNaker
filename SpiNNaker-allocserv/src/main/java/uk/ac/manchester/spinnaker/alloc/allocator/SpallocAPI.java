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

import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.MAY_SEE_JOB_DETAILS;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.access.prepost.PostFilter;

import uk.ac.manchester.spinnaker.alloc.SecurityConfig.Permit;
import uk.ac.manchester.spinnaker.alloc.model.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.model.ConnectionInfo;
import uk.ac.manchester.spinnaker.alloc.model.DownLink;
import uk.ac.manchester.spinnaker.alloc.model.JobDescription;
import uk.ac.manchester.spinnaker.alloc.model.JobListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.model.MachineDescription;
import uk.ac.manchester.spinnaker.alloc.model.MachineListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;
import uk.ac.manchester.spinnaker.alloc.web.IssueReportRequest;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
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
	 */
	Map<String, Machine> getMachines();

	/**
	 * List the machines.
	 *
	 * @return A description of all the machines.
	 */
	List<MachineListEntryRecord> listMachines();

	/**
	 * Get a specific machine.
	 *
	 * @param name
	 *            The name of the machine to get.
	 * @return A machine, on which more operations can be done.
	 */
	Optional<Machine> getMachine(String name);

	/**
	 * Get info about a specific machine.
	 *
	 * @param machine
	 *            The name of the machine to get.
	 * @param permit
	 *            Encodes what the caller may do.
	 * @return A machine description model.
	 */
	Optional<MachineDescription> getMachineInfo(String machine, Permit permit);

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
	 */
	Jobs getJobs(boolean deleted, int limit, int start);

	/**
	 * List the active jobs.
	 *
	 * @param permit
	 *            Encodes what the caller may do.
	 * @return A description of all the active jobs.
	 */
	List<JobListEntryRecord> listJobs(Permit permit);

	/**
	 * Get a specific job. Only owners or admins can see full job details or
	 * manipulate the job.
	 *
	 * @param permit
	 *            Encodes what the caller may do.
	 * @param id
	 *            The identifier of the job.
	 * @return A job object on which more operations can be done, or empty if
	 *         the job isn't there or isn't available to you.
	 */
	@PostFilter(MAY_SEE_JOB_DETAILS)
	Optional<Job> getJob(Permit permit, int id);

	/**
	 * Get a specific job. Only owners or admins can see full job details or
	 * manipulate the job.
	 *
	 * @param permit
	 *            Encodes what the caller may do.
	 * @param id
	 *            The identifier of the job.
	 * @return A job description, or empty if the job isn't there (or isn't
	 *         available to you).
	 */
	@PostFilter(MAY_SEE_JOB_DETAILS)
	Optional<JobDescription> getJobInfo(Permit permit, int id);

	/**
	 * Create a job.
	 *
	 * @param owner
	 *            Who is making this job.
	 * @param descriptor
	 *            What sort of allocation is desired?
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
	 * @return Handle to the job, or {@code empty} if the job couldn't be made.
	 */
	Optional<Job> createJob(String owner, CreateDescriptor descriptor,
			String machineName, List<String> tags, Duration keepaliveInterval,
			Integer maxDeadBoards, byte[] originalRequest);

	/** Purge the cache of what boards are down. */
	void purgeDownCache();

	/**
	 * Describes what sort of request to create a job this is.
	 *
	 * @see CreateNumBoards
	 * @see CreateDimensions
	 * @see CreateBoard
	 */
	abstract class CreateDescriptor {
		/** Only known subclasses permitted. */
		private CreateDescriptor() {
		}
	}

	/**
	 * A request for a number of boards.
	 */
	final class CreateNumBoards extends CreateDescriptor {
		/** The number of boards requested. */
		public final int numBoards;

		public CreateNumBoards(int numBoards) {
			this.numBoards = numBoards;
		}
	}

	/**
	 * A request for a rectangle of boards.
	 */
	final class CreateDimensions extends CreateDescriptor {
		/** Width requested, in boards. */
		public final int width;

		/** Height requested, in boards. */
		public final int height;

		public CreateDimensions(int width, int height) {
			this.width = width;
			this.height = height;
		}
	}

	/**
	 * A request for a specific board.
	 */
	final class CreateBoard extends CreateDescriptor {
		static final class Triad {
			private Triad(int x, int y, int z) {
				this.x = x;
				this.y = y;
				this.z = z;
			}

			/** X coordinate. */
			public final int x;

			/** Y coordinate. */
			public final int y;

			/** Z coordinate. */
			public final int z;
		}

		static final class Phys {
			private Phys(int cabinet, int frame, int board) {
				this.cabinet = cabinet;
				this.frame = frame;
				this.board = board;
			}

			/** Cabinet number. */
			public final int cabinet;

			/** Frame number. */
			public final int frame;

			/** Board number. */
			public final int board;
		}

		/** The logical coordinates, or {@code null}. */
		public final Triad triad;

		/** The physical coordinates, or {@code null}. */
		public final Phys physical;

		/** The network coordinates, or {@code null}. */
		public final String ip;

		private CreateBoard(Triad triad, Phys physical, String ip) {
			this.triad = triad;
			this.physical = physical;
			this.ip = ip;
		}

		/**
		 * Create a request for a specific board.
		 *
		 * @param x
		 *            The X coordinate of the board.
		 * @param y
		 *            The Y coordinate of the board.
		 * @param z
		 *            The Z coordinate of the board.
		 * @return Descriptor
		 */
		public static CreateBoard triad(int x, int y, int z) {
			return new CreateBoard(new Triad(x, y, z), null, null);
		}

		/**
		 * Create a request for a specific board.
		 *
		 * @param cabinet
		 *            The cabinet number of the board.
		 * @param frame
		 *            The frame number of the board.
		 * @param board
		 *            The board number of the board.
		 * @return Descriptor
		 */
		public static CreateBoard physical(int cabinet, int frame, int board) {
			return new CreateBoard(null, new Phys(cabinet, frame, board), null);
		}

		/**
		 * Create a request for a specific board.
		 *
		 * @param ip
		 *            The network address of the board.
		 * @return Descriptor
		 */
		public static CreateBoard address(String ip) {
			return new CreateBoard(null, null, ip);
		}
	}

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
		 *            How long to wait.
		 */
		void waitForChange(Duration timeout);
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
		 */
		void access(String keepaliveAddress);

		/**
		 * Mark the job as destroyed. To do this to an already destroyed job is
		 * a no-op.
		 *
		 * @param reason
		 *            Why the job is being destroyed.
		 */
		void destroy(String reason);

		/**
		 * @return The state of the job.
		 */
		JobState getState();

		/**
		 * @return When the job started.
		 */
		Instant getStartTime();

		/**
		 * @return Host address that issued last keepalive event, if any.
		 */
		Optional<String> getKeepaliveHost();

		/**
		 * @return Time of the last keepalive event, if any.
		 */
		Instant getKeepaliveTimestamp();

		/**
		 * @return The creator of the job.
		 */
		Optional<String> getOwner();

		/**
		 * @return The serialized original request to create the job.
		 */
		Optional<byte[]> getOriginalRequest();

		/**
		 * @return When the job finished.
		 */
		Optional<Instant> getFinishTime();

		/**
		 * @return Why the job died. Might be {@code null} if this isn't known
		 *         (including if the job is alive).
		 */
		Optional<String> getReason();

		/**
		 * @return The (sub-)machine allocated to the job. {@code null} if no
		 *         resources allocated.
		 */
		Optional<SubMachine> getMachine();

		/**
		 * Locate a board within the allocation.
		 *
		 * @param x
		 *            The X coordinate of a chip on the board of interest.
		 * @param y
		 *            The Y coordinate of a chip on the board of interest.
		 * @return The location, if resources allocated and the location maps.
		 */
		Optional<BoardLocation> whereIs(int x, int y);

		/**
		 * @return The absolute location of root chip. {@code null} if no
		 *         resources allocated.
		 */
		Optional<ChipLocation> getRootChip();

		/**
		 * @return the allocated width of the job's rectangle, or {@code null}
		 *         if not allocated (or not known).
		 */
		Optional<Integer> getWidth();

		/**
		 * @return the allocated height of the job's rectangle, or {@code null}
		 *         if not allocated (or not known).
		 */
		Optional<Integer> getHeight();

		/**
		 * @return the allocated depth of this sub-machine, or {@code null} if
		 *         not allocated (or not known). When supplied, will be 1
		 *         (single board) or 3 (by triad)
		 */
		Optional<Integer> getDepth();

		/**
		 * Report an issue with some boards in the job.
		 *
		 * @param reqBody
		 *            The description of the issue.
		 * @param permit
		 *            Who is actually reporting this?
		 * @return The text part of the response
		 */
		String reportIssue(IssueReportRequest reqBody, Permit permit);
	}

	/**
	 * Describes a list of jobs known to the allocator.
	 *
	 * @author Donal Fellows
	 */
	interface Jobs extends Waitable {
		/**
		 * @return The job IDs.
		 */
		List<Integer> ids();

		/**
		 * @return The jobs. Simplified view only. (No keepalive host or owner
		 *         information.)
		 */
		List<Job> jobs();
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
		 */
		Set<String> getTags();

		/**
		 * @return The width of the machine.
		 */
		int getWidth();

		/**
		 * @return The height of the machine.
		 */
		int getHeight();

		/**
		 * The IDs of boards marked as dead or otherwise taken out of service.
		 *
		 * @return A list of boards. Not modifiable.
		 */
		List<BoardCoords> getDeadBoards();

		/**
		 * The links within the machine that are marked as dead or otherwise
		 * taken out of service. Note that this does not include links that lead
		 * out of the machine.
		 *
		 * @return A list of links. Not modifiable.
		 */
		List<DownLink> getDownLinks();

		/**
		 * Get a description of the location of a board given the global
		 * coordinates of a chip on it.
		 *
		 * @param x
		 *            Global chip X coordinate.
		 * @param y
		 *            Global chip Y coordinate.
		 * @return Board location description
		 */
		Optional<BoardLocation> getBoardByChip(int x, int y);

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
		 */
		Optional<BoardLocation> getBoardByPhysicalCoords(int cabinet, int frame,
				int board);

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
		 */
		Optional<BoardLocation> getBoardByLogicalCoords(int x, int y, int z);

		/**
		 * Get a description of the location of a board given the address of its
		 * ethernet chip.
		 *
		 * @param address
		 *            IP address of the board (in {@code 0.0.0.0} form; will be
		 *            matched exactly by the values in the DB).
		 * @return Board location description
		 */
		Optional<BoardLocation> getBoardByIPAddress(String address);

		/**
		 * @return The IP address of the BMP of the root board of the machine.
		 */
		String getRootBoardBMPAddress();

		/**
		 * @return The boards supported by the machine.
		 */
		List<Integer> getBoardNumbers();

		/**
		 * @return The IDs of boards currently available to be allocated.
		 */
		List<Integer> getAvailableBoards();

		/**
		 * Get the address of a particular BMP of a machine.
		 *
		 * @param bmp
		 *            The BMP coordinates (cabinet, frame).
		 * @return The IP address of the BMP.
		 */
		String getBMPAddress(BMPCoords bmp);

		/**
		 * Get the board numbers managed by a particular BMP of a machine.
		 *
		 * @param bmp
		 *            The BMP coordinates( cabinet, frame).
		 * @return The board numbers managed by that BMP.
		 */
		List<Integer> getBoardNumbers(BMPCoords bmp);
	}

	/**
	 * Describes the locations of boards in a machine. Note that instances of
	 * this class are expected to be fully instantiated; reading from them will
	 * <em>not</em> touch the database.
	 *
	 * @author Donal Fellows
	 */
	interface BoardLocation {
		/**
		 * Get the location of the characteristic chip of a board. This is
		 * usually the root chip of the board.
		 *
		 * @return chip location, in <em>global</em> (whole machine) coordinates
		 */
		ChipLocation getBoardChip();

		/**
		 * Get the location of this board location relative to another global
		 * location (the global location of the root chip of an allocation).
		 *
		 * @param rootChip
		 *            The global location of the root chip of an allocation that
		 *            we are converting this board location to be relative to.
		 * @return chip location, in <em>relative</em> (single job) coordinates
		 */
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
		 * @return a chip location, in <em>global</em> (whole machine)
		 *         coordinates
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
		 */
		Machine getMachine();

		/**
		 * @return The root X coordinate of this sub-machine.
		 */
		int getRootX();

		/**
		 * @return The root Y coordinate of this sub-machine.
		 */
		int getRootY();

		/**
		 * @return The root Z coordinate of this sub-machine.
		 */
		int getRootZ();

		/**
		 * @return The width of this sub-machine, in triads.
		 */
		int getWidth();

		/**
		 * @return The height of this sub-machine, in triads.
		 */
		int getHeight();

		/**
		 * @return The depth of this sub-machine. 1 (single board) or 3 (by
		 *         triad)
		 */
		int getDepth();

		/**
		 * @return The connection details of this sub-machine.
		 */
		List<ConnectionInfo> getConnections();

		/**
		 * @return The board locations of this sub-machine.
		 */
		List<BoardCoordinates> getBoards();

		/**
		 * @return Whether this sub-machine is switched on.
		 */
		PowerState getPower();

		/**
		 * Set whether this sub-machine is switched on. Note that actually
		 * changing the power of a sub-machine can take some time.
		 *
		 * @param powerState
		 *            What to set the power state to.
		 */
		void setPower(PowerState powerState);
	}
}
