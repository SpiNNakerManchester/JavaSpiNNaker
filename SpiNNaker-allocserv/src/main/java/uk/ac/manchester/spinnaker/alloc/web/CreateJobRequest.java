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

import java.time.Duration;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

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
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class CreateJobRequest {
	/**
	 * Who owns the job. Ignored when the job is submitted by a non-admin.
	 */
	public String owner;

	/**
	 * What group will the job be accounted against; the owner <em>must</em> be
	 * a member of the group. If {@code null}, the single group that the owner
	 * is a member of will be used (with it being an error for that to not exist
	 * or not be unique).
	 */
	public String group;

	/**
	 * How long after a keepalive message will the job be auto-deleted?
	 * <em>Required.</em> Must be between 30 and 300 seconds.
	 */
	@NotNull(message = "keepalive-interval is required")
	public Duration keepaliveInterval;

	/**
	 * The number of boards to allocate. May be {@code null} to either use the
	 * default (1) or to let one of the other selectors ({@link #dimensions},
	 * {@link #board}) make the choice.
	 */
	@Positive(message = "number of boards must be at least 1 if given")
	public Integer numBoards;

	/**
	 * The dimensions of rectangle of triads of boards to allocate. May be
	 * {@code null} to let one of the other selectors ({@link #numBoards},
	 * {@link #board}) make the choice.
	 */
	@Valid
	public Dimensions dimensions;

	/**
	 * The specific board to allocate. May be {@code null} to let one of the
	 * other selectors ({@link #numBoards}, {@link #dimensions}) make the
	 * choice.
	 */
	@Valid
	public SpecificBoard board;

	/**
	 * Which machine to allocate on. This and {@link #tags} are mutually
	 * exclusive, but at least one must be given.
	 */
	public String machineName;

	/**
	 * The tags to select which machine to allocate on. This and
	 * {@link #machineName} are mutually exclusive, but at least one must be
	 * given.
	 */
	public List<@NotBlank(message = "tags must not be blank") String> tags;

	/**
	 * The maximum number of dead boards allowed in a rectangular allocation.
	 * Note that the allocation engine might increase this if it decides to
	 * overallocate. Defaults to {@code 0}.
	 */
	@PositiveOrZero(message = "max-dead-boards may not be negative")
	public Integer maxDeadBoards;

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

	/** Describes a request for an allocation of given dimensions. */
	public static class Dimensions {
		/** The width of the rectangle of boards to allocate, in triads. */
		@ValidTriadWidth
		public int width;

		/** The height of the rectangle of boards to allocate, in triads. */
		@ValidTriadHeight
		public int height;
	}

	/** Describes a request for a specific board. */
	public static class SpecificBoard {
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
