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

import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;

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

	private List<Report> reports = new ArrayList<>();

	/** A report of an issue with a board. */
	public static class Report {
		private int id;

		private String issue;

		private String reporter;

		private Instant timestamp;

		public Report() {
		}

		/**
		 * Create a record from a row.
		 *
		 * @param row
		 *            The database row.
		 * @see SQLQueries#GET_BOARD_REPORTS
		 */
		public Report(Row row) {
			id = row.getInt("report_id");
			issue = row.getString("reported_issue");
			reporter = row.getString("reporter_name");
			timestamp = row.getInstant("report_timestamp");
		}

		/** @return The report ID. */
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		/**
		 * @return What did they report?
		 */
		public String getIssue() {
			return issue;
		}

		public void setIssue(String issue) {
			this.issue = issue;
		}

		/**
		 * @return Who reported it?
		 */
		public String getReporter() {
			return reporter;
		}

		public void setReporter(String reporter) {
			this.reporter = reporter;
		}

		/**
		 * @return When was it reported?
		 */
		public Instant getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(Instant timestamp) {
			this.timestamp = timestamp;
		}
	}

	// TODO What other attributes should be in here?
	// Allocated job?
	// Last time turned on and off?
	// Number of reports outstanding?

	/** @return The board ID. */
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return whether we have an ID
	 */

	public boolean isIdPresent() {
		return nonNull(id);
	}

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
		return nonNull(x) && nonNull(y) && nonNull(z);
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
		return nonNull(cabinet) && nonNull(frame) && nonNull(board);
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
		return nonNull(ipAddress);
	}

	/**
	 * @return whether we have either the ID of a board (from a previous lookup)
	 *         or the name of a machine and at least one set of coordinates for
	 *         a board on that machine
	 */
	@AssertTrue
	boolean isValidBoardLocator() {
		return isIdPresent() || (nonNull(machineName) && (isTriadCoordPresent()
				|| isPhysicalCoordPresent() || isAddressPresent()));
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
		return nonNull(enabled);
	}

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

	public void setJobId(Integer jobId) {
		this.jobId = jobId;
	}

	/** @return When the board was last powered on, if known. */
	public Instant getLastPowerOn() {
		return lastPowerOn;
	}

	public void setLastPowerOn(Instant lastPowerOn) {
		this.lastPowerOn = lastPowerOn;
	}

	/** @return When the board was last powered off, if known. */
	public Instant getLastPowerOff() {
		return lastPowerOff;
	}

	public void setLastPowerOff(Instant lastPowerOff) {
		this.lastPowerOff = lastPowerOff;
	}

	/** @return The reports associated with this board. */
	@NotNull
	public List<Report> getReports() {
		return reports;
	}

	public void setReports(List<Report> reports) {
		this.reports = nonNull(reports) ? reports : new ArrayList<>();
	}

	/** @return Whether this board is powered on. */
	public boolean isPowered() {
		return powered;
	}

	public void setPowered(boolean power) {
		powered = power;
	}
}
