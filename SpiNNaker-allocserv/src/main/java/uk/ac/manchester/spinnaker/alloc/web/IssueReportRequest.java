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
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;

import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.tags.IPAddress;

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
		public ChipLocation chip;

		/** The X triad coordinate of the board. */
		@PositiveOrZero(message = "x must be at least 0")
		public Integer x;

		/** The Y triad coordinate of the board. */
		@PositiveOrZero(message = "y must be at least 0")
		public Integer y;

		/** The Z triad coordinate of the board. */
		@Min(value = 0, message = "z must be at least 0")
		@Max(value = 2, message = "z must be at most 2")
		public Integer z;

		/** The physical cabinet number of the board. */
		@PositiveOrZero(message = "cabinet must be at least 0")
		public Integer cabinet;

		/** The physical frame number of the board. */
		@PositiveOrZero(message = "frame must be at least 0")
		public Integer frame;

		/** The physical board number of the board. */
		@PositiveOrZero(message = "board must be at least 0")
		public Integer board;

		/** The IP address of the board. */
		@IPAddress(message = "address must be an IP address")
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
		@JsonIgnore
		@AssertTrue(message = "at least one way of identifying a board "
				+ "must be given")
		private boolean isBoardValid() {
			return isChipValid() || isTriadValid() || isPhysicalValid()
					|| isIPValid();
		}
	}
}
