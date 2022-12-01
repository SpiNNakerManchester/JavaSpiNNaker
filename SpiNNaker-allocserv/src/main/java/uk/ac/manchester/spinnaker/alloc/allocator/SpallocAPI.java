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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.alloc.Constants.TRIAD_DEPTH;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.MAY_SEE_JOB_DETAILS;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.springframework.security.access.prepost.PostFilter;

import com.google.errorprone.annotations.Keep;

import uk.ac.manchester.spinnaker.alloc.compat.V1CompatService;
import uk.ac.manchester.spinnaker.alloc.model.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.model.ConnectionInfo;
import uk.ac.manchester.spinnaker.alloc.model.DownLink;
import uk.ac.manchester.spinnaker.alloc.model.JobDescription;
import uk.ac.manchester.spinnaker.alloc.model.JobListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.model.MachineDescription;
import uk.ac.manchester.spinnaker.alloc.model.MachineListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;
import uk.ac.manchester.spinnaker.alloc.proxy.ProxyCore;
import uk.ac.manchester.spinnaker.alloc.security.Permit;
import uk.ac.manchester.spinnaker.alloc.web.IssueReportRequest;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.ValidX;
import uk.ac.manchester.spinnaker.machine.ValidY;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadHeight;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadWidth;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * The API of the core service that interacts with the database.
 *
 * @author Donal Fellows
 */
public interface SpallocAPI {
	/**
	 * List the machines.
	 *
	 * @param allowOutOfService
	 *            Whether to include machines marked as out of service.
	 * @return A mapping from names to machines (which are live objects).
	 */
	Map<String, Machine> getMachines(boolean allowOutOfService);

	/**
	 * List the machines.
	 *
	 * @param allowOutOfService
	 *            Whether to include machines marked as out of service.
	 * @return A description of all the machines.
	 */
	List<MachineListEntryRecord> listMachines(boolean allowOutOfService);

	/**
	 * Get a specific machine.
	 *
	 * @param name
	 *            The name of the machine to get.
	 * @param allowOutOfService
	 *            Whether to include machines marked as out of service.
	 * @return A machine, on which more operations can be done.
	 */
	Optional<Machine> getMachine(@NotNull String name,
			boolean allowOutOfService);

	/**
	 * Get info about a specific machine.
	 *
	 * @param machine
	 *            The name of the machine to get.
	 * @param allowOutOfService
	 *            Whether to include machines marked as out of service.
	 * @param permit
	 *            Encodes what the caller may do.
	 * @return A machine description model.
	 */
	Optional<MachineDescription> getMachineInfo(@NotNull String machine,
			boolean allowOutOfService, Permit permit);

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
	 * Create a job. Note that jobs <em>cannot</em> be created on machines that
	 * are out of service, but marking a machine as out of service does not stop
	 * the jobs that are already running on it.
	 *
	 * @param owner
	 *            Who is making this job.
	 * @param group
	 *            What group is associated with this job for accounting
	 *            purposes. If {@code null} then a guess is made based on the
	 *            owner's memberships (i.e., if the owner is a member of a
	 *            single group, with it being an error for a job to be submitted
	 *            where there is a choice of groups available yet the job
	 *            doesn't say which to use).
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
	 * @param originalRequest
	 *            The serialized original request, which will be stored in the
	 *            database for later retrieval.
	 * @return Handle to the job, or {@code empty} if the job couldn't be made.
	 */
	Optional<Job> createJob(@NotNull String owner, String group,
			@Valid CreateDescriptor descriptor, String machineName,
			List<String> tags, Duration keepaliveInterval,
			byte[] originalRequest);

	/** Purge the cache of what boards are down. */
	void purgeDownCache();

	/**
	 * Tells the service that there may be a problem with a board at a
	 * particular address.
	 *
	 * @param address
	 *            The IP address of the board. Note that we haven't yet
	 *            identified which machine has the board.
	 * @param coreLocation
	 *            Where on the board is the problem. If the problem is at the
	 *            core level, it's a {@link HasCoreLocation}. If the problem is
	 *            at the board level, this is {@code null}.
	 * @param description
	 *            Optional problem description. May be {@code null}.
	 * @param permit
	 *            Who is making the request.
	 */
	@UsedInJavadocOnly(HasCoreLocation.class)
	void reportProblem(@IPAddress String address, HasChipLocation coreLocation,
			String description, Permit permit);

	/**
	 * Describes what sort of request to create a job this is.
	 *
	 * @see CreateNumBoards
	 * @see CreateDimensions
	 * @see CreateDimensionsAt
	 * @see CreateBoard
	 */
	abstract sealed class CreateDescriptor
			permits CreateDimensions, CreateNumBoards, HasBoardCoords {
		/**
		 * The maximum number of dead boards tolerated in the allocation.
		 * Ignored when asking for a single board.
		 */
		@PositiveOrZero(message = "maxDeadBoards must not be negative")
		public final int maxDead;

		/**
		 * Only known subclasses permitted.
		 *
		 * @param maxDeadBoards
		 *            The maximum number of dead boards. {@code null} is
		 *            equivalent to 0.
		 */
		private CreateDescriptor(Integer maxDeadBoards) {
			this.maxDead = isNull(maxDeadBoards) ? 0 : maxDeadBoards;
		}

		/**
		 * Apply a visitor to this descriptor.
		 *
		 * @param <T>
		 *            The type of the result of the visiting.
		 * @param visitor
		 *            The visitor to apply.
		 * @return The result computed by the visitor.
		 */
		public final <T> T visit(CreateVisitor<T> visitor) {
			return doVisit(requireNonNull(visitor));
		}

		/**
		 * Get the area. The area is the number of boards required.
		 *
		 * @return The number of boards requested by the job.
		 */
		public abstract int getArea();

		abstract <T> T doVisit(CreateVisitor<T> visitor);
	}

	/**
	 * A request for a number of boards.
	 */
	final class CreateNumBoards extends CreateDescriptor {
		/** The number of boards requested. */
		@Positive(message = "number of boards to request must be positive")
		public final int numBoards;

		/**
		 * Request a count of boards. The service <em>may</em> over-allocate.
		 *
		 * @param numBoards
		 *            The number of boards desired.
		 * @param maxDeadBoards
		 *            The number of dead boards that can be tolerated within
		 *            that.
		 */
		public CreateNumBoards(int numBoards, Integer maxDeadBoards) {
			super(maxDeadBoards);
			this.numBoards = numBoards;
		}

		@Override
		<T> T doVisit(CreateVisitor<T> visitor) {
			return visitor.numBoards(this);
		}

		@Override
		public int getArea() {
			return numBoards;
		}
	}

	/**
	 * A request for a rectangle of boards. This is expressed in boards for
	 * reasons relating to the legacy API.
	 *
	 * @see V1CompatService
	 */
	@UsedInJavadocOnly(V1CompatService.class)
	final class CreateDimensions extends CreateDescriptor {
		/** Width requested, in triads. */
		@ValidTriadWidth
		public final int width;

		/** Height requested, in triads. */
		@ValidTriadHeight
		public final int height;

		/**
		 * Request a rectangle of boards. The service <em>may</em>
		 * over-allocate.
		 *
		 * @param width
		 *            The width of rectangle to request, in triads.
		 * @param height
		 *            The height of rectangle to request, in triads.
		 * @param maxDeadBoards
		 *            The number of dead boards that can be tolerated in that
		 *            rectangle.
		 */
		public CreateDimensions(int width, int height, Integer maxDeadBoards) {
			super(maxDeadBoards);
			this.width = width;
			this.height = height;
		}

		@Override
		<T> T doVisit(CreateVisitor<T> visitor) {
			return visitor.dimensions(this);
		}

		@Override
		public int getArea() {
			return width * height;
		}
	}

	/** Some requests have the locations of boards. */
	abstract sealed class HasBoardCoords
			extends CreateDescriptor permits CreateDimensionsAt, CreateBoard {
		/** The logical coordinates, or {@code null}. */
		@Valid
		public final TriadCoords triad;

		/** The physical coordinates, or {@code null}. */
		@Valid
		public final PhysicalCoords physical;

		/** The network coordinates, or {@code null}. */
		@IPAddress(nullOK = true)
		public final String ip;

		private HasBoardCoords(TriadCoords triad, PhysicalCoords physical,
				String ip, Integer maxDeadBoards) {
			super(maxDeadBoards);
			this.triad = triad;
			this.physical = physical;
			this.ip = ip;
		}

		private static int get(Integer value) {
			return Objects.nonNull(value) ? value : 0;
		}

		@Keep
		@AssertTrue(message = "a method of locating a board must be provided")
		private boolean isLocated() {
			return nonNull(triad) || nonNull(physical) || nonNull(ip);
		}
	}

	/**
	 * A request for a rectangle of triads rooted at a particular triad. No
	 * option for using physical coordinates is supported with this method.
	 */
	final class CreateDimensionsAt extends HasBoardCoords {
		/** Width requested, in triads. */
		@ValidTriadWidth
		public final int width;

		/** Height requested, in triads. */
		@ValidTriadHeight
		public final int height;

		/**
		 * Create a request for a rectangle at a specific board. The board will
		 * have a Z coordinate of 0.
		 *
		 * @param width
		 *            Width requested, in triads.
		 * @param height
		 *            Height requested, in triads.
		 * @param x
		 *            The X coordinate of the root board of the request.
		 * @param y
		 *            The Y coordinate of the root board of the request.
		 * @param maxDeadBoards
		 *            The maximum number of dead boards tolerated in the
		 *            allocation. Ignored when asking for a single board.
		 */
		public CreateDimensionsAt(int width, int height, int x, int y,
				Integer maxDeadBoards) {
			super(new TriadCoords(x, y, 0), null, null, maxDeadBoards);
			this.width = width;
			this.height = height;
		}

		/**
		 * Create a request for a rectangle at a specific board. The board will
		 * have a Z coordinate of 0.
		 *
		 * @param width
		 *            Width requested, in triads.
		 * @param height
		 *            Height requested, in triads.
		 * @param x
		 *            The X coordinate of the root board of the request.
		 * @param y
		 *            The Y coordinate of the root board of the request.
		 * @param maxDeadBoards
		 *            The maximum number of dead boards tolerated in the
		 *            allocation. Ignored when asking for a single board.
		 */
		public CreateDimensionsAt(int width, int height, Integer x, Integer y,
				Integer maxDeadBoards) {
			super(new TriadCoords(HasBoardCoords.get(x), HasBoardCoords.get(y),
					0), null, null, maxDeadBoards);
			this.width = width;
			this.height = height;
		}

		/**
		 * Create a request for a rectangle at a specific board. The board must
		 * have a Z coordinate of 0.
		 *
		 * @param width
		 *            Width requested, in triads.
		 * @param height
		 *            Height requested, in triads.
		 * @param x
		 *            The X coordinate of the root board of the request.
		 * @param y
		 *            The Y coordinate of the root board of the request.
		 * @param z
		 *            The Z coordinate of the root board of the request.
		 * @param maxDeadBoards
		 *            The maximum number of dead boards tolerated in the
		 *            allocation. Ignored when asking for a single board.
		 */
		public CreateDimensionsAt(int width, int height, int x, int y, int z,
				Integer maxDeadBoards) {
			super(new TriadCoords(x, y, z), null, null, maxDeadBoards);
			this.width = width;
			this.height = height;
		}

		/**
		 * Create a request for a rectangle at a specific board. The board must
		 * have a Z coordinate of 0.
		 *
		 * @param width
		 *            Width requested, in triads.
		 * @param height
		 *            Height requested, in triads.
		 * @param x
		 *            The X coordinate of the root board of the request.
		 * @param y
		 *            The Y coordinate of the root board of the request.
		 * @param z
		 *            The Z coordinate of the root board of the request.
		 * @param maxDeadBoards
		 *            The maximum number of dead boards tolerated in the
		 *            allocation. Ignored when asking for a single board.
		 */
		public CreateDimensionsAt(int width, int height, Integer x, Integer y,
				Integer z, Integer maxDeadBoards) {
			super(new TriadCoords(HasBoardCoords.get(x), HasBoardCoords.get(y),
					HasBoardCoords.get(z)), null, null, maxDeadBoards);
			this.width = width;
			this.height = height;
		}

		/**
		 * Create a request for a rectangle at a specific board. The board must
		 * have a Z coordinate of 0.
		 *
		 * @param width
		 *            Width requested, in triads.
		 * @param height
		 *            Height requested, in triads.
		 * @param ip
		 *            The network address of the root board of the request.
		 * @param maxDeadBoards
		 *            The maximum number of dead boards tolerated in the
		 *            allocation. Ignored when asking for a single board.
		 */
		public CreateDimensionsAt(int width, int height, String ip,
				Integer maxDeadBoards) {
			super(null, null, ip, maxDeadBoards);
			this.width = width;
			this.height = height;
		}

		private CreateDimensionsAt(int width, int height,
				PhysicalCoords physical, Integer maxDeadBoards) {
			super(null, physical, null, maxDeadBoards);
			this.width = width;
			this.height = height;
		}

		/**
		 * Create a request for a rectangle at a specific board. The board must
		 * have a Z coordinate of 0.
		 *
		 * @param width
		 *            Width requested, in triads.
		 * @param height
		 *            Height requested, in triads.
		 * @param cabinet
		 *            The cabinet number of the root board of the request.
		 * @param frame
		 *            The frame number of the root board.
		 * @param board
		 *            The board number of the root board.
		 * @param maxDeadBoards
		 *            The maximum number of dead boards tolerated in the
		 *            allocation. Ignored when asking for a single board.
		 * @return Descriptor
		 */
		public static CreateDimensionsAt physical(int width, int height,
				int cabinet, int frame, int board, Integer maxDeadBoards) {
			// Done like this to avoid syntactic ambiguity
			return new CreateDimensionsAt(width, height,
					new PhysicalCoords(cabinet, frame, board), maxDeadBoards);
		}

		/**
		 * Create a request for a rectangle at a specific board. The board must
		 * have a Z coordinate of 0.
		 *
		 * @param width
		 *            Width requested, in triads.
		 * @param height
		 *            Height requested, in triads.
		 * @param cabinet
		 *            The cabinet number of the root board of the request.
		 * @param frame
		 *            The frame number of the root board.
		 * @param board
		 *            The board number of the root board.
		 * @param maxDeadBoards
		 *            The maximum number of dead boards tolerated in the
		 *            allocation. Ignored when asking for a single board.
		 * @return Descriptor
		 */
		public static CreateDimensionsAt physical(int width, int height,
				Integer cabinet, Integer frame, Integer board,
				Integer maxDeadBoards) {
			// Done like this to avoid syntactic ambiguity
			return new CreateDimensionsAt(width, height,
					new PhysicalCoords(HasBoardCoords.get(cabinet),
							HasBoardCoords.get(frame),
							HasBoardCoords.get(board)),
					maxDeadBoards);
		}

		@Override
		<T> T doVisit(CreateVisitor<T> visitor) {
			return visitor.dimensionsAt(this);
		}

		@Override
		public int getArea() {
			return width * height * TRIAD_DEPTH;
		}
	}

	/**
	 * A request for a specific board.
	 */
	final class CreateBoard extends HasBoardCoords {
		private CreateBoard(TriadCoords triad, PhysicalCoords physical,
				String ip) {
			super(triad, physical, ip, null);
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
			return new CreateBoard(new TriadCoords(x, y, z), null, null);
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
			return new CreateBoard(null,
					new PhysicalCoords(cabinet, frame, board), null);
		}

		/**
		 * Create a request for a specific board.
		 *
		 * @param ip
		 *            The network address of the board.
		 * @return Descriptor
		 */
		public static CreateBoard address(@IPAddress String ip) {
			return new CreateBoard(null, null, ip);
		}

		@Override
		<T> T doVisit(CreateVisitor<T> visitor) {
			return visitor.board(this);
		}

		@Override
		public int getArea() {
			return 1;
		}
	}

	/**
	 * Visitor for {@link CreateDescriptor}.
	 *
	 * @param <T>
	 *            The type of the result of visiting.
	 */
	interface CreateVisitor<T> {
		/**
		 * Visit a descriptor.
		 *
		 * @param createNumBoards
		 *            The descriptor.
		 * @return The result of the visiting.
		 */
		T numBoards(@NotNull CreateNumBoards createNumBoards);

		/**
		 * Visit a descriptor.
		 *
		 * @param createDimensionsAt
		 *            The descriptor.
		 * @return The result of the visiting.
		 */
		T dimensionsAt(@NotNull CreateDimensionsAt createDimensionsAt);

		/**
		 * Visit a descriptor.
		 *
		 * @param createDimensions
		 *            The descriptor.
		 * @return The result of the visiting.
		 */
		T dimensions(@NotNull CreateDimensions createDimensions);

		/**
		 * Visit a descriptor.
		 *
		 * @param createBoard
		 *            The descriptor.
		 * @return The result of the visiting.
		 */
		T board(@NotNull CreateBoard createBoard);
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
		void waitForChange(@NotNull Duration timeout);
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
		void access(@NotNull @IPAddress String keepaliveAddress);

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
		Optional<BoardLocation> whereIs(@ValidX int x, @ValidY int y);

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

		/**
		 * Note that a proxy has been set up for the job. This allows the proxy
		 * to be closed when the job state changes. (The proxy may already be
		 * closed at that point.)
		 *
		 * @param proxy
		 *            The proxy.
		 */
		void rememberProxy(ProxyCore proxy);

		/**
		 * Note that a proxy has been dropped from the job and doesn't need to
		 * be remembered any more.
		 *
		 * @param proxy
		 *            The proxy.
		 */
		void forgetProxy(ProxyCore proxy);
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

		/** @return Whether this machine is currently in service. */
		boolean isInService();

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
		 * @param chipLocation
		 *            Global chip coordinates.
		 * @return Board location description
		 */
		Optional<BoardLocation> getBoardByChip(
				@Valid @NotNull HasChipLocation chipLocation);

		/**
		 * Get a description of the location of a board given the physical
		 * coordinates of the board.
		 *
		 * @param coords
		 *            PhysicalCoordinates
		 * @return Board location description
		 */
		Optional<BoardLocation> getBoardByPhysicalCoords(
				@Valid @NotNull PhysicalCoords coords);

		/**
		 * Get a description of the location of a board given the triad
		 * coordinates of the board.
		 *
		 * @param coords
		 *            Triad coordinates.
		 * @return Board location description
		 */
		Optional<BoardLocation> getBoardByLogicalCoords(
				@Valid @NotNull TriadCoords coords);

		/**
		 * Get a description of the location of a board given the address of its
		 * ethernet chip.
		 *
		 * @param address
		 *            IP address of the board (in {@code 0.0.0.0} form; will be
		 *            matched exactly by the values in the DB).
		 * @return Board location description
		 */
		Optional<BoardLocation> getBoardByIPAddress(@IPAddress String address);

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
		String getBMPAddress(@Valid BMPCoords bmp);

		/**
		 * Get the board numbers managed by a particular BMP of a machine.
		 *
		 * @param bmp
		 *            The BMP coordinates( cabinet, frame).
		 * @return The board numbers managed by that BMP.
		 */
		List<Integer> getBoardNumbers(@Valid BMPCoords bmp);
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
		ChipLocation getChipRelativeTo(@NotNull ChipLocation rootChip);

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

	/** A view of part of a machine that is allocated to a job. */
	interface SubMachine {
		/** @return The machine that this sub-machine is part of. */
		Machine getMachine();

		/** @return The root X coordinate of this sub-machine. */
		int getRootX();

		/** @return The root Y coordinate of this sub-machine. */
		int getRootY();

		/** @return The root Z coordinate of this sub-machine. */
		int getRootZ();

		/** @return The width of this sub-machine, in triads. */
		int getWidth();

		/** @return The height of this sub-machine, in triads. */
		int getHeight();

		/**
		 * @return The depth of this sub-machine. 1 (single board) or 3 (by
		 *         triad)
		 */
		int getDepth();

		/** @return The connection details of this sub-machine. */
		List<ConnectionInfo> getConnections();

		/** @return The board locations of this sub-machine. */
		List<BoardCoordinates> getBoards();

		/** @return Whether this sub-machine is switched on. */
		PowerState getPower();

		/**
		 * Set whether this sub-machine is switched on. Note that actually
		 * changing the power of a sub-machine can take some time.
		 *
		 * @param powerState
		 *            What to set the power state to.
		 */
		void setPower(@NotNull PowerState powerState);
	}
}
