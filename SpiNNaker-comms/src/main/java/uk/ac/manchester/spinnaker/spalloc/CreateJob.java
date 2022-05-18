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
package uk.ac.manchester.spinnaker.spalloc;

import static uk.ac.manchester.spinnaker.spalloc.JobConstants.KEEPALIVE_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.KEEPALIVE_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MACHINE_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_BOARDS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_LINKS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MIN_RATIO_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.REQUIRE_TORUS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.TAGS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.USER_PROPERTY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.manchester.spinnaker.spalloc.messages.CreateJobCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;

/**
 * An abstract, high-level request to create a job.
 *
 * @author Donal Fellows
 */
public class CreateJob {
	private final List<Integer> args = new ArrayList<>();

	private final Map<String, Object> kwargs = new HashMap<>();

	private boolean setTags = false;

	private boolean setMachine = false;

	private boolean setOwner = false;

	/**
	 * Build a request for a single board.
	 */
	public CreateJob() {
	}

	/**
	 * Build a request for a number of boards.
	 *
	 * @param numBoards
	 *            How many boards to request.
	 */
	public CreateJob(int numBoards) {
		args.add(numBoards);
	}

	/**
	 * Build a request for a rectangle of boards.
	 *
	 * @param width
	 *            Horizontal size of rectangle
	 * @param height
	 *            Vertical size of rectangle
	 */
	public CreateJob(int width, int height) {
		args.add(width);
		args.add(height);
	}

	/**
	 * Build a request for a specific board.
	 *
	 * @param x
	 *            First coordinate
	 * @param y
	 *            Second coordinate
	 * @param z
	 *            Third coordinate
	 * @see WhereIs
	 */
	public CreateJob(int x, int y, int z) {
		args.add(x);
		args.add(y);
		args.add(z);
	}

	static {
		WhereIs.class.hashCode(); // NEEDED FOR JAVADOC @see ABOVE
	}

	/**
	 * Create the actual command to send.
	 *
	 * @return The command.
	 * @throws IllegalStateException
	 *             If owner is not given
	 */
	public CreateJobCommand build() {
		if (!setOwner) {
			throw new IllegalStateException(
					"owner must be set before building");
		}
		return new CreateJobCommand(args, kwargs);
	}

	/**
	 * @param owner
	 *            The name of the owner of this job. <strong>Required.</strong>
	 * @return {@code this} (fluent interface)
	 */
	public CreateJob owner(String owner) {
		kwargs.put(USER_PROPERTY, owner);
		setOwner = true;
		return this;
	}

	/**
	 * @param keepalive
	 *            The maximum number of seconds which may elapse between a query
	 *            on this job before it is automatically destroyed. If
	 *            {@code null}, no timeout is used. (Default: 60.0)
	 * @return {@code this} (fluent interface)
	 */
	public CreateJob keepAlive(Double keepalive) {
		kwargs.put(KEEPALIVE_PROPERTY, keepalive);
		return this;
	}

	/**
	 * @param keepalive
	 *            The maximum number of seconds which may elapse between a query
	 *            on this job before it is automatically destroyed. (Default:
	 *            60.0)
	 * @return {@code this} (fluent interface)
	 */
	public CreateJob keepAlive(double keepalive) {
		kwargs.put(KEEPALIVE_PROPERTY, keepalive);
		return this;
	}

	/**
	 * @param machine
	 *            Specify the name of a machine which this job must be executed
	 *            on. If not specified, the first suitable machine available
	 *            will be used, according to the tags selected below. Must be
	 *            not specified when tags are given.
	 * @return {@code this} (fluent interface)
	 * @throws IllegalStateException
	 *             If tags are already given
	 */
	public CreateJob machine(String machine) {
		if (setTags) {
			throw new IllegalStateException("tags already set");
		}
		kwargs.put(MACHINE_PROPERTY, machine);
		setMachine = true;
		return this;
	}

	/**
	 * @param tags
	 *            The set of tags which any machine running this job must have.
	 *            If none are supplied, only machines with the “{@code default}”
	 *            tag will be used. If {@code machine} is given, this argument
	 *            must be not supplied. (Default: empty list)
	 * @return {@code this} (fluent interface)
	 * @throws IllegalStateException
	 *             If machine is already given
	 */
	public CreateJob tags(String... tags) {
		if (setMachine) {
			throw new IllegalStateException("machine already set");
		}
		kwargs.put(TAGS_PROPERTY, tags);
		setTags = true;
		return this;
	}

	/**
	 * @param minRatio
	 *            The aspect ratio (h/w) which the allocated region must be ‘at
	 *            least as square as’. Set to {@code 0.0} for any allowable
	 *            shape, {@code 1.0} to be exactly square. Ignored when
	 *            allocating single boards or specific rectangles of triads.
	 * @return {@code this} (fluent interface)
	 */
	public CreateJob minRatio(double minRatio) {
		kwargs.put(MIN_RATIO_PROPERTY, minRatio);
		return this;
	}

	/**
	 * @param maxDeadBoards
	 *            The maximum number of broken or unreachable boards to allow in
	 *            the allocated region. If unspecified, any number of dead
	 *            boards is permitted, as long as the board on the bottom-left
	 *            corner is alive.
	 * @return {@code this} (fluent interface)
	 */
	public CreateJob maxDeadBoards(int maxDeadBoards) {
		kwargs.put(MAX_DEAD_BOARDS_PROPERTY, maxDeadBoards);
		return this;
	}

	/**
	 * @param maxDeadLinks
	 *            The maximum number of broken links allow in the allocated
	 *            region. When {@code require_torus} is true this includes
	 *            wrap-around links, otherwise peripheral links are not counted.
	 *            If unspecified, any number of broken links is allowed.
	 * @return {@code this} (fluent interface)
	 */
	public CreateJob maxDeadLinks(int maxDeadLinks) {
		kwargs.put(MAX_DEAD_LINKS_PROPERTY, maxDeadLinks);
		return this;
	}

	/**
	 * Equivalent to: {@link #requireTorus(boolean) requireTorus(true)}.
	 *
	 * @return {@code this} (fluent interface)
	 */
	public CreateJob requireTorus() {
		return requireTorus(true);
	}

	/**
	 * @param requireTorus
	 *            If {@code true}, only allocate blocks with torus connectivity.
	 *            In general this will only succeed for requests to allocate an
	 *            entire machine (when the machine is otherwise not in use!).
	 *            Must be {@code false} (the default) when allocating boards.
	 * @return {@code this} (fluent interface)
	 */
	public CreateJob requireTorus(boolean requireTorus) {
		kwargs.put(REQUIRE_TORUS_PROPERTY, requireTorus);
		return this;
	}

	/**
	 * @return The current keepalive value in the builder.
	 */
	public Double getKeepAlive() {
		return (Double) kwargs.getOrDefault(KEEPALIVE_PROPERTY,
				KEEPALIVE_DEFAULT);
	}
}
