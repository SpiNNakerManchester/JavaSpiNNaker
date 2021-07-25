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

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

/**
 * Model of a board, for configuration purposes.
 *
 * @author Donal Fellows
 */
public class BoardRecord {
	private String machineName;

	private Integer x;

	private Integer y;

	private Integer z;

	private Integer cabinet;

	private Integer frame;

	private Integer board;

	private String ipAddress;

	private Boolean enabled;

	/**
	 * @return the machine name
	 */
	@NotNull
	public String getMachineName() {
		return machineName;
	}

	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	/**
	 * @return the board X coordinate
	 */
	public Integer getX() {
		return x;
	}

	public void setX(Integer x) {
		this.x = x;
	}

	/**
	 * @return the board Y coordinate
	 */
	public Integer getY() {
		return y;
	}

	public void setY(Integer y) {
		this.y = y;
	}

	/**
	 * @return the board Z coordinate
	 */
	public Integer getZ() {
		return z;
	}

	public void setZ(Integer z) {
		this.z = z;
	}

	/**
	 * @return whether we have a full set of triad coordinates
	 */
	public boolean isTriadCoordPresent() {
		return x != null && y != null && z != null;
	}

	/**
	 * @return the cabinet number
	 */
	public Integer getCabinet() {
		return cabinet;
	}

	public void setCabinet(Integer cabinet) {
		this.cabinet = cabinet;
	}

	/**
	 * @return the frame number
	 */
	public Integer getFrame() {
		return frame;
	}

	public void setFrame(Integer frame) {
		this.frame = frame;
	}

	/**
	 * @return the board number
	 */
	public Integer getBoard() {
		return board;
	}

	public void setBoard(Integer board) {
		this.board = board;
	}

	/**
	 * @return whether we have a full set of physical coordinates
	 */
	public boolean isPhysicalCoordPresent() {
		return cabinet != null && frame != null && board != null;
	}

	/**
	 * @return the board's IP address
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * @return whether we have an IP address
	 */
	public boolean isAddressPresent() {
		return ipAddress != null;
	}

	/**
	 * @return whether we have the name of a machine and at least one set of
	 *         coordinates for a board on that machine
	 */
	@AssertTrue
	boolean isValidBoardLocator() {
		return (machineName != null) && (isTriadCoordPresent()
				|| isPhysicalCoordPresent() || isAddressPresent());
	}

	/**
	 * @return whether the board is enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @return whether the board enabled state is defined
	 */
	public boolean isEnabledDefined() {
		return enabled != null;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
}
