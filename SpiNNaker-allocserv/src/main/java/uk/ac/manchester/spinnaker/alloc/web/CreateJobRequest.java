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

import java.time.Duration;
import java.util.List;

/**
 * A request to create a job.
 *
 * @author Donal Fellows
 */
public class CreateJobRequest {
	/**
	 * Who owns the job. <em>Required.</em>
	 */
	public String owner;

	/**
	 * How long after a keepalive message will the job be auto-deleted?
	 * <em>Required.</em> Must be between 30 and 300 seconds.
	 */
	public Duration keepaliveInterval;

	/**
	 * 0 to 3 values indicating what size of job to make.
	 * <ol>
	 * <li value="0">A single board job. (Default)
	 * <li>A job with at least the given number of boards.
	 * <li>An allocation that should incorporate the given number of triads of
	 * boards in each direction. Be aware that this is in triads!
	 * <li>A specific board, by X, Y, Z (<em>logical</em> coordinates).
	 * </ol>
	 */
	// TODO: want to support create by XYZ, by CFB, and by board IP address
	// There's really no need to stick to the limitations of the Python code
	public List<Integer> dimensions;

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
	public List<String> tags;

	/**
	 * The maximum number of dead boards allowed in a rectangular allocation.
	 * Note that the allocation engine might increase this if it decides to
	 * overallocate. Defaults to {@code 0}.
	 */
	public Integer maxDeadBoards;
}
