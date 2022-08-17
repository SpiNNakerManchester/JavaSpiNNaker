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
package uk.ac.manchester.spinnaker.allocator;

import java.time.Duration;
import java.util.List;

/**
 * A request to create a job.
 *
 * @author Donal Fellows
 */
public final class CreateJob {
	private Duration keepaliveInterval;

	private Integer numBoards;

	private Dimensions dimensions;

	private SpecificBoard board;

	private String machineName;

	private List<String> tags;

	private Integer maxDeadBoards;

	/**
	 * Used when asking for a rectangle of boards.
	 */
	public static final class Dimensions {
		private int width;

		private int height;

		public Dimensions(int w, int h) {
			width = w;
			height = h;
		}

		/** @return The width of the allocation, in boards. */
		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		/** @return The height of the allocation, in boards. */
		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}
	}

	/**
	 * Used when asking for a specific board.
	 */
	public static final class SpecificBoard {
		private Integer x;

		private Integer y;

		private Integer z;

		private Integer cabinet;

		private Integer frame;

		private Integer board;

		private String address;

		public SpecificBoard() {
		}

		SpecificBoard(boolean type, int a, int b, int c) {
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

		SpecificBoard(String addr) {
			address = addr;
		}

		/** @return The triad X coordinate. */
		public Integer getX() {
			return x;
		}

		public void setX(Integer x) {
			this.x = x;
		}

		/** @return The triad Y coordinate. */
		public Integer getY() {
			return y;
		}

		public void setY(Integer y) {
			this.y = y;
		}

		/** @return The triad Z coordinate. */
		public Integer getZ() {
			return z;
		}

		public void setZ(Integer z) {
			this.z = z;
		}

		/** @return The cabinet number. */
		public Integer getCabinet() {
			return cabinet;
		}

		public void setCabinet(Integer cabinet) {
			this.cabinet = cabinet;
		}

		/** @return The frame number. */
		public Integer getFrame() {
			return frame;
		}

		public void setFrame(Integer frame) {
			this.frame = frame;
		}

		/** @return The board number. */
		public Integer getBoard() {
			return board;
		}

		public void setBoard(Integer board) {
			this.board = board;
		}

		/** @return The board IP address. */
		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}
	}

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
	 * Create a request to run on rectangle of boards using the default machine
	 * operated by the Spalloc service.
	 * <p>
	 * Note that you can configure this request further.
	 *
	 * @param width
	 *            The width of the rectangle
	 * @param height
	 *            The height of the rectangle
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
	public CreateJob(String machine, Triad triad) {
		board = new SpecificBoard(true, triad.getX(), triad.getY(),
				triad.getZ());
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
	 * @param cabinet
	 *            The cabinet number of the board to request.
	 * @param frame
	 *            The frame number of the board to request.
	 * @param board
	 *            The board number of the board to request.
	 */
	public CreateJob(String machine, int cabinet, int frame, int board) {
		this.board = new SpecificBoard(false, cabinet, frame, board);
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

	public void setNumBoards(Integer numBoards) {
		this.numBoards = numBoards;
	}

	/** @return The size of rectangle of boards to request. */
	public Dimensions getDimensions() {
		return dimensions;
	}

	public void setDimensions(Dimensions dimensions) {
		this.dimensions = dimensions;
	}

	/** @return The address of the specific board to request. */
	public SpecificBoard getBoard() {
		return board;
	}

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

	public void setTags(List<String> tags) {
		this.tags = tags;
		this.machineName = null;
	}

	/**
	 * @return The maximum number of dead boards allowed in a rectangular
	 *         allocation. Note that the allocation engine might increase this
	 *         if it decides to overallocate.
	 */
	public Integer getMaxDeadBoards() {
		return maxDeadBoards;
	}

	public void setMaxDeadBoards(Integer maxDeadBoards) {
		this.maxDeadBoards = maxDeadBoards;
	}
}
