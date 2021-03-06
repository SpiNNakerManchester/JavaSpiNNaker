/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc.messages;

import java.util.Objects;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 * The description of where some resource is on a SpiNNaker system.
 */
public class WhereIs {
	private ChipLocation jobChip;

	private int jobId;

	private ChipLocation chip;

	private BoardCoordinates logical;

	private String machine;

	private ChipLocation boardChip;

	private BoardPhysicalCoordinates physical;

	/**
	 * Default constructor for unmarshaller use only.
	 */
	public WhereIs() {
	}

	/**
	 * Create.
	 *
	 * @param jobChip
	 *            the chip location relative to the job's allocation.
	 * @param jobId
	 *            the job id.
	 * @param chip
	 *            the absolute chip location.
	 * @param logical
	 *            the logical coordinates of the board
	 * @param machine
	 *            the name of the machine
	 * @param boardChip
	 *            the chip location relative to its board
	 * @param physical
	 *            the physical coordinates of the board
	 */
	public WhereIs(ChipLocation jobChip, int jobId, ChipLocation chip,
			BoardCoordinates logical, String machine, ChipLocation boardChip,
			BoardPhysicalCoordinates physical) {
		this.jobChip = jobChip;
		this.jobId = jobId;
		this.chip = chip;
		this.logical = logical;
		this.machine = machine;
		this.boardChip = boardChip;
		this.physical = physical;
	}

	/**
	 * Get the chip location relative to the job's allocation.
	 *
	 * @return the job-relative chip location
	 */
	public ChipLocation getJobChip() {
		return jobChip;
	}

	/**
	 * Sets the chip location relative to the job's allocation.
	 *
	 * @param jobChip
	 *            the job-relative chip location to set
	 */
	public void setJobChip(ChipLocation jobChip) {
		this.jobChip = jobChip;
	}

	/**
	 * Get the job id.
	 *
	 * @return the job id
	 */
	public int getJobId() {
		return jobId;
	}

	/**
	 * Sets the job id.
	 *
	 * @param jobId
	 *            the job id to set
	 */
	public void setJobId(int jobId) {
		this.jobId = jobId;
	}

	/**
	 * Get the chip.
	 *
	 * @return the chip
	 */
	public ChipLocation getChip() {
		return chip;
	}

	/**
	 * Sets the chip.
	 *
	 * @param chip
	 *            the chip to set
	 */
	public void setChip(ChipLocation chip) {
		this.chip = chip;
	}

	/**
	 * Get the logical board coordinates.
	 *
	 * @return the logical board coordinates
	 */
	public BoardCoordinates getLogical() {
		return logical;
	}

	/**
	 * Sets the logical board coordinates.
	 *
	 * @param logical
	 *            the logical board coordinates to set
	 */
	public void setLogical(BoardCoordinates logical) {
		this.logical = logical;
	}

	/**
	 * Get the machine.
	 *
	 * @return the machine
	 */
	public String getMachine() {
		return machine;
	}

	/**
	 * Sets the machine.
	 *
	 * @param machine
	 *            the machine to set
	 */
	public void setMachine(String machine) {
		this.machine = machine;
	}

	/**
	 * Get the chip location relative to the board.
	 *
	 * @return the board chip location
	 */
	public ChipLocation getBoardChip() {
		return boardChip;
	}

	/**
	 * Sets the chip location relative to the board.
	 *
	 * @param boardChip
	 *            the board chip location to set
	 */
	public void setBoardChip(ChipLocation boardChip) {
		this.boardChip = boardChip;
	}

	/**
	 * Get the physical board coordinates.
	 *
	 * @return the physical board coordinates
	 */
	public BoardPhysicalCoordinates getPhysical() {
		return physical;
	}

	/**
	 * Sets the physical board coordinates.
	 *
	 * @param physical
	 *            the physical board coordinates to set
	 */
	public void setPhysical(BoardPhysicalCoordinates physical) {
		this.physical = physical;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof WhereIs) {
			WhereIs other = (WhereIs) o;
			return Objects.equals(jobChip, other.jobChip)
					&& jobId == other.jobId && Objects.equals(chip, other.chip)
					&& Objects.equals(logical, other.logical)
					&& Objects.equals(machine, other.machine)
					&& Objects.equals(boardChip, other.boardChip)
					&& Objects.equals(physical, other.physical);
		}
		return false;
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "jobChip: " + jobChip + " jobId: " + jobId + " chip: " + chip
				+ " logical: " + logical + " machine: " + machine
				+ " boardChip: " + boardChip + " physical: " + physical;
	}
}
