/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.client;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import uk.ac.manchester.spinnaker.alloc.client.SpallocClient.Machine;
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
