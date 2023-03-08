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
import static uk.ac.manchester.spinnaker.utils.UnitConstants.NSEC_PER_SEC;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import uk.ac.manchester.spinnaker.machine.board.ValidTriadHeight;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadWidth;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadX;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadY;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;
import uk.ac.manchester.spinnaker.spalloc.messages.CreateJobCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * An abstract, high-level request to create a job.
 *
 * @author Donal Fellows
 */
public class CreateJob {
	private final List<@NotNull @PositiveOrZero Integer> args =
			new ArrayList<>();

	private final Map<@NotBlank String, @NotNull Object> kwargs =
			new HashMap<>();

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
	public CreateJob(@Positive int numBoards) {
		args.add(numBoards);
	}

	/**
	 * Build a request for a rectangle of boards.
	 *
	 * @param width
	 *            Horizontal size of rectangle, in triads
	 * @param height
	 *            Vertical size of rectangle, in triads
	 */
	public CreateJob(@ValidTriadWidth int width, @ValidTriadHeight int height) {
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
	@UsedInJavadocOnly(WhereIs.class)
	public CreateJob(@ValidTriadX int x, @ValidTriadY int y,
			@ValidTriadZ int z) {
		args.add(x);
		args.add(y);
		args.add(z);
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
	@CanIgnoreReturnValue
	public CreateJob owner(@NotBlank String owner) {
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
	@CanIgnoreReturnValue
	public CreateJob keepAlive(@Positive Double keepalive) {
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
	@CanIgnoreReturnValue
	public CreateJob keepAlive(@Positive double keepalive) {
		kwargs.put(KEEPALIVE_PROPERTY, keepalive);
		return this;
	}

	/**
	 * @param keepalive
	 *            The maximum amount of time which may elapse between a query on
	 *            this job before it is automatically destroyed. (Default: 60
	 *            seconds)
	 * @return {@code this} (fluent interface)
	 * @throws IllegalArgumentException
	 *             If the duration is negative.
	 */
	@CanIgnoreReturnValue
	public CreateJob keepAlive(@NotNull Duration keepalive) {
		double t = keepalive.getSeconds();
		if (t < 0.0) {
			throw new IllegalArgumentException(
					"negative durations not supported");
		}
		t += keepalive.getNano() / (double) NSEC_PER_SEC;
		kwargs.put(KEEPALIVE_PROPERTY, t);
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
	@CanIgnoreReturnValue
	public CreateJob machine(@NotBlank String machine) {
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
	@CanIgnoreReturnValue
	public CreateJob tags(@NotBlank String... tags) {
		if (setMachine) {
			throw new IllegalStateException("machine already set");
		}
		kwargs.put(TAGS_PROPERTY, tags);
		setTags = true;
		return this;
	}

	/** @return Whether either the machine or the tags are set. */
	boolean isTargetDefined() {
		return setMachine || setTags;
	}

	/**
	 * @param minRatio
	 *            The aspect ratio (h/w) which the allocated region must be ‘at
	 *            least as square as’. Set to {@code 0.0} for any allowable
	 *            shape, {@code 1.0} to be exactly square. Ignored when
	 *            allocating single boards or specific rectangles of triads.
	 * @return {@code this} (fluent interface)
	 */
	@CanIgnoreReturnValue
	public CreateJob minRatio(@PositiveOrZero double minRatio) {
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
	@CanIgnoreReturnValue
	public CreateJob maxDeadBoards(@PositiveOrZero int maxDeadBoards) {
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
	@CanIgnoreReturnValue
	public CreateJob maxDeadLinks(@PositiveOrZero int maxDeadLinks) {
		kwargs.put(MAX_DEAD_LINKS_PROPERTY, maxDeadLinks);
		return this;
	}

	/**
	 * Equivalent to: {@link #requireTorus(boolean) requireTorus(true)}.
	 *
	 * @return {@code this} (fluent interface)
	 * @deprecated You probably can't use this sensibly with the hardware as
	 *             deployed (or you automatically get it when meaningful). The
	 *             default is fine.
	 */
	@CanIgnoreReturnValue
	@Deprecated(forRemoval = true)
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
	 * @deprecated You probably can't use this sensibly with the hardware as
	 *             deployed (or you automatically get it when meaningful). The
	 *             default is fine.
	 */
	@CanIgnoreReturnValue
	@Deprecated(forRemoval = true)
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
