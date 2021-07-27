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

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;

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
		 * @throws IOException
		 *             If communication with the server fails
		 */
		WhereIs getBoardByTriad(int x, int y, int z) throws IOException;

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
		 * @throws IOException
		 *             If communication with the server fails
		 */
		WhereIs getBoardByPhysicalCoords(int cabinet, int frame, int board)
				throws IOException;

		/**
		 * Given a <em>global</em> chip location, return more info about the
		 * board that contains it.
		 *
		 * @param chip
		 *            The chip location
		 * @return Board information
		 * @throws IOException
		 *             If communication with the server fails
		 */
		WhereIs getBoardByChip(HasChipLocation chip) throws IOException;

		/**
		 * Given an IP address, return more info about a board.
		 *
		 * @param address
		 *            Board IP address
		 * @return Board information
		 * @throws IOException
		 *             If communication with the server fails
		 */
		WhereIs getBoardByIPAddress(String address) throws IOException;
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
		 * @throws IOException
		 *             If communication fails, the resources have not yet been
		 *             allocated, or the job is deleted.
		 */
		WhereIs whereIs(HasChipLocation chip) throws IOException;
	}

	/**
	 * A request to create a job.
	 *
	 * @author Donal Fellows
	 */
	@SuppressWarnings("checkstyle:visibilitymodifier")
	class CreateJob {
		/**
		 * How long after a keepalive message will the job be auto-deleted?
		 * <em>Required.</em> Must be between 30 and 300 seconds.
		 */
		public Duration keepaliveInterval;

		/**
		 * 0 to 3 values indicating what size of job to make.
		 * <ol>
		 * <li value="0">A single board job. (Default)
		 * <li>A job with at least the given number of boards.
		 * <li>An allocation that should incorporate the given number of triads
		 * of boards in each direction. Be aware that this is in triads!
		 * <li>A specific board, by X, Y, Z (<em>logical</em> coordinates).
		 * </ol>
		 */
		// TODO: want to support create by XYZ, by CFB, and by board IP address
		// There's really no need to stick to the limitations of the Python code
		public List<Integer> dimensions;

		/**
		 * Which machine to allocate on. This and {@link #tags} are mutually
		 * exclusive, but at least one must be given.
		 */
		public String machineName;

		/**
		 * The tags to select which machine to allocate on. This and
		 * {@link #machineName} are mutually exclusive, but at least one must be
		 * given.
		 */
		public List<String> tags;

		/**
		 * The maximum number of dead boards allowed in a rectangular
		 * allocation. Note that the allocation engine might increase this if it
		 * decides to overallocate. Defaults to {@code 0}.
		 */
		public Integer maxDeadBoards;

		// TODO needs more work
	}

	@JsonIgnoreProperties({
		"keepalive-ref", "machine-ref", "power-ref", "chip-ref"
	})
	class JobDescription {
		// TODO state should be an enum
		private String state;

		private String owner;

		private Instant startTime;

		private Instant finishTime;

		private String reason;

		private String keepaliveHost;

		private Instant keepaliveTime;

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

		public String getOwner() {
			return owner;
		}

		public void setOwner(String owner) {
			this.owner = owner;
		}

		@JsonAlias("start-time")
		public Instant getStartTime() {
			return startTime;
		}

		public void setStartTime(Instant startTime) {
			this.startTime = startTime;
		}

		@JsonAlias("finish-time")
		public Instant getFinishTime() {
			return finishTime;
		}

		public void setFinishTime(Instant finishTime) {
			this.finishTime = finishTime;
		}

		public String getReason() {
			return reason;
		}

		public void setReason(String reason) {
			this.reason = reason;
		}

		@JsonAlias("keepalive-host")
		public String getKeepaliveHost() {
			return keepaliveHost;
		}

		public void setKeepaliveHost(String keepaliveHost) {
			this.keepaliveHost = keepaliveHost;
		}

		@JsonAlias("keepalive-time")
		public Instant getKeepaliveTime() {
			return keepaliveTime;
		}

		public void setKeepaliveTime(Instant keepaliveTime) {
			this.keepaliveTime = keepaliveTime;
		}
	}

	class BoardCoords {
		/** Logical triad X coordinate. */
		private final int x;

		/** Logical triad Y coordinate. */
		private final int y;

		/** Logical triad Z coordinate. */
		private final int z;

		/** Physical cabinet number. */
		private final int cabinet;

		/** Physical frame number. */
		private final int frame;

		/** Physical board number. */
		private final int board;

		/**
		 * IP address of ethernet chip. May be {@code null} if the current user
		 * doesn't have permission to see the board address at this point.
		 */
		private final String address;

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
		BoardCoords(@JsonProperty("x") int x, @JsonProperty("y") int y,
				@JsonProperty("z") int z, @JsonProperty("cabinet") int cabinet,
				@JsonProperty("frame") int frame,
				@JsonProperty("board") int board,
				@JsonProperty("address") String address) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.cabinet = cabinet;
			this.frame = frame;
			this.board = board;
			this.address = address;
		}

		/**
		 * @return Logical triad X coordinate.
		 */
		public int getX() {
			return x;
		}

		/**
		 * @return Logical triad Y coordinate.
		 */
		public int getY() {
			return y;
		}

		/**
		 * @return Logical triad Z coordinate.
		 */
		public int getZ() {
			return z;
		}

		/**
		 * @return Physical cabinet number.
		 */
		public int getCabinet() {
			return cabinet;
		}

		/**
		 * @return Physical frame number.
		 */
		public int getFrame() {
			return frame;
		}

		/**
		 * @return Physical board number.
		 */
		public int getBoard() {
			return board;
		}

		/**
		 * @return IP address of ethernet chip. May be {@code null} if the
		 *         current user doesn't have permission to see the board address
		 *         at this point.
		 */
		public String getAddress() {
			return address;
		}

		@Override
		public String toString() {
			return String.format("Board(%d,%d,%d|%d:%d:%d|%s)", x, y, z,
					cabinet, frame, board, address);
		}
	}

	@JsonFormat(shape = Shape.ARRAY)
	@JsonPropertyOrder({
		"end1", "end2"
	})
	/** Describes a dead link. */
	class DeadLink {
		/** One end of a dead link. */
		public static class End {
			/** The board at the end of a dead link. */
			public final BoardCoords board;

			// TODO direction should be an enum
			/** The direction that the dead link goes in. */
			public final String direction;

			public End(@JsonProperty("board") BoardCoords board,
					@JsonProperty("direction") String direction) {
				this.board = board;
				this.direction = direction;
			}
		}

		@JsonProperty
		private End end1;

		@JsonProperty
		private End end2;

		public List<End> getEnds() {
			return Arrays.asList(end1, end2);
		}
	}

	@JsonFormat(shape = Shape.ARRAY)
	class Triad {
		private int x;

		private int y;

		private int z;

		public int getX() {
			return x;
		}

		public void setX(int x) {
			this.x = x;
		}

		public int getY() {
			return y;
		}

		public void setY(int y) {
			this.y = y;
		}

		public int getZ() {
			return z;
		}

		public void setZ(int z) {
			this.z = z;
		}

		@Override
		public String toString() {
			return String.format("[X:%d, Y:%d, Z:%d]", x, y, z);
		}
	}

	@JsonFormat(shape = Shape.ARRAY)
	class Physical {
		private int cabinet;

		private int frame;

		private int board;

		public int getCabinet() {
			return cabinet;
		}

		public void setCabinet(int cabinet) {
			this.cabinet = cabinet;
		}

		public int getFrame() {
			return frame;
		}

		public void setFrame(int frame) {
			this.frame = frame;
		}

		public int getBoard() {
			return board;
		}

		public void setBoard(int board) {
			this.board = board;
		}

		@Override
		public String toString() {
			return String.format("[%d:%d:%d]", cabinet, frame, board);
		}
	}

	class WhereIs {
		@JsonAlias("job-id")
		private Integer jobId;

		@JsonAlias("job-ref")
		private URI jobRef;

		@JsonAlias("job-chip")
		private ChipLocation jobChip;

		private ChipLocation chip;

		@JsonIgnore
		private Machine machineHandle;

		private String machineName;

		private URI machineRef;

		private ChipLocation boardChip;

		private Triad logicalCoords;

		private Physical physicalCoords;

		public Integer getJobId() {
			return jobId;
		}

		public void setJobId(Integer jobId) {
			this.jobId = jobId;
		}

		public URI getJobRef() {
			return jobRef;
		}

		public void setJobRef(URI jobRef) {
			this.jobRef = jobRef;
		}

		public ChipLocation getJobChip() {
			return jobChip;
		}

		public void setJobChip(ChipLocation jobChip) {
			this.jobChip = jobChip;
		}

		public ChipLocation getChip() {
			return chip;
		}

		public void setChip(ChipLocation chip) {
			this.chip = chip;
		}

		public Machine getMachineHandle() {
			return machineHandle;
		}

		public void setMachineHandle(Machine machineHandle) {
			this.machineHandle = machineHandle;
		}

		@JsonAlias("machine")
		public String getMachineName() {
			return machineName;
		}

		public void setMachineName(String machineName) {
			this.machineName = machineName;
		}

		public URI getMachineRef() {
			return machineRef;
		}

		public void setMachineRef(URI machineRef) {
			this.machineRef = machineRef;
		}

		public ChipLocation getBoardChip() {
			return boardChip;
		}

		public void setBoardChip(ChipLocation boardChip) {
			this.boardChip = boardChip;
		}

		@JsonAlias("logical-board-coordinates")
		public Triad getLogicalCoords() {
			return logicalCoords;
		}

		public void setLogicalCoords(Triad logicalCoords) {
			this.logicalCoords = logicalCoords;
		}

		@JsonAlias("physical-board-coordinates")
		public Physical getPhysicalCoords() {
			return physicalCoords;
		}

		public void setPhysicalCoords(Physical physicalCoords) {
			this.physicalCoords = physicalCoords;
		}
	}

	@JsonIgnoreProperties({
		"power", "machine", "machine-ref"
	})
	class AllocatedMachine {
		private int width;

		private int height;

		private int depth;

		private String machineName;

		private Machine machine;

		private List<Object> connections;

		private List<BoardCoordinates> boards;

		/** @return Rectangle width. */
		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		/** @return Rectangle height. */
		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		/** @return Depth of rectangle. 1 or 3. */
		public int getDepth() {
			return depth;
		}

		public void setDepth(int depth) {
			this.depth = depth;
		}

		/** @return On what machine. */
		public String getMachineName() {
			return machineName;
		}

		public void setMachineName(String machineName) {
			this.machineName = machineName;
		}

		/** @return The hosting SpiNNaker machine. */
		public Machine getMachine() {
			return machine;
		}

		void setMachine(Machine machine) {
			this.machine = machine;
		}

		/** @return How to talk to boards. */
		public List<Object/* ConnectionInfo */> getConnections() {
			return connections;
		}

		public void setConnections(List<Object> connections) {
			this.connections = connections;
		}

		/** @return Where the boards are. */
		public List<BoardCoordinates> getBoards() {
			return boards;
		}

		public void setBoards(List<BoardCoordinates> boards) {
			this.boards = boards;
		}
	}

}
