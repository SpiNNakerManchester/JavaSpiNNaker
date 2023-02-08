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
