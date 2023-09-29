/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.spalloc.messages;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 * The description of where some resource is on a SpiNNaker system.
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
@Immutable
@JsonDeserialize(builder = WhereIs.Builder.class)
public record WhereIs(ChipLocation jobChip, Integer jobId, ChipLocation chip,
		BoardCoordinates logical, String machine, ChipLocation boardChip,
		BoardPhysicalCoordinates physical) {
	@Override
	public String toString() {
		return "jobChip: " + jobChip + " jobId: " + jobId + " chip: " + chip
				+ " logical: " + logical + " machine: " + machine
				+ " boardChip: " + boardChip + " physical: " + physical;
	}

	/**
	 * Builder for {@link WhereIs}.
	 */
	@JsonPOJOBuilder(withPrefix = "set")
	public static class Builder {
		private ChipLocation jobChip;

		private Integer jobId;

		private ChipLocation chip;

		private BoardCoordinates logical;

		private String machine;

		private ChipLocation boardChip;

		private BoardPhysicalCoordinates physical;

		/**
		 * @param jobChip
		 *            The chip location relative to the job's allocation.
		 */
		public void setJobChip(ChipLocation jobChip) {
			this.jobChip = jobChip;
		}

		/**
		 * @param jobId
		 *            The job id.
		 */
		public void setJobId(Integer jobId) {
			this.jobId = jobId;
		}

		/**
		 * @param chip
		 *            The absolute chip location.
		 */
		public void setChip(ChipLocation chip) {
			this.chip = chip;
		}

		/**
		 * @param logical
		 *            The logical coordinates of the board.
		 */
		public void setLogical(BoardCoordinates logical) {
			this.logical = logical;
		}

		/**
		 * @param machine
		 *            The name of the machine.
		 */
		public void setMachine(String machine) {
			this.machine = machine;
		}

		/**
		 * @param boardChip
		 *            The chip location relative to its board.
		 */
		public void setBoardChip(ChipLocation boardChip) {
			this.boardChip = boardChip;
		}

		/**
		 * @param physical
		 *            The physical coordinates of the board.
		 */
		public void setPhysical(BoardPhysicalCoordinates physical) {
			this.physical = physical;
		}

		/**
		 * Build an instance of the immutable {@link WhereIs}.
		 *
		 * @return The instance.
		 */
		public WhereIs build() {
			return new WhereIs(jobChip, jobId, chip, logical, machine,
					boardChip, physical);
		}
	}
}
