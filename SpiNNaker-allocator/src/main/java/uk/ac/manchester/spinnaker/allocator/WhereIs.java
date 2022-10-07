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

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.manchester.spinnaker.allocator.SpallocClient.Machine;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadX;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadY;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;

/** A description of where a board is and what it is doing. */
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
public class WhereIs {
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

	private TriadCoords logicalCoords;

	private PhysicalCoords physicalCoords;

	/**
	 * @return The ID of the job allocated to the board with the chip.
	 *         {@code null} if none.
	 */
	public Integer getJobId() {
		return jobId;
	}

	void setJobId(Integer jobId) {
		this.jobId = jobId;
	}

	/**
	 * @return The location of more information about the job allocated to the
	 *         board with the chip. {@code null} if none.
	 */
	public URI getJobRef() {
		return jobRef;
	}

	void setJobRef(URI jobRef) {
		this.jobRef = jobRef;
	}

	/**
	 * @return The global location of the chip at the root of the job
	 *         allocation, if one exists.
	 */
	public ChipLocation getJobChip() {
		return jobChip;
	}

	void setJobChip(ChipLocation jobChip) {
		this.jobChip = jobChip;
	}

	/** @return The global location of the chip. */
	public ChipLocation getChip() {
		return chip;
	}

	void setChip(ChipLocation chip) {
		this.chip = chip;
	}

	/** @return Information about the machine containing the chip. */
	public Machine getMachineHandle() {
		return machineHandle;
	}

	void setMachineHandle(Machine machineHandle) {
		this.machineHandle = machineHandle;
	}

	/** @return The name of the machine containing the chip. */
	public String getMachineName() {
		return machineName;
	}

	@JsonAlias("machine")
	void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	/**
	 * @return The location of more information about the machine containing the
	 *         chip.
	 */
	public URI getMachineRef() {
		return machineRef;
	}

	void setMachineRef(URI machineRef) {
		this.machineRef = machineRef;
	}

	/**
	 * @return The global location of the chip at the root of the board
	 *         containing the chip being described.
	 */
	public ChipLocation getBoardChip() {
		return boardChip;
	}

	void setBoardChip(ChipLocation boardChip) {
		this.boardChip = boardChip;
	}

	/** @return The logical coordinates for the board containing the chip. */
	public TriadCoords getLogicalCoords() {
		return logicalCoords;
	}

	@JsonAlias("logical-board-coordinates")
	void setLogicalCoords(Triad logicalCoords) {
		this.logicalCoords = logicalCoords.toStd();
	}

	/** @return The physical coordinates for the board containing the chip. */
	public PhysicalCoords getPhysicalCoords() {
		return physicalCoords;
	}

	@JsonAlias("physical-board-coordinates")
	void setPhysicalCoords(Physical physicalCoords) {
		this.physicalCoords = physicalCoords.toStd();
	}

	/**
	 * Logical coordinates of a board.
	 * <p>
	 * This is a helper for deserialization only.
	 */
	@JsonFormat(shape = ARRAY)
	@JsonAutoDetect(setterVisibility = NON_PRIVATE)
	static class Triad {
		@ValidTriadX
		private int x;

		@ValidTriadY
		private int y;

		@ValidTriadZ
		private int z;

		Triad() {
		}

		/**
		 * @param x The X coordinate of the board.
		 * @param y The Y coordinate of the board.
		 * @param z The Z coordinate of the board.
		 */
		Triad(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		/** @return The X coordinate of the board. */
		public int getX() {
			return x;
		}

		void setX(int x) {
			this.x = x;
		}

		/** @return The Y coordinate of the board. */
		public int getY() {
			return y;
		}

		void setY(int y) {
			this.y = y;
		}

		/** @return The Z coordinate of the board. */
		public int getZ() {
			return z;
		}

		void setZ(int z) {
			this.z = z;
		}

		public TriadCoords toStd() {
			return new TriadCoords(x, y, z);
		}
	}

	/**
	 * Physical coordinates of a board. SpiNNaker boards are arranged in frames
	 * (multi-unit racks that share a management layer) and frames are arranged
	 * in cabinets (full 19" server cabinets).
	 * <p>
	 * This is a helper for deserialization only.
	 */
	@JsonFormat(shape = ARRAY)
	@JsonAutoDetect(setterVisibility = NON_PRIVATE)
	static class Physical {
		private int cabinet;

		private int frame;

		private Integer board;

		Physical() {
		}

		/**
		 * @param cabinet
		 *            The cabinet number.
		 * @param frame
		 *            The frame number.
		 * @param board
		 *            The board number.
		 */
		Physical(int cabinet, int frame, int board) {
			this.cabinet = cabinet;
			this.frame = frame;
			this.board = board;
		}

		/** @return The cabinet number. */
		public int getCabinet() {
			return cabinet;
		}

		void setCabinet(int cabinet) {
			this.cabinet = cabinet;
		}

		/** @return The frame number. */
		public int getFrame() {
			return frame;
		}

		void setFrame(int frame) {
			this.frame = frame;
		}

		/** @return The board number. */
		public Integer getBoard() {
			// TODO document when this can be null
			return board;
		}

		void setBoard(Integer board) {
			this.board = board;
		}

		public PhysicalCoords toStd() {
			if (board == null) {
				return null;
			}
			return new PhysicalCoords(cabinet, frame, board);
		}
	}
}
