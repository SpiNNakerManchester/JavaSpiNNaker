/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocProtocolException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocProtocolTimeoutException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.JobsChangedNotification;
import uk.ac.manchester.spinnaker.spalloc.messages.Machine;
import uk.ac.manchester.spinnaker.spalloc.messages.MachinesChangedNotification;
import uk.ac.manchester.spinnaker.spalloc.messages.Notification;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * The interface exposed by the low-level Spalloc Client.
 *
 * @author Donal Fellows
 */
@UsedInJavadocOnly({
	JobsChangedNotification.class,
	MachinesChangedNotification.class
})
public interface SpallocAPI {
	/**
	 * Request the version of the spalloc server.
	 *
	 * @return the server's version.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default Version version()
			throws IOException, SpallocServerException, InterruptedException {
		return version(null);
	}

	/**
	 * Request the version of the spalloc server.
	 *
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return the server's version.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	Version version(@Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Create a job.
	 *
	 * @param args
	 *            The arguments, describing default (empty), the number of
	 *            boards (one arg), the triad size (two args) or the board
	 *            location (three args).
	 * @param kwargs
	 *            Additional arguments required. Must include the key
	 *            {@code owner}. Values can be boxed primitive types or strings.
	 * @return the ID of the created job.
	 * @deprecated Consider using {@link #createJob(CreateJob)} instead.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	@Deprecated(forRemoval = true)
	default int createJob(List<@PositiveOrZero Integer> args,
			Map<@NotBlank String, @NotNull Object> kwargs)
			throws IOException, SpallocServerException, InterruptedException {
		return createJob(args, kwargs, null);
	}

	/**
	 * Create a job.
	 *
	 * @param builder
	 *            The builder saying what sort of job to create.
	 * @return the ID of the created job.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default int createJob(@Valid CreateJob builder)
			throws IOException, SpallocServerException, InterruptedException {
		return createJob(builder, null);
	}

	/**
	 * Create a job.
	 *
	 * @param builder
	 *            The builder saying what sort of job to create.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return the ID of the created job.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	int createJob(@Valid CreateJob builder, @Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Create a job.
	 *
	 * @param args
	 *            The arguments, describing default (empty), the number of
	 *            boards (one arg), the triad size (two args) or the board
	 *            location (three args).
	 * @param kwargs
	 *            Additional arguments required. Must include the key
	 *            {@code owner}. Values can be boxed primitive types or strings.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return the ID of the created job.
	 * @deprecated Consider using {@link #createJob(CreateJob, Integer)}
	 *             instead.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	@Deprecated(forRemoval = true) // TODO remove this
	int createJob(List<@PositiveOrZero Integer> args,
			Map<@NotBlank String, @NotNull Object> kwargs,
			@Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Keep a job alive. Needs to be regularly called.
	 *
	 * @param jobID
	 *            The job to ask about.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default void jobKeepAlive(int jobID)
			throws IOException, SpallocServerException, InterruptedException {
		jobKeepAlive(jobID, null);
	}

	/**
	 * Keep a job alive. Needs to be regularly called.
	 *
	 * @param jobID
	 *            The job to ask about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	void jobKeepAlive(int jobID, @Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Get the state of a job.
	 *
	 * @param jobID
	 *            The job to get the state of.
	 * @return a description of the job's state.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default JobState getJobState(int jobID)
			throws IOException, SpallocServerException, InterruptedException {
		return getJobState(jobID, null);
	}

	/**
	 * Get the state of a job.
	 *
	 * @param jobID
	 *            The job to get the state of.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return a description of the job's state.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	JobState getJobState(int jobID, @Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Get information about a job's allocated machine.
	 *
	 * @param jobID
	 *            The job whose machine you want to ask about.
	 * @return a description of the machine.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default JobMachineInfo getJobMachineInfo(int jobID)
			throws IOException, SpallocServerException, InterruptedException {
		return getJobMachineInfo(jobID, null);
	}

	/**
	 * Get information about a job's allocated machine.
	 *
	 * @param jobID
	 *            The job whose machine you want to ask about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return a description of the machine.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	JobMachineInfo getJobMachineInfo(int jobID, @Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Turn on a job's allocated boards.
	 *
	 * @param jobID
	 *            The job to request about.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default void powerOnJobBoards(int jobID)
			throws IOException, SpallocServerException, InterruptedException {
		powerOnJobBoards(jobID, null);
	}

	/**
	 * Turn on a job's allocated boards.
	 *
	 * @param jobID
	 *            The job to request about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	void powerOnJobBoards(int jobID, @Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Turn off a job's allocated boards.
	 *
	 * @param jobID
	 *            The job to request about.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default void powerOffJobBoards(int jobID)
			throws IOException, SpallocServerException, InterruptedException {
		powerOffJobBoards(jobID, null);
	}

	/**
	 * Turn off a job's allocated boards.
	 *
	 * @param jobID
	 *            The job to request about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	void powerOffJobBoards(int jobID, @Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Destroy a job.
	 *
	 * @param jobID
	 *            The ID of the job.
	 * @param reason
	 *            Why the job is to be destroyed.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default void destroyJob(int jobID, @NotBlank String reason)
			throws IOException, SpallocServerException, InterruptedException {
		destroyJob(jobID, reason, null);
	}

	/**
	 * Destroy a job.
	 *
	 * @param jobID
	 *            The ID of the job.
	 * @param reason
	 *            Why the job is to be destroyed.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	void destroyJob(int jobID, @NotBlank String reason,
			@Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Enable or disable notifications of changes in job state.
	 *
	 * @param jobID
	 *            The job to request (or cancel requests) about, or {@code null}
	 *            to be notified/not notified about all jobs.
	 * @param enable
	 *            True to enable notifications, false to disable them.
	 * @see JobsChangedNotification
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default void notifyJob(Integer jobID, boolean enable)
			throws IOException, SpallocServerException, InterruptedException {
		notifyJob(jobID, enable, null);
	}

	/**
	 * Enable or disable notifications of changes in job state.
	 *
	 * @param jobID
	 *            The job to request (or cancel requests) about, or {@code null}
	 *            to be notified/not notified about all jobs.
	 * @param enable
	 *            True to enable notifications, false to disable them.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @see JobsChangedNotification
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	void notifyJob(Integer jobID, boolean enable, @Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Enable or disable notifications of changes in machine state.
	 *
	 * @param machineName
	 *            The machine to request (or cancel requests) about, or
	 *            {@code null} to be notified/not notified about all machines
	 *            (known to spalloc).
	 * @param enable
	 *            True to enable notifications, false to disable them.
	 * @see MachinesChangedNotification
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default void notifyMachine(String machineName, boolean enable)
			throws IOException, SpallocServerException, InterruptedException {
		notifyMachine(machineName, enable, null);
	}

	/**
	 * Enable or disable notifications of changes in machine state.
	 *
	 * @param machineName
	 *            The machine to request (or cancel requests) about, or
	 *            {@code null} to be notified/not notified about all machines
	 *            (known to spalloc).
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @param enable
	 *            True to enable notifications, false to disable them.
	 * @see MachinesChangedNotification
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	void notifyMachine(String machineName, boolean enable,
			@Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * List all jobs.
	 *
	 * @return A list of allocated/queued jobs in order of creation from oldest
	 *         (first) to newest (last). This is unmodifiable.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default List<JobDescription> listJobs()
			throws IOException, SpallocServerException, InterruptedException {
		return listJobs(null);
	}

	/**
	 * List all jobs.
	 *
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return A list of allocated/queued jobs in order of creation from oldest
	 *         (first) to newest (last). This is unmodifiable.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	List<JobDescription> listJobs(@Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * List all known machines.
	 *
	 * @return The list of machines known to the system in order of priority
	 *         from highest (first) to lowest (last). This is unmodifiable.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default List<Machine> listMachines()
			throws IOException, SpallocServerException, InterruptedException {
		return listMachines(null);
	}

	/**
	 * List all known machines.
	 *
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return The list of machines known to the system in order of priority
	 *         from highest (first) to lowest (last). This is unmodifiable.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	List<Machine> listMachines(@Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Get the physical location of a board.
	 *
	 * @param machineName
	 *            the name of the machine containing the board.
	 * @param coords
	 *            the logical location of the board.
	 * @return the physical location, or {@code null} if the logical location
	 *         doesn't map to a real board.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default BoardPhysicalCoordinates getBoardPosition(
			@NotBlank String machineName, @Valid BoardCoordinates coords)
			throws IOException, SpallocServerException, InterruptedException {
		return getBoardPosition(machineName, coords, null);
	}

	/**
	 * Get the physical location of a board.
	 *
	 * @param machineName
	 *            the name of the machine containing the board.
	 * @param coords
	 *            the logical location of the board.
	 * @return the physical location, or {@code null} if the logical location
	 *         doesn't map to a real board.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default BoardPhysicalCoordinates getBoardPosition(
			@NotBlank String machineName, @Valid TriadCoords coords)
			throws IOException, SpallocServerException, InterruptedException {
		return getBoardPosition(machineName, coords, null);
	}

	/**
	 * Get the physical location of a board.
	 *
	 * @param machineName
	 *            the name of the machine containing the board.
	 * @param coords
	 *            the logical location of the board.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return the physical location, or {@code null} if the logical location
	 *         doesn't map to a real board.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default BoardPhysicalCoordinates getBoardPosition(
			@NotBlank String machineName, @Valid BoardCoordinates coords,
			@Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException {
		return getBoardPosition(machineName, coords.toStandardCoords(),
				timeout);
	}

	/**
	 * Get the physical location of a board.
	 *
	 * @param machineName
	 *            the name of the machine containing the board.
	 * @param coords
	 *            the logical location of the board.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return the physical location, or {@code null} if the logical location
	 *         doesn't map to a real board.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	BoardPhysicalCoordinates getBoardPosition(@NotBlank String machineName,
			@Valid TriadCoords coords, @Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Get the logical location of a board.
	 *
	 * @param machineName
	 *            the name of the machine containing the board.
	 * @param coords
	 *            the physical location of the board.
	 * @return the logical location, or {@code null} if the physical location
	 *         doesn't map to a real board.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default BoardCoordinates getBoardPosition(@NotBlank String machineName,
			@Valid BoardPhysicalCoordinates coords)
			throws IOException, SpallocServerException, InterruptedException {
		return getBoardPosition(machineName, coords, null);
	}

	/**
	 * Get the logical location of a board.
	 *
	 * @param machineName
	 *            the name of the machine containing the board.
	 * @param coords
	 *            the physical location of the board.
	 * @return the logical location, or {@code null} if the physical location
	 *         doesn't map to a real board.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default BoardCoordinates getBoardPosition(@NotBlank String machineName,
			@Valid PhysicalCoords coords)
			throws IOException, SpallocServerException, InterruptedException {
		return getBoardPosition(machineName, coords, null);
	}

	/**
	 * Get the logical location of a board.
	 *
	 * @param machineName
	 *            the name of the machine containing the board.
	 * @param coords
	 *            the physical location of the board.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return the logical location, or {@code null} if the physical location
	 *         doesn't map to a real board.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default BoardCoordinates getBoardPosition(@NotBlank String machineName,
			@Valid BoardPhysicalCoordinates coords, @Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException {
		return getBoardPosition(machineName, coords.toStandardCoords(),
				timeout);
	}

	/**
	 * Get the logical location of a board.
	 *
	 * @param machineName
	 *            the name of the machine containing the board.
	 * @param coords
	 *            the physical location of the board.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return the logical location, or {@code null} if the physical location
	 *         doesn't map to a real board.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	BoardCoordinates getBoardPosition(@NotBlank String machineName,
			@Valid PhysicalCoords coords, @Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Locate a chip within a job's allocation.
	 *
	 * @param jobID
	 *            The job to ask about.
	 * @param chip
	 *            The coordinates of the chip to ask about.
	 * @return A description of the chip's location, or {@code null} if it can't
	 *         be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default WhereIs whereIs(int jobID, @Valid HasChipLocation chip)
			throws IOException, SpallocServerException, InterruptedException {
		return whereIs(jobID, chip, null);
	}

	/**
	 * Locate a chip within a job's allocation.
	 *
	 * @param jobID
	 *            The job to ask about.
	 * @param chip
	 *            The coordinates of the chip to ask about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return A description of the chip's location, or {@code null} if it can't
	 *         be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	WhereIs whereIs(int jobID, @Valid HasChipLocation chip,
			@Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Locate a chip within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param chip
	 *            The coordinates of the chip to ask about.
	 * @return A description of the chip's location, or {@code null} if it can't
	 *         be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default WhereIs whereIs(@NotBlank String machine,
			@Valid HasChipLocation chip)
			throws IOException, SpallocServerException, InterruptedException {
		return whereIs(machine, chip, null);
	}

	/**
	 * Locate a chip within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param chip
	 *            The coordinates of the chip to ask about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return A description of the chip's location, or {@code null} if it can't
	 *         be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	WhereIs whereIs(@NotBlank String machine, @Valid HasChipLocation chip,
			@Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Locate a board within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The physical coordinates of the board to ask about.
	 * @return A description of the board's location, or {@code null} if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default WhereIs whereIs(@NotBlank String machine,
			@Valid BoardPhysicalCoordinates coords)
			throws IOException, SpallocServerException, InterruptedException {
		return whereIs(machine, coords, null);
	}

	/**
	 * Locate a board within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The physical coordinates of the board to ask about.
	 * @return A description of the board's location, or {@code null} if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default WhereIs whereIs(@NotBlank String machine,
			@Valid PhysicalCoords coords)
			throws IOException, SpallocServerException, InterruptedException {
		return whereIs(machine, coords, null);
	}

	/**
	 * Locate a board within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The physical coordinates of the board to ask about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return A description of the board's location, or {@code null} if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default WhereIs whereIs(@NotBlank String machine,
			@Valid BoardPhysicalCoordinates coords, @Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException {
		return whereIs(machine, coords.toStandardCoords(), timeout);
	}

	/**
	 * Locate a board within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The physical coordinates of the board to ask about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return A description of the board's location, or {@code null} if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	WhereIs whereIs(@NotBlank String machine, @Valid PhysicalCoords coords,
			@Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Locate a board within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The logical coordinates of the board to ask about.
	 * @return A description of the board's location, or {@code null} if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default WhereIs whereIs(@NotBlank String machine,
			@Valid BoardCoordinates coords)
			throws IOException, SpallocServerException, InterruptedException {
		return whereIs(machine, coords, null);
	}

	/**
	 * Locate a board within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The logical coordinates of the board to ask about.
	 * @return A description of the board's location, or {@code null} if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default WhereIs whereIs(@NotBlank String machine, @Valid TriadCoords coords)
			throws IOException, SpallocServerException, InterruptedException {
		return whereIs(machine, coords, null);
	}

	/**
	 * Locate a board within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The logical coordinates of the board to ask about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return A description of the board's location, or {@code null} if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	default WhereIs whereIs(@NotBlank String machine,
			@Valid BoardCoordinates coords, @Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException {
		return whereIs(machine, coords.toStandardCoords(), timeout);
	}

	/**
	 * Locate a board within a machine.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The logical coordinates of the board to ask about.
	 * @param timeout
	 *            How long to wait for the request to complete, in milliseconds,
	 *            or {@code null} to wait forever.
	 * @return A description of the board's location, or {@code null} if it
	 *         can't be found.
	 * @throws SpallocServerException
	 *             if the server returns an exception response.
	 * @throws SpallocProtocolTimeoutException
	 *             if the request times out.
	 * @throws IOException
	 *             if network communications fail.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	WhereIs whereIs(@NotBlank String machine, @Valid TriadCoords coords,
			@Positive Integer timeout)
			throws IOException, SpallocProtocolTimeoutException,
			SpallocServerException, InterruptedException;

	/**
	 * Return the next notification to arrive. Waits indefinitely.
	 *
	 * @return The notification sent by the server.
	 * @see JobsChangedNotification
	 * @see MachinesChangedNotification
	 * @throws SpallocProtocolException
	 *             If the socket is unusable or becomes disconnected.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 * @throws RuntimeException
	 *             If there is a timeout.
	 */
	default Notification waitForNotification()
			throws SpallocProtocolException, InterruptedException {
		try {
			return waitForNotification(null);
		} catch (SpallocProtocolTimeoutException e) {
			throw new RuntimeException("unexpected timeout", e);
		}
	}

	/**
	 * Return the next notification to arrive.
	 *
	 * @param timeout
	 *            The number of seconds to wait before timing out or
	 *            {@code null} if this function should try again forever.
	 *            <p>
	 *            If negative, only responses already-received will be returned;
	 *            if no responses are available, in this case the function does
	 *            not raise a {@link SpallocProtocolTimeoutException} but
	 *            returns {@code null} instead.
	 * @return The notification sent by the server.
	 * @see JobsChangedNotification
	 * @see MachinesChangedNotification
	 * @throws SpallocProtocolTimeoutException
	 *             If a timeout occurs (implying {@code timeout} is not
	 *             negative).
	 * @throws SpallocProtocolException
	 *             If the socket is unusable or becomes disconnected.
	 * @throws InterruptedException
	 *             If interrupted while waiting.
	 */
	Notification waitForNotification(Integer timeout)
			throws SpallocProtocolException, SpallocProtocolTimeoutException,
			InterruptedException;
}
