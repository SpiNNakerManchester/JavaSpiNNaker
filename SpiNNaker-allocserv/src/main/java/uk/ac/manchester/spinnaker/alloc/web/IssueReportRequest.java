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
	public record ReportedBoard(@Valid ChipLocation chip,
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
