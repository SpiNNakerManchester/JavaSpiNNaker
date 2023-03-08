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
package uk.ac.manchester.spinnaker.alloc.model;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Entry in a table of machines. The table is like this:
 *
 * <table border="1">
 * <caption style="display:none">Job List</caption>
 * <tr>
 * <th>ID
 * <th>State
 * <th>Power
 * <th>Boards
 * <th>Machine
 * <th>Created at
 * <th>Keepalive
 * <th>Owner (Host)
 * <tr>
 * <td>6035859
 * <td>ready
 * <td>on
 * <td>1
 * <td>SpiNNaker1M
 * <td>09/07/2021 06:23:25
 * <td>60.0
 * <td>NMPI (10.11.192.12)
 * <tr>
 * <td>6035890
 * <td>ready
 * <td>on
 * <td>1
 * <td>SpiNNaker1M
 * <td>09/07/2021 07:41:06
 * <td>60.0
 * <td>NMPI (10.11.192.12)
 * <tr>
 * <td>6037654
 * <td>ready
 * <td>on
 * <td>1
 * <td>SpiNNaker1M
 * <td>11/07/2021 13:04:43
 * <td>60.0
 * <td>NMPI (10.11.192.12)
 * <tr>
 * <td colspan=8>...
 * </table>
 */
public class JobListEntryRecord {
	private int id;

	private URI detailsUrl;

	@NotNull
	private JobState state;

	private boolean powered;

	@Positive
	private Integer numBoards;

	@PositiveOrZero
	private int machineId;

	@NotBlank
	private String machineName;

	private URI machineUrl;

	private Instant creationTimestamp;

	private Duration keepaliveInterval;

	private String owner;

	private String host;

	/** @return the job's ID */
	public int getId() {
		return id;
	}

	/** @param id the job's ID */
	public void setId(int id) {
		this.id = id;
	}

	/** @return the URL for more details */
	public Optional<URI> getDetailsUrl() {
		return Optional.ofNullable(detailsUrl);
	}

	/** @param detailsUrl the URL for more details */
	public void setDetailsUrl(URI detailsUrl) {
		this.detailsUrl = detailsUrl;
	}

	/** @return the job state */
	public JobState getState() {
		return state;
	}

	/** @param state the job state */
	public void setState(JobState state) {
		this.state = state;
	}

	/** @return whether the job's boards are powered on */
	public boolean isPowered() {
		return powered;
	}

	/** @param powered whether the job's boards are powered on */
	public void setPowered(boolean powered) {
		this.powered = powered;
	}

	/** @return the number of boards allocated to the job */
	public Optional<Integer> getNumBoards() {
		return Optional.ofNullable(numBoards);
	}

	/** @param numBoards the number of boards allocated to the job */
	public void setNumBoards(Integer numBoards) {
		this.numBoards = numBoards;
	}

	/** @return the ID of the machine that the job is using */
	public int getMachineId() {
		return machineId;
	}

	/** @param machineId the ID of the machine that the job is using */
	public void setMachineId(int machineId) {
		this.machineId = machineId;
	}

	/** @return the name of the machine that the job is using */
	public String getMachineName() {
		return machineName;
	}

	/** @param machineName the name of the machine that the job is using */
	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	/** @return the URL for machine info */
	public Optional<URI> getMachineUrl() {
		return Optional.ofNullable(machineUrl);
	}

	/** @param machineUrl the URL for machine info */
	public void setMachineUrl(URI machineUrl) {
		this.machineUrl = machineUrl;
	}

	/** @return the time of the job's creation */
	public Instant getCreationTimestamp() {
		return creationTimestamp;
	}

	/** @param creationTimestamp the time of the job's creation */
	public void setCreationTimestamp(Instant creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}

	/** @return the keepalive interval */
	public Duration getKeepaliveInterval() {
		return keepaliveInterval;
	}

	/** @param keepaliveInterval the keepalive interval */
	public void setKeepaliveInterval(Duration keepaliveInterval) {
		this.keepaliveInterval = keepaliveInterval;
	}

	/** @return the owner, if not shrouded */
	public Optional<String> getOwner() {
		return Optional.ofNullable(owner);
	}

	/** @param owner the owner */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/** @return the host keeping things alive, if not shrouded */
	public Optional<String> getHost() {
		return Optional.ofNullable(host);
	}

	/** @param host the host keeping things alive */
	public void setHost(String host) {
		this.host = host;
	}
}
