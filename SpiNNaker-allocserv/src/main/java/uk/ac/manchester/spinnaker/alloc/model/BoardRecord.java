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

import static java.util.Objects.nonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

import com.google.errorprone.annotations.Keep;

import uk.ac.manchester.spinnaker.machine.board.ValidBoardNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidCabinetNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidFrameNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadX;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadY;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * Model of a board, for configuration purposes.
 */
public class BoardRecord {
	// TODO convert to structured form
	private Integer id;

	private Integer bmpId;

	@NotBlank
	private String machineName;

	@ValidTriadX
	private Integer x;

	@ValidTriadY
	private Integer y;

	@ValidTriadZ
	private Integer z;

	@ValidCabinetNumber
	private Integer cabinet;

	@ValidFrameNumber
	private Integer frame;

	@ValidBoardNumber
	private Integer board;

	@IPAddress(nullOK = true, emptyOK = true)
	private String ipAddress;

	private Boolean enabled;

	private Integer jobId;

	private boolean powered;

	private Instant lastPowerOn;

	private Instant lastPowerOff;

	/** The BMP serial number, if known. */
	private String bmpSerial;

	/** The physical board serial number, if known. */
	private String physicalSerial;

	private List<BoardIssueReport> reports = new ArrayList<>();

	/**
	 * {@return {The board ID, if known}}
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the board id.
	 *
	 * @param id
	 *            The board ID.
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * {@return The BMP ID}
	 */

	public Integer getBmpId() {
		return this.bmpId;
	}

	/**
	 * Sets the BMP ID.
	 *
	 * @param bmpId
	 *            The BMP ID.
	 */
	public void setBmpId(Integer bmpId) {
		this.bmpId = bmpId;
	}

	/**
	 * {@return Whether we have an ID}
	 */
	public boolean isIdPresent() {
		return nonNull(id);
	}

	/**
	 * {@return The machine name}
	 */
	public String getMachineName() {
		return machineName;
	}

	/**
	 * Sets the machine name.
	 *
	 * @param machineName
	 *            The machine name.
	 */
	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	/**
	 * {@return The board X coordinate, if known}
	 */
	public Integer getX() {
		return x;
	}

	/**
	 * Set the board X coordinate.
	 *
	 * @param x
	 *            The board X coordinate.
	 */
	public void setX(Integer x) {
		this.x = x;
	}

	/**
	 * {@return The board Y coordinate, if known}
	 */
	public Integer getY() {
		return y;
	}

	/**
	 * Sets the board Y coordinate.
	 *
	 * @param y
	 *            The board Y coordinate.
	 */
	public void setY(Integer y) {
		this.y = y;
	}

	/**
	 * {@return The board Z coordinate, if known}
	 */
	public Integer getZ() {
		return z;
	}

	/**
	 * Sets the board Z coordinate.
	 *
	 * @param z
	 *            The board Z coordinate.
	 */
	public void setZ(Integer z) {
		this.z = z;
	}

	/**
	 * {@return Whether we have a full set of triad coordinates}
	 */
	public boolean isTriadCoordPresent() {
		return nonNull(x) && nonNull(y) && nonNull(z);
	}

	/**
	 * {@return The cabinet number, if known}
	 */
	public Integer getCabinet() {
		return cabinet;
	}

	/**
	 * Sets the cabinet number.
	 *
	 * @param cabinet
	 *            The cabinet number.
	 */
	public void setCabinet(Integer cabinet) {
		this.cabinet = cabinet;
	}

	/**
	 * {@return The frame number, if known}
	 */
	public Integer getFrame() {
		return frame;
	}

	/**
	 * Sets the frame number.
	 *
	 * @param frame
	 *            The frame number.
	 */
	public void setFrame(Integer frame) {
		this.frame = frame;
	}

	/**
	 * {@return The board number, if known}
	 */
	public Integer getBoard() {
		return board;
	}

	/**
	 * Sets the board number.
	 *
	 * @param board
	 *            The board number.
	 */
	public void setBoard(Integer board) {
		this.board = board;
	}

	/**
	 * {@return Whether we have a full set of physical coordinates}
	 */
	public boolean isPhysicalCoordPresent() {
		return nonNull(cabinet) && nonNull(frame) && nonNull(board);
	}

	/**
	 * {@return The board's IP address, if known}
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * Sets the board's IP address.
	 *
	 * @param ipAddress
	 *            The board's IP address.
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * {@return Whether we have an IP address}
	 */
	public boolean isAddressPresent() {
		return nonNull(ipAddress);
	}

	/**
	 * Weather we have a valid location.
	 *
	 * Could be either the ID of a board (from a previous lookup)
	 * or the name of a machine and
	 * at least one set of coordinates for a board on that machine.
	 *
	 * @return True if a there is a valid location.
	 */
	@Keep
	@AssertTrue(message = "board must have some mechanism for being located")
	private boolean isValidBoardLocator() {
		return isIdPresent() || (nonNull(machineName) && (isTriadCoordPresent()
				|| isPhysicalCoordPresent() || isAddressPresent()));
	}

	/**
	 * {@return Whether the board is enabled}
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * {@return Whether the board enabled state is defined}
	 */
	public boolean isEnabledDefined() {
		return nonNull(enabled);
	}

	/**
	 * set whether the board is enabled.
	 *
	 * @param enabled
	 *            Whether the board is enabled.
	 */
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * {@return The ID of the job allocated to the board, if any}
	 */
	public Integer getJobId() {
		return jobId;
	}

	/**
	 * {@return Whether a job is allocated to the board}
	 */
	public boolean isJobAllocated() {
		return nonNull(jobId);
	}

	/**
	 * Set the ID of the job allocated to the board.
	 *
	 * @param jobId
	 *            The ID of the job allocated to the board.
	 */
	public void setJobId(Integer jobId) {
		this.jobId = jobId;
	}

	/**
	 * {@return When the board was last powered on, if known}
	 */
	public Instant getLastPowerOn() {
		return lastPowerOn;
	}

	/**
	 * Set when the board was last powered on.
	 *
	 * @param lastPowerOn
	 *            When the board was last powered on.
	 */
	public void setLastPowerOn(Instant lastPowerOn) {
		this.lastPowerOn = lastPowerOn;
	}

	/**
	 * {@return When the board was last powered off, if known}
	 */
	public Instant getLastPowerOff() {
		return lastPowerOff;
	}

	/**
	 * Sets when the board was last powered off.
	 *
	 * @param lastPowerOff
	 *            When the board was last powered off.
	 */
	public void setLastPowerOff(Instant lastPowerOff) {
		this.lastPowerOff = lastPowerOff;
	}

	/**
	 * The reports associated with this board.
	 *
	 * The list is not modifiable.
     *
	 * @return The reports
	 */
	public List<BoardIssueReport> getReports() {
		return reports;
	}

	/**
	 * Sets the reports associated with this board.
	 *
	 * Overwrites any previous reports stored.
	 *
	 * @param reports
	 *            The reports associated with this board.
	 */
	public void setReports(List<BoardIssueReport> reports) {
		this.reports = nonNull(reports) ? List.copyOf(reports) : List.of();
	}

	/**
	 * {@return Whether this board is powered on}
	 */
	public boolean isPowered() {
		return powered;
	}

	/**
	 * Sets whether this board is powered on.
	 *
	 * @param power
	 *            Whether this board is powered on.
	 */
	public void setPowered(boolean power) {
		powered = power;
	}

	/**
	 * {@return The BMP serial number, if known}
	 */
	public String getBmpSerial() {
		return bmpSerial;
	}

	/**
	 * Sets the BMP serial number.
	 *
	 * @param bmpSerial
	 *            The BMP serial number.
	 */
	public void setBmpSerial(String bmpSerial) {
		this.bmpSerial = bmpSerial;
	}

	/**
	 * {@return The physical board serial number, if known}
	 */
	public String getPhysicalSerial() {
		return physicalSerial;
	}

	/**
	 * Sets the physical board serial number.
	 *
	 * @param physicalSerial
	 *            The physical board serial number.
	 */
	public void setPhysicalSerial(String physicalSerial) {
		this.physicalSerial = physicalSerial;
	}
}
