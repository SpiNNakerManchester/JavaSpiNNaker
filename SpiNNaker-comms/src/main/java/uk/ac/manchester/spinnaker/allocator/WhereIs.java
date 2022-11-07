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

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import uk.ac.manchester.spinnaker.allocator.SpallocClient.Machine;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;

/** A description of where a board is and what it is doing. */
@JsonDeserialize(builder = WhereIs.Builder.class)
public final class WhereIs {
	@JsonProperty("job-id")
	private final Integer jobId;

	@JsonProperty("job-ref")
	private final URI jobRef;

	@JsonProperty("job-chip")
	private final ChipLocation jobChip;

	@JsonProperty("chip")
	private final ChipLocation chip;

	@JsonIgnore
	private transient Machine machineHandle;

	@JsonProperty("machine")
	private final String machineName;

	@JsonProperty("machine-ref")
	private URI machineRef;

	@JsonProperty("board-chip")
	private final ChipLocation boardChip;

	@JsonProperty("logical-board-coordinates")
	private final TriadCoords logicalCoords;

	@JsonProperty("physical-board-coordinates")
	private final PhysicalCoords physicalCoords;

	private WhereIs(Integer jobId, URI jobRef, ChipLocation jobChip,
			ChipLocation chip, String machineName, URI machineRef,
			ChipLocation boardChip, TriadCoords logicalCoords,
			PhysicalCoords physicalCoords) {
		this.jobId = jobId;
		this.jobRef = jobRef;
		this.jobChip = jobChip;
		this.chip = chip;
		this.machineName = machineName;
		this.machineRef = machineRef;
		this.boardChip = boardChip;
		this.logicalCoords = logicalCoords;
		this.physicalCoords = physicalCoords;
	}

	/**
	 * @return The ID of the job allocated to the board with the chip.
	 *         {@code null} if none.
	 */
	public Integer getJobId() {
		return jobId;
	}

	/**
	 * @return The location of more information about the job allocated to the
	 *         board with the chip. {@code null} if none.
	 */
	public URI getJobRef() {
		return jobRef;
	}

	/**
	 * @return The global location of the chip at the root of the job
	 *         allocation, if one exists.
	 */
	public ChipLocation getJobChip() {
		return jobChip;
	}

	/** @return The global location of the chip. */
	public ChipLocation getChip() {
		return chip;
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

	/**
	 * @return The location of more information about the machine containing the
	 *         chip.
	 */
	URI getMachineRef() {
		return machineRef;
	}

	void clearMachineRef() {
		machineRef = null;
	}

	/**
	 * @return The global location of the chip at the root of the board
	 *         containing the chip being described.
	 */
	public ChipLocation getBoardChip() {
		return boardChip;
	}

	/** @return The logical coordinates for the board containing the chip. */
	public TriadCoords getLogicalCoords() {
		return logicalCoords;
	}

	/** @return The physical coordinates for the board containing the chip. */
	public PhysicalCoords getPhysicalCoords() {
		return physicalCoords;
	}

	@JsonPOJOBuilder(withPrefix = "set")
	static class Builder {
		private Integer jobId;

		private URI jobRef;

		private ChipLocation jobChip;

		private ChipLocation chip;

		private String machineName;

		private URI machineRef;

		private ChipLocation boardChip;

		private TriadCoords logicalCoords;

		private PhysicalCoords physicalCoords;

		void setJobId(Integer jobId) {
			this.jobId = jobId;
		}

		void setJobRef(URI jobRef) {
			this.jobRef = jobRef;
		}

		void setJobChip(ChipLocation jobChip) {
			this.jobChip = jobChip;
		}

		void setChip(ChipLocation chip) {
			this.chip = chip;
		}

		@JsonProperty("machine")
		@JsonAlias("machine-name")
		void setMachineName(String machineName) {
			this.machineName = machineName;
		}

		void setMachineRef(URI machineRef) {
			this.machineRef = machineRef;
		}

		void setBoardChip(ChipLocation boardChip) {
			this.boardChip = boardChip;
		}

		@JsonProperty("logical-board-coordinates")
		void setLogicalCoords(TriadCoords logicalCoords) {
			this.logicalCoords = logicalCoords;
		}

		@JsonProperty("physical-board-coordinates")
		void setPhysicalCoords(PhysicalCoords physicalCoords) {
			this.physicalCoords = physicalCoords;
		}

		WhereIs build() {
			return new WhereIs(jobId, jobRef, jobChip, chip, machineName,
					machineRef, boardChip, logicalCoords, physicalCoords);
		}
	}
}
