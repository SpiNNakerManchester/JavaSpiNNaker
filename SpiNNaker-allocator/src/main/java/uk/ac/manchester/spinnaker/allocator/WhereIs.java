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

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.manchester.spinnaker.allocator.SpallocClient.Machine;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

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

	private Triad logicalCoords;

	private Physical physicalCoords;

	public Integer getJobId() {
		return jobId;
	}

	void setJobId(Integer jobId) {
		this.jobId = jobId;
	}

	public URI getJobRef() {
		return jobRef;
	}

	void setJobRef(URI jobRef) {
		this.jobRef = jobRef;
	}

	public ChipLocation getJobChip() {
		return jobChip;
	}

	void setJobChip(ChipLocation jobChip) {
		this.jobChip = jobChip;
	}

	public ChipLocation getChip() {
		return chip;
	}

	void setChip(ChipLocation chip) {
		this.chip = chip;
	}

	public Machine getMachineHandle() {
		return machineHandle;
	}

	void setMachineHandle(Machine machineHandle) {
		this.machineHandle = machineHandle;
	}

	@JsonAlias("machine")
	public String getMachineName() {
		return machineName;
	}

	void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	public URI getMachineRef() {
		return machineRef;
	}

	void setMachineRef(URI machineRef) {
		this.machineRef = machineRef;
	}

	public ChipLocation getBoardChip() {
		return boardChip;
	}

	void setBoardChip(ChipLocation boardChip) {
		this.boardChip = boardChip;
	}

	@JsonAlias("logical-board-coordinates")
	public Triad getLogicalCoords() {
		return logicalCoords;
	}

	void setLogicalCoords(Triad logicalCoords) {
		this.logicalCoords = logicalCoords;
	}

	@JsonAlias("physical-board-coordinates")
	public Physical getPhysicalCoords() {
		return physicalCoords;
	}

	void setPhysicalCoords(Physical physicalCoords) {
		this.physicalCoords = physicalCoords;
	}
}
