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

import java.time.Duration;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.Keep;

import uk.ac.manchester.spinnaker.machine.board.ValidBoardNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidCabinetNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidFrameNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadHeight;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadWidth;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadX;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadY;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * A request to create a job.
 *
 * @author Donal Fellows
 * @param owner
 *            Who owns the job. Ignored when the job is submitted by a
 *            non-admin.
 * @param group
 *            What group will the job be accounted against; the owner
 *            <em>must</em> be a member of the group. If {@code null}, the
 *            single group that the owner is a member of will be used (with it
 *            being an error for that to not exist or not be unique).
 * @param keepaliveInterval
 *            How long after a keepalive message will the job be auto-deleted?
 *            <em>Required.</em> Must be between 30 and 300 seconds.
 * @param numBoards
 *            The number of boards to allocate. May be {@code null} to either
 *            use the default (1) or to let one of the other selectors
 *            ({@link #dimensions}, {@link #board}) make the choice.
 * @param dimensions
 *            The dimensions of rectangle of triads of boards to allocate. May
 *            be {@code null} to let one of the other selectors
 *            ({@link #numBoards}, {@link #board}) make the choice.
 * @param board
 *            The specific board to allocate. May be {@code null} to let one of
 *            the other selectors ({@link #numBoards}, {@link #dimensions}) make
 *            the choice.
 * @param machineName
 *            Which machine to allocate on. This and {@link #tags} are mutually
 *            exclusive, but at least one must be given.
 * @param tags
 *            The tags to select which machine to allocate on. This and
 *            {@link #machineName} are mutually exclusive, but at least one must
 *            be given.
 * @param maxDeadBoards
 *            The maximum number of dead boards allowed in a rectangular
 *            allocation. Note that the allocation engine might increase this if
 *            it decides to overallocate. Defaults to {@code 0}.
 */
public record CreateJobRequest(String owner, String group,
		@NotNull(message = "keepalive-interval is "
				+ "required") Duration keepaliveInterval,
		@Positive(message = "number of boards must be "
				+ "at least 1 if given") Integer numBoards,
		@Valid Dimensions dimensions, @Valid SpecificBoard board,
		String machineName,
		List<@NotBlank(message = "tags must not be blank") String> tags,
		@PositiveOrZero(message = "max-dead-boards may not be "
				+ "negative") Integer maxDeadBoards) {
	@JsonCreator
	CreateJobRequest(String owner, String group,
			@NotNull(message = "keepalive-interval is "
					+ "required") String keepaliveInterval,
			@Positive(message = "number of boards must be "
					+ "at least 1 if given") Integer numBoards,
			@Valid Dimensions dimensions, @Valid SpecificBoard board,
			String machineName,
			List<@NotBlank(message = "tags must not be blank") String> tags,
			@PositiveOrZero(message = "max-dead-boards may not be "
					+ "negative") Integer maxDeadBoards) {
		this(owner, group, Duration.parse(keepaliveInterval), numBoards,
				dimensions, board, machineName, tags, maxDeadBoards);
	}

	// Extended validation

	@Keep
	@JsonIgnore
	@AssertTrue(message = "either machine-name or tags must be supplied")
	private boolean isMachineNameAndTagsMutuallyExclusive() {
		return nonNull(machineName) != nonNull(tags);
	}

	@Keep
	@JsonIgnore
	@AssertFalse(message = "machine-name, if given, must be non-blank")
	private boolean isMachineNameInsane() {
		return nonNull(machineName) && machineName.isBlank();
	}

	@Keep
	@JsonIgnore
	@AssertTrue(message = "only at most one of num-boards, dimensions and "
			+ "board should be given")
	private boolean isOverLocated() {
		int count = 0;
		if (nonNull(numBoards)) {
			count++;
		}
		if (nonNull(dimensions)) {
			count++;
		}
		if (nonNull(board)) {
			count++;
		}
		return count <= 1;
	}

	private static final Duration MIN_KEEPALIVE = Duration.parse("PT30S");

	private static final Duration MAX_KEEPALIVE = Duration.parse("PT300S");

	@Keep
	@JsonIgnore
	@AssertTrue(message = "keepalive-interval must be 30 to 300 seconds")
	private boolean isKeepaliveIntervalInRange() {
		/*
		 * Really ought to be validated against config, but we've not got the
		 * config at this point.
		 */
		return keepaliveInterval.compareTo(MAX_KEEPALIVE) <= 0
				&& keepaliveInterval.compareTo(MIN_KEEPALIVE) >= 0;
	}

	/**
	 * Describes a request for an allocation of given dimensions.
	 *
	 * @param width
	 *            The width of the rectangle of boards to allocate, in triads.
	 * @param height
	 *            The height of the rectangle of boards to allocate, in triads.
	 */
	public record Dimensions(@ValidTriadWidth int width,
			@ValidTriadHeight int height) {
	}

	/**
	 * Describes a request for a specific board.
	 *
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
	public record SpecificBoard(@ValidTriadX Integer x, @ValidTriadY Integer y,
			@ValidTriadZ Integer z, @ValidCabinetNumber Integer cabinet,
			@ValidFrameNumber Integer frame, @ValidBoardNumber Integer board,
			@IPAddress(nullOK = true, message = "address must be "
					+ "an IP address") String address) {
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
		private boolean isSpecificBoardValid() {
			return isTriadValid() || isPhysicalValid() || isIPValid();
		}
	}
}
