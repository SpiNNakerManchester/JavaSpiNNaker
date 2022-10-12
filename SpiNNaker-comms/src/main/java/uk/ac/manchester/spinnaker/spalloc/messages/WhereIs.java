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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 * The description of where some resource is on a SpiNNaker system.
 */
@Immutable
@JsonDeserialize(builder = WhereIs.Builder.class)
public final class WhereIs {
	private final ChipLocation jobChip;

	private final Integer jobId;

	private final ChipLocation chip;

	private final BoardCoordinates logical;

	private final String machine;

	private final ChipLocation boardChip;

	private final BoardPhysicalCoordinates physical;

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
	public WhereIs(ChipLocation jobChip, Integer jobId, ChipLocation chip,
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
	 * Get the job id.
	 *
	 * @return the job id
	 */
	public Integer getJobId() {
		return jobId;
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
	 * Get the logical board coordinates.
	 *
	 * @return the logical board coordinates
	 */
	public BoardCoordinates getLogical() {
		return logical;
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
	 * Get the chip location relative to the board.
	 *
	 * @return the board chip location
	 */
	public ChipLocation getBoardChip() {
		return boardChip;
	}

	/**
	 * Get the physical board coordinates.
	 *
	 * @return the physical board coordinates
	 */
	public BoardPhysicalCoordinates getPhysical() {
		return physical;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof WhereIs) {
			var other = (WhereIs) o;
			return Objects.equals(jobChip, other.jobChip)
					&& Objects.equals(jobId, other.jobId)
					&& Objects.equals(chip, other.chip)
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

	@JsonPOJOBuilder
	public static class Builder {
		private ChipLocation jobChip;

		private Integer jobId;

		private ChipLocation chip;

		private BoardCoordinates logical;

		private String machine;

		private ChipLocation boardChip;

		private BoardPhysicalCoordinates physical;

		@CanIgnoreReturnValue
		public Builder withJobChip(ChipLocation jobChip) {
			this.jobChip = jobChip;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withJobId(Integer jobId) {
			this.jobId = jobId;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withChip(ChipLocation chip) {
			this.chip = chip;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withLogical(BoardCoordinates logical) {
			this.logical = logical;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withMachine(String machine) {
			this.machine = machine;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withBoardChip(ChipLocation boardChip) {
			this.boardChip = boardChip;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withPhysical(BoardPhysicalCoordinates physical) {
			this.physical = physical;
			return this;
		}

		public WhereIs build() {
			return new WhereIs(jobChip, jobId, chip, logical, machine,
					boardChip, physical);
		}
	}
}
