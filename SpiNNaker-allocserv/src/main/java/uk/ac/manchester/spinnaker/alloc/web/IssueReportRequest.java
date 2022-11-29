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

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.Keep;

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
 * @param issue
 *            What the problem is believed to be.
 * @param boards
 *            Describes the boards that have the issue.
 */
public record IssueReportRequest(
		@NotBlank(message = "an issue description must be given") String issue,
		List<@Valid ReportedBoard> boards) {
	/**
	 * Describes a board that has an issue.
	 *
	 * @param chip
	 *            The location of the chip within the reporting allocation.
	 * @param x
	 *            The X triad coordinate of the board.
	 * @param y
	 *            The Y triad coordinate of the board.
	 * @param z
	 *            The Z triad coordinate of the board.
	 * @param cabinet
	 *            The physical cabinet number of the board.
	 * @param frame
	 *            The physical frame number of the board.
	 * @param board
	 *            The physical board number of the board.
	 * @param address
	 *            The IP address of the board.
	 */
	public static record ReportedBoard(@Valid ChipLocation chip,
			@ValidTriadX Integer x, @ValidTriadY Integer y,
			@ValidTriadZ Integer z, @ValidCabinetNumber Integer cabinet,
			@ValidFrameNumber Integer frame, @ValidBoardNumber Integer board,
			@IPAddress(nullOK = true, message = "address must be "
					+ "an IP address") String address) {
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
