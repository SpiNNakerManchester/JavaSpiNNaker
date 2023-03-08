/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.client;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static java.util.Objects.nonNull;

import java.time.Duration;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.errorprone.annotations.Keep;

import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
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
public final class CreateJob {
	private Duration keepaliveInterval;

	@Positive
	private Integer numBoards;

	@Valid
	private Dimensions dimensions;

	@Valid
	private SpecificBoard board;

	private String machineName;

	private List<String> tags;

	@PositiveOrZero
	private Integer maxDeadBoards;

	/**
	 * Used when asking for a rectangle of boards.
	 */
	@JsonAutoDetect(setterVisibility = NON_PRIVATE)
	public static final class Dimensions {
		@ValidTriadWidth
		private int width;

		@ValidTriadHeight
		private int height;

		/**
		 * @param width
		 *            The width of rectangle to ask for, in triads.
		 * @param height
		 *            The height of rectangle to ask for, in triads.
		 */
		public Dimensions(int width, int height) {
			this.width = width;
			this.height = height;
		}

		/** @return The width of the allocation, in triads. */
		public int getWidth() {
			return width;
		}

		void setWidth(int width) {
			this.width = width;
		}

		/** @return The height of the allocation, in triads. */
		public int getHeight() {
			return height;
		}

		void setHeight(int height) {
			this.height = height;
		}
	}

	/**
	 * Used when asking for a specific board.
	 */
	public static final class SpecificBoard {
		@ValidTriadX
		private Integer x;

		@ValidTriadY
		private Integer y;

		@ValidTriadZ
		private Integer z;

		@ValidCabinetNumber
		private Integer cabinet;

		@ValidFrameNumber
		private Integer frame;

		@ValidBoardNumber
		private Integer board;

		@IPAddress(nullOK = true)
		private String address;

		/**
		 * Make an untargeted instance. You must set either the triad triple (x,
		 * y, z), the physical triple (cabinet, frame, board) or the IP address
		 * to use this object.
		 */
		public SpecificBoard() {
		}

		private SpecificBoard(boolean type, int a, int b, int c) {
			if (type) {
				x = a;
				y = b;
				z = c;
			} else {
				cabinet = a;
				frame = b;
				board = c;
			}
		}

		private SpecificBoard(String addr) {
			address = addr;
		}

		/** @return The triad X coordinate. */
		public Integer getX() {
			return x;
		}

		/** @param x The triad X coordinate. */
		public void setX(Integer x) {
			this.x = x;
		}

		/** @return The triad Y coordinate. */
		public Integer getY() {
			return y;
		}

		/** @param y The triad Y coordinate. */
		public void setY(Integer y) {
			this.y = y;
		}

		/** @return The triad Z coordinate. */
		public Integer getZ() {
			return z;
		}

		/** @param z The triad Z coordinate. */
		public void setZ(Integer z) {
			this.z = z;
		}

		/** @return The cabinet number. */
		public Integer getCabinet() {
			return cabinet;
		}

		/** @param cabinet The cabinet number. */
		public void setCabinet(Integer cabinet) {
			this.cabinet = cabinet;
		}

		/** @return The frame number. */
		public Integer getFrame() {
			return frame;
		}

		/** @param frame The frame number. */
		public void setFrame(Integer frame) {
			this.frame = frame;
		}

		/** @return The board number. */
		public Integer getBoard() {
			return board;
		}

		/** @param board The board number. */
		public void setBoard(Integer board) {
			this.board = board;
		}

		/** @return The board IP address. */
		public String getAddress() {
			return address;
		}

		/** @param address The board IP address. */
		public void setAddress(String address) {
			this.address = address;
		}
	}

	// TODO Support a request for dimensions rooted at a board

	/**
	 * Create a request to run on a single board using the default machine
	 * operated by the Spalloc service.
	 * <p>
	 * Note that you can configure this request further.
	 */
	public CreateJob() {
		numBoards = 1;
		tags = List.of("default");
	}

	/**
	 * Create a request to run on a number of boards using the default machine
	 * operated by the Spalloc service.
	 * <p>
	 * Note that you can configure this request further.
	 *
	 * @param numBoards
	 *            The number of boards to ask for.
	 * @throws IllegalArgumentException
	 *             If the number of boards is less than 1
	 */
	public CreateJob(int numBoards) {
		if (numBoards <= 0) {
			throw new IllegalArgumentException(
					"number of boards must be positive");
		}
		this.numBoards = numBoards;
		tags = List.of("default");
	}

	/**
	 * Create a request to run on rectangle of triads of boards using the
	 * default machine operated by the Spalloc service.
	 * <p>
	 * Note that you can configure this request further.
	 *
	 * @param width
	 *            The width of the rectangle, in triads
	 * @param height
	 *            The height of the rectangle, in triads
	 * @throws IllegalArgumentException
	 *             If either of the dimensions is less than 1
	 */
	public CreateJob(int width, int height) {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException(
					"dimensions must be positive");
		}
		dimensions = new Dimensions(width, height);
		tags = List.of("default");
	}

	/**
	 * Create a request to run on a specific board of a specific machine
	 * operated by the Spalloc service.
	 * <p>
	 * Note that you can configure this request further.
	 *
	 * @param machine
	 *            Which machine of the service to use?
	 * @param triad
	 *            Which board of the machine to request? This is the logical
	 *            coordinates.
	 */
	public CreateJob(String machine, TriadCoords triad) {
		board = new SpecificBoard(true, triad.x, triad.y, triad.z);
		machineName = machine;
	}

	/**
	 * Create a request to run on a specific board of a specific machine
	 * operated by the Spalloc service.
	 * <p>
	 * Note that you can configure this request further.
	 *
	 * @param machine
	 *            Which machine of the service to use?
	 * @param coords
	 *            The physical coordinates of the board to request.
	 */
	public CreateJob(String machine, PhysicalCoords coords) {
		this.board = new SpecificBoard(false, coords.c, coords.f, coords.b);
		machineName = machine;
	}

	/**
	 * Create a request to run on a specific board of a specific machine
	 * operated by the Spalloc service.
	 * <p>
	 * Note that you can configure this request further.
	 *
	 * @param machine
	 *            Which machine of the service to use?
	 * @param ipAddress
	 *            The IP address of the board of the machine to request
	 */
	public CreateJob(String machine, String ipAddress) {
		board = new SpecificBoard(ipAddress);
		machineName = machine;
	}

	/**
	 * @return How long after a keepalive message will the job be auto-deleted?
	 *         <em>Required.</em> Must be between 30 and 300 seconds.
	 */
	public Duration getKeepaliveInterval() {
		return keepaliveInterval;
	}

	/**
	 * @param keepaliveInterval
	 *            How long after a keepalive message will the job be
	 *            auto-deleted? <em>Required.</em> Must be between 30 and 300
	 *            seconds.
	 */
	public void setKeepaliveInterval(Duration keepaliveInterval) {
		this.keepaliveInterval = keepaliveInterval;
	}

	/**
	 * @return The number of boards to request. May be {@code null} if a
	 *         different type of request is made.
	 */
	public Integer getNumBoards() {
		return numBoards;
	}

	/**
	 * @param numBoards
	 *            The number of boards to request.
	 */
	public void setNumBoards(Integer numBoards) {
		this.numBoards = numBoards;
	}

	/** @return The size of rectangle of boards to request. */
	public Dimensions getDimensions() {
		return dimensions;
	}

	/** @param dimensions The size of rectangle of boards to request. */
	public void setDimensions(Dimensions dimensions) {
		this.dimensions = dimensions;
	}

	/** @return The address of the specific board to request. */
	public SpecificBoard getBoard() {
		return board;
	}

	/** @param board The address of the specific board to request. */
	public void setBoard(SpecificBoard board) {
		this.board = board;
	}

	/**
	 * @return Which machine to allocate on. This and {@code tags} are mutually
	 *         exclusive, but at least one must be given.
	 */
	public String getMachineName() {
		return machineName;
	}

	/**
	 * @param machineName
	 *            Which machine to allocate on. This and {@code tags} are
	 *            mutually exclusive, but at least one must be given.
	 */
	public void setMachineName(String machineName) {
		this.machineName = machineName;
		this.tags = null;
	}

	/**
	 * @return The tags to select which machine to allocate on. This and
	 *         {@code machineName} are mutually exclusive, but at least one must
	 *         be given.
	 */
	public List<String> getTags() {
		return tags;
	}

	/**
	 * @param tags
	 *            The tags to select which machine to allocate on. This and
	 *            {@code machineName} are mutually exclusive, but at least one
	 *            must be given.
	 */
	public void setTags(List<String> tags) {
		this.tags = tags;
		this.machineName = null;
	}

	@Keep
	@AssertTrue(message = "either machineName or tags must be given")
	private boolean isTargetted() {
		return nonNull(machineName) || (nonNull(tags) && !tags.isEmpty());
	}

	/**
	 * @return The maximum number of dead boards allowed in a rectangular
	 *         allocation. Note that the allocation engine might increase this
	 *         if it decides to overallocate.
	 */
	public Integer getMaxDeadBoards() {
		return maxDeadBoards;
	}

	/**
	 * @param maxDeadBoards
	 *            The maximum number of dead boards allowed in a rectangular
	 *            allocation. Note that the allocation engine might increase
	 *            this if it decides to overallocate.
	 */
	public void setMaxDeadBoards(Integer maxDeadBoards) {
		this.maxDeadBoards = maxDeadBoards;
	}
}
