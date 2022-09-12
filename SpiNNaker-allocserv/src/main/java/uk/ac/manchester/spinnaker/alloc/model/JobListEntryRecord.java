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
package uk.ac.manchester.spinnaker.alloc.model;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import uk.ac.manchester.spinnaker.machine.tags.IPAddress;

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

	private int machineId;

	@NotBlank
	private String machineName;

	private URI machineUrl;

	@NotNull
	private Instant creationTimestamp;

	@NotNull
	private Duration keepaliveInterval;

	private String owner;

	@IPAddress
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
