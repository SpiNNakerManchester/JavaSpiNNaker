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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.Version;

public interface SpallocClient {
	Version getVersion() throws IOException;

	List<Machine> listMachines() throws IOException;

	List<Job> listJobs() throws IOException;

	Job createJob(Object createInstructions) throws IOException;
	// FIXME define real argument to createJob

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

		// TODO wait-for-change

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

		WhereIs getBoardByPhysicalCoords(int cabinet, int frame, int board)
				throws IOException;

		WhereIs getBoardByChip(HasChipLocation chip) throws IOException;

		WhereIs getBoardByIPAddress(String address) throws IOException;
	}

	interface Job {
		JobDescription describe() throws IOException;

		// TODO wait-for-change

		void keepalive() throws IOException;

		void delete(String reason) throws IOException;

		AllocatedMachine machine() throws IOException;

		boolean getPower() throws IOException;

		boolean setPower(boolean switchOn) throws IOException;

		WhereIs whereIs(HasChipLocation chip) throws IOException;
	}

	class JobDescription {
		// FIXME is this needed?
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

	class DeadLink {

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

		@JsonAlias("machine")
		private String machineName;

		@JsonAlias("machine-ref")
		private URI machineRef;

		@JsonAlias("board-chip")
		private ChipLocation boardChip;

		@JsonAlias("logical-board-coordinates")
		private Triad logicalCoords;

		@JsonAlias("physical-board-coordinates")
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

		public Triad getLogicalCoords() {
			return logicalCoords;
		}

		public void setLogicalCoords(Triad logicalCoords) {
			this.logicalCoords = logicalCoords;
		}

		public Physical getPhysicalCoords() {
			return physicalCoords;
		}

		public void setPhysicalCoords(Physical physicalCoords) {
			this.physicalCoords = physicalCoords;
		}
	}

	class AllocatedMachine {

	}

}
