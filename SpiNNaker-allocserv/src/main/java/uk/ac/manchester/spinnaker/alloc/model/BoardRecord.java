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

import static java.util.Objects.nonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

/**
 * Model of a board, for configuration purposes.
 *
 * @author Donal Fellows
 */
public class BoardRecord {
	private Integer id;

	private String machineName;

	private Integer x;

	private Integer y;

	private Integer z;

	private Integer cabinet;

	private Integer frame;

	private Integer board;

	private String ipAddress;

	private Boolean enabled;

	private Integer jobId;

	private boolean powered;

	private Instant lastPowerOn;

	private Instant lastPowerOff;

	private List<BoardIssueReport> reports = new ArrayList<>();

	/** @return The board ID, if known. */
	public Integer getId() {
		return id;
	}

	/** @param id The board ID. */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @return Whether we have an ID. */
	public boolean isIdPresent() {
		return nonNull(id);
	}

	/** @return The machine name. */
	@NotNull
	public String getMachineName() {
		return machineName;
	}

	/** @param machineName The machine name. */
	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	/** @return The board X coordinate, if known. */
	public Integer getX() {
		return x;
	}

	/** @param x The board X coordinate. */
	public void setX(Integer x) {
		this.x = x;
	}

	/** @return The board Y coordinate, if known. */
	public Integer getY() {
		return y;
	}

	/** @param y The board Y coordinate. */
	public void setY(Integer y) {
		this.y = y;
	}

	/** @return The board Z coordinate, if known. */
	public Integer getZ() {
		return z;
	}

	/** @param z The board Z coordinate. */
	public void setZ(Integer z) {
		this.z = z;
	}

	/** @return Whether we have a full set of triad coordinates. */
	public boolean isTriadCoordPresent() {
		return nonNull(x) && nonNull(y) && nonNull(z);
	}

	/** @return The cabinet number, if known. */
	public Integer getCabinet() {
		return cabinet;
	}

	/** @param cabinet The cabinet number. */
	public void setCabinet(Integer cabinet) {
		this.cabinet = cabinet;
	}

	/** @return The frame number, if known. */
	public Integer getFrame() {
		return frame;
	}

	/** @param frame The frame number. */
	public void setFrame(Integer frame) {
		this.frame = frame;
	}

	/** @return The board number, if known. */
	public Integer getBoard() {
		return board;
	}

	/** @param board The board number. */
	public void setBoard(Integer board) {
		this.board = board;
	}

	/** @return Whether we have a full set of physical coordinates. */
	public boolean isPhysicalCoordPresent() {
		return nonNull(cabinet) && nonNull(frame) && nonNull(board);
	}

	/** @return The board's IP address, if known. */
	public String getIpAddress() {
		return ipAddress;
	}

	/** @param ipAddress The board's IP address. */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/** @return Whether we have an IP address. */
	public boolean isAddressPresent() {
		return nonNull(ipAddress);
	}

	/**
	 * @return Whether we have either the ID of a board (from a previous lookup)
	 *         or the name of a machine and at least one set of coordinates for
	 *         a board on that machine.
	 */
	@AssertTrue
	boolean isValidBoardLocator() {
		return isIdPresent() || (nonNull(machineName) && (isTriadCoordPresent()
				|| isPhysicalCoordPresent() || isAddressPresent()));
	}

	/** @return Whether the board is enabled. */
	public boolean isEnabled() {
		return enabled;
	}

	/** @return Whether the board enabled state is defined. */
	public boolean isEnabledDefined() {
		return nonNull(enabled);
	}

	/** @param enabled Whether the board is enabled. */
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	/** @return The ID of the job allocated to the board, if any. */
	public Integer getJobId() {
		return jobId;
	}

	/** @return Whether a job is allocated to the board. */
	public boolean isJobAllocated() {
		return nonNull(jobId);
	}

	/** @param jobId The ID of the job allocated to the board. */
	public void setJobId(Integer jobId) {
		this.jobId = jobId;
	}

	/** @return When the board was last powered on, if known. */
	public Instant getLastPowerOn() {
		return lastPowerOn;
	}

	/** @param lastPowerOn When the board was last powered on. */
	public void setLastPowerOn(Instant lastPowerOn) {
		this.lastPowerOn = lastPowerOn;
	}

	/** @return When the board was last powered off, if known. */
	public Instant getLastPowerOff() {
		return lastPowerOff;
	}

	/** @param lastPowerOff When the board was last powered off. */
	public void setLastPowerOff(Instant lastPowerOff) {
		this.lastPowerOff = lastPowerOff;
	}

	/** @return The reports associated with this board. */
	@NotNull
	public List<BoardIssueReport> getReports() {
		return reports;
	}

	/** @param reports The reports associated with this board. */
	public void setReports(List<BoardIssueReport> reports) {
		this.reports = nonNull(reports) ? reports : new ArrayList<>();
	}

	/** @return Whether this board is powered on. */
	public boolean isPowered() {
		return powered;
	}

	/** @param power Whether this board is powered on. */
	public void setPowered(boolean power) {
		powered = power;
	}
}
