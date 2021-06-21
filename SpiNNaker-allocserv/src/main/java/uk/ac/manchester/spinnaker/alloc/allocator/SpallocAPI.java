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

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonFormat;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

public interface SpallocAPI {
	/**
	 * List the machines.
	 *
	 * @throws SQLException
	 *             If something goes wrong
	 */
	Map<String, Machine> getMachines() throws SQLException;

	/**
	 * Get a specific machine.
	 *
	 * @throws SQLException
	 *             If something goes wrong
	 */
	Optional<Machine> getMachine(String name) throws SQLException;

	/**
	 * List the jobs.
	 *
	 * @throws SQLException
	 *             If something goes wrong
	 */
	Jobs getJobs(boolean deleted, int limit, int start) throws SQLException;

	/**
	 * Get a specific job.
	 *
	 * @throws SQLException
	 *             If something goes wrong
	 */
	Optional<Job> getJob(int id) throws SQLException;

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
		void access(String keepaliveAddress) throws SQLException;

		void destroy(String reason) throws SQLException;

		/** @return Job ID */
		int getId();

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
		String getKeepaliveHost() throws SQLException;

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
		String getOwner() throws SQLException;

		/**
		 * @return The serialized original request to create the job.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		byte[] getOriginalRequest() throws SQLException;

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
		 *         not allocated (or not known). When suppplied, will be 1
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
		 * @return The jobs. Simplified view only.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		List<Job> jobs() throws SQLException;
	}

	/**
	 * Basic coordinates of a board.
	 *
	 * @author Donal Fellows
	 */
	class BoardCoords {
		/** Logical triad X coordinate. */
		public final int x;

		/** Logical triad Y coordinate. */
		public final int y;

		/** Logical triad Z coordinate. */
		public final int z;

		/** Physical cabinet number. */
		public final int cabinet;

		/** Physical frame number. */
		public final int frame;

		/** Physical board number. */
		public final int board;

		/** IP address of ethernet chip. */
		public final String address;

		/**
		 * @param x
		 *            Logical triad X coordinate
		 * @param y
		 *            Logical triad Y coordinate
		 * @param z
		 *            Logical triad Z coordinate
		 * @param cabinet
		 *            Physical cabinet number
		 * @param frame
		 *            Physical frame number
		 * @param board
		 *            Physical board number
		 * @param address
		 *            IP address of ethernet chip
		 */
		public BoardCoords(int x, int y, int z, int cabinet, int frame,
				int board, String address) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.cabinet = cabinet;
			this.frame = frame;
			this.board = board;
			this.address = address;
		}
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

		/**
		 * The tags associated with the machine.
		 *
		 * @throws SQLException
		 *             If something goes wrong
		 */
		List<String> getTags() throws SQLException;

		/**
		 * The width of the machine.
		 *
		 * @throws SQLException
		 *             If something goes wrong
		 */
		int getWidth() throws SQLException;

		/**
		 * The height of the machine.
		 *
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
	 * Describes a link that is disabled.
	 *
	 * @author Donal Fellows
	 */
	@JsonFormat(shape = ARRAY)
	static class DownLink {
		/**
		 * Describes one end of a link that is disabled.
		 *
		 * @author Donal Fellows
		 */
		public static class End {
			private End() {
			}

			/**
			 * On what board is this end of the link.
			 */
			public BoardCoords board;

			/**
			 * In which direction does this end of the link go?
			 */
			public Direction direction;
		}

		public DownLink(BoardCoords board1, Direction dir1, BoardCoords board2,
				Direction dir2) {
			end1 = new End();
			end1.board = board1;
			end1.direction = dir1;
			end2 = new End();
			end2.board = board2;
			end2.direction = dir2;
		}

		/** One end of the down link. */
		public End end1;

		/** The other end of the down link. */
		public End end2;
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
		 * @param ps
		 *            What to set the power state to.
		 * @throws SQLException
		 *             If something goes wrong
		 */
		void setPower(PowerState ps) throws SQLException;
	}
}
