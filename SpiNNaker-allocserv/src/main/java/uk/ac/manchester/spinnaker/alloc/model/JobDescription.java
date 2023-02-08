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
package uk.ac.manchester.spinnaker.alloc.model;

import static java.util.stream.Collectors.summarizingInt;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.copy;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import uk.ac.manchester.spinnaker.machine.ValidMachineHeight;
import uk.ac.manchester.spinnaker.machine.ValidMachineWidth;

/**
 * Descriptive detail for a job. Used for HTML generation, something like:
 *
 * <pre>
     Job ID: 6037654
      Owner: gorp
      State: ready
 Start time: 11/07/2021 13:04:43
  Keepalive: 60 seconds
 Owner host: 192.168.0.33
    Request: Job(1)
 Allocation:  ___
             / . \
             \___/
   Hostname: 10.11.12.13
      Width: 8
     Height: 8
 Num boards: 1
Board power: on
 Running on: SpiNNaker1M
 * </pre>
 *
 * (That's actually slightly edited output from {@code spalloc-job -info})
 */
public class JobDescription {
	private int id;

	private Optional<String> owner = Optional.empty();

	private JobState state;

	private Instant startTime;

	private Duration keepAlive;

	private Optional<String> ownerHost = Optional.empty();

	private String request;

	@ValidMachineWidth
	private Integer width;

	@ValidMachineHeight
	private Integer height;

	private boolean powered;

	@NotBlank
	private String machine;

	private URI machineUrl;

	private List<@Valid BoardCoords> boards = List.of();

	private byte[] requestBytes;

	/** @return the machine ID */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the machine ID
	 */
	public void setId(int id) {
		this.id = id;
	}

	/** @return the job's owner */
	public Optional<String> getOwner() {
		return owner;
	}

	/**
	 * @param owner
	 *            the job's owner
	 */
	public void setOwner(String owner) {
		this.owner = Optional.ofNullable(owner);
	}

	/** @return the job's state */
	public JobState getState() {
		return state;
	}

	/**
	 * @param state
	 *            the job's state
	 */
	public void setState(JobState state) {
		this.state = state;
	}

	/** @return when the job started */
	public Instant getStartTime() {
		return startTime;
	}

	/**
	 * @param startTime
	 *            when the job started
	 */
	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	/** @return the maximum keepalive interval */
	public Duration getKeepAlive() {
		return keepAlive;
	}

	/**
	 * @param keepAlive
	 *            the maximum keepalive interval
	 */
	public void setKeepAlive(Duration keepAlive) {
		this.keepAlive = keepAlive;
	}

	/** @return the owner's host (the one supplying keepalives) */
	public Optional<String> getOwnerHost() {
		return ownerHost;
	}

	/**
	 * @param ownerHost
	 *            the owner's host (the one supplying keepalives)
	 */
	public void setOwnerHost(String ownerHost) {
		this.ownerHost = Optional.ofNullable(ownerHost);
	}

	/** @return the request */
	public String getRequest() {
		return request;
	}

	/**
	 * @param request
	 *            the request
	 */
	public void setRequest(String request) {
		this.request = request;
	}

	/**
	 * @return the width of the allocation in <em>chips</em>, or
	 *         {@link Optional#empty() empty()} if the job is not allocated.
	 */
	public Optional<Integer> getWidth() {
		return Optional.ofNullable(width);
	}

	/**
	 * @param width
	 *            the width of the allocation in <em>chips</em>.
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * @return the height of the allocation in <em>chips</em>, or
	 *         {@link Optional#empty() empty()} if the job is not allocated.
	 */
	public Optional<Integer> getHeight() {
		return Optional.ofNullable(height);
	}

	/**
	 * @param height
	 *            the height of the allocation in <em>chips</em>.
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * @return whether all boards are powered up; unallocated jobs are
	 *         considered unpowered.
	 */
	public boolean isPowered() {
		return powered;
	}

	/**
	 * @param powered
	 *            whether all boards are powered up; unallocated jobs are
	 *            considered unpowered.
	 */
	public void setPowered(boolean powered) {
		this.powered = powered;
	}

	/** @return the machine name */
	public String getMachine() {
		return machine;
	}

	/**
	 * @param machine
	 *            the machine name
	 */
	public void setMachine(String machine) {
		this.machine = machine;
	}

	/** @return the board coordinates of all boards allocated to the job */
	public List<BoardCoords> getBoards() {
		return boards;
	}

	/**
	 * @param boards
	 *            the board coordinates of all boards allocated to the job
	 */
	public void setBoards(List<BoardCoords> boards) {
		this.boards = copy(boards);
	}

	/** @return the URL to get machine information */
	public URI getMachineUrl() {
		return machineUrl;
	}

	/**
	 * @param machineUrl
	 *            the URL to get machine information
	 */
	public void setMachineUrl(URI machineUrl) {
		this.machineUrl = machineUrl;
	}

	/**
	 * @return The serialized request. Needs to be processed to hand out of
	 *         {@link #getRequest()}.
	 */
	public byte[] getRequestBytes() {
		return requestBytes;
	}

	/**
	 * @param bytes
	 *            The serialized request.
	 */
	public void setRequestBytes(byte[] bytes) {
		requestBytes = bytes;
	}

	/**
	 * @return The width of the allocation in triads. 0 if not yet allocated.
	 */
	public int getTriadWidth() {
		var stats = boards.stream().collect(summarizingInt(BoardCoords::getX));
		if (stats.getCount() < 1) {
			return 0;
		}
		return stats.getMax() - stats.getMin() + 1;
	}

	/**
	 * @return The height of the allocation in triads. 0 if not yet allocated.
	 */
	public int getTriadHeight() {
		var stats = boards.stream().collect(summarizingInt(BoardCoords::getY));
		if (stats.getCount() < 1) {
			return 0;
		}
		return stats.getMax() - stats.getMin() + 1;
	}
}
