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

import static java.util.Arrays.asList;

import java.time.Duration;
import java.util.List;

/**
 * A request to create a job.
 *
 * @author Donal Fellows
 */
public class CreateJob {
	private Duration keepaliveInterval;

	private List<Integer> dimensions;

	private String machineName;

	private List<String> tags;

	private Integer maxDeadBoards;

	/**
	 * Create a request to run on a single board using the default machine
	 * operated by the Spalloc service.
	 * <p>
	 * Note that you can configure this request further.
	 */
	public CreateJob() {
		dimensions = asList(1);
		tags = asList("default");
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
		dimensions = asList(numBoards);
		tags = asList("default");
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
		dimensions = asList(width, height);
		tags = asList("default");
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
	 *            Which board of the machine to request?
	 */
	public CreateJob(String machine, Triad triad) {
		dimensions = asList(triad.getX(), triad.getY(), triad.getZ());
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
	 * @return 0 to 3 values indicating what size of job to make.
	 *         <ol>
	 *         <li value="0">A single board job. (Default)
	 *         <li>A job with at least the given number of boards.
	 *         <li>An allocation that should incorporate the given number of
	 *         triads of boards in each direction. Be aware that this is in
	 *         triads!
	 *         <li>A specific board, by X, Y, Z (<em>logical</em> coordinates).
	 *         </ol>
	 */
	// TODO: want to support create by XYZ, by CFB, and by board IP address
	// There's really no need to stick to the limitations of the old Python
	// code
	public List<Integer> getDimensions() {
		return dimensions;
	}

	public void setDimensions(List<Integer> dimensions) {
		this.dimensions = dimensions;
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
