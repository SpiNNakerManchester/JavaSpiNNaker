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
package uk.ac.manchester.spinnaker.alloc.web;

import static java.util.Objects.nonNull;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.Keep;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.board.ValidBoardNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidCabinetNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidFrameNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadX;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadY;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * A request to report an issue with some boards.
 *
 * @author Donal Fellows
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class IssueReportRequest {
	/** What the problem is believed to be. */
	@NotBlank(message = "an issue description must be given")
	public String issue;

	/** Describes the boards that have the issue. */
	public List<@Valid ReportedBoard> boards;

	/** Describes a board that has an issue. */
	public static class ReportedBoard {
		/** The location of the chip within the reporting allocation. */
		@Valid
		public ChipLocation chip;

		/** The X triad coordinate of the board. */
		@ValidTriadX
		public Integer x;

		/** The Y triad coordinate of the board. */
		@ValidTriadY
		public Integer y;

		/** The Z triad coordinate of the board. */
		@ValidTriadZ
		public Integer z;

		/** The physical cabinet number of the board. */
		@ValidCabinetNumber
		public Integer cabinet;

		/** The physical frame number of the board. */
		@ValidFrameNumber
		public Integer frame;

		/** The physical board number of the board. */
		@ValidBoardNumber
		public Integer board;

		/** The IP address of the board. */
		@IPAddress(nullOK = true, message = "address must be an IP address")
		public String address;

		@JsonIgnore
		private boolean isChipValid() {
			return nonNull(chip);
		}

		@JsonIgnore
		private boolean isTriadValid() {
			return nonNull(x) && nonNull(y) && nonNull(z);
		}

		@JsonIgnore
		private boolean isPhysicalValid() {
			return nonNull(cabinet) && nonNull(frame) && nonNull(board);
		}

		@JsonIgnore
		private boolean isIPValid() {
			return nonNull(address);
		}

		/**
		 * Does this represent a valid way of identifying a board?
		 *
		 * @return Yes if at least one way of giving coordinates is valid.
		 */
		@Keep
		@JsonIgnore
		@AssertTrue(message = "at least one way of identifying a board "
				+ "must be given")
		private boolean isBoardValid() {
			return isChipValid() || isTriadValid() || isPhysicalValid()
					|| isIPValid();
		}
	}
}
