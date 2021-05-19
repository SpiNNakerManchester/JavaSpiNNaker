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
package uk.ac.manchester.spinnaker.alloc.allocator;

import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.runUpdate;
import static uk.ac.manchester.spinnaker.alloc.allocator.JobState.DESTROYED;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.JobsEpoch;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

public class Job {
	@JsonIgnore
	private final Connection conn;

	@JsonIgnore
	private JobsEpoch epoch;

	/** Job ID */
	int id;

	/** If not {@code null}, the allocated width of the job's rectangle. */
	Integer width;

	/** If not {@code null}, the allocated height of the job's rectangle. */
	Integer height;

	JobState state;

	/** If not {@code null}, the ID of the root board of the job. */
	Integer root;

	/** The creator of the job. */
	String owner;

	/** Timestamp of last event keeping job alive. (ms from epoch) */
	long keepaliveTime;

	/** Host address that issued last keepalive event, if any. */
	String keepaliveHost;

	Job(Connection conn, JobsEpoch epoch) {
		this.conn = conn; // TODO do not retain this!
		this.epoch = epoch;
	}

	private static final String UPDATE_KEEPALIVE =
			"UPDATE jobs SET keepalive_timestamp = ?, keepalive_host = ? "
					+ "WHERE job_id = ? AND job_state != ?";

	private static final String DESTROY_JOB = "UPDATE jobs SET "
			+ "job_state = ?, death_reason = ?, death_timestamp = ? "
			+ "WHERE job_id = ? AND job_state != ?";

	public void access(String keepaliveAddress) throws SQLException {
		Date now = new Date();
		try (PreparedStatement s = conn.prepareStatement(UPDATE_KEEPALIVE)) {
			runUpdate(s, now, keepaliveAddress, id, DESTROYED);
		}
	}

	public SubMachine getMachine() {
		// TODO Auto-generated method stub
		return null;
	}

	public void destroy(String reason) throws SQLException {
		Date now = new Date();
		try (PreparedStatement s = conn.prepareStatement(DESTROY_JOB)) {
			runUpdate(s, DESTROYED, reason, now, id, DESTROYED);
		}
	}

	public void waitForChange(long timeout) {
		try {
			epoch.waitForChange(timeout);
		} catch (InterruptedException ignored) {
		}
	}

	public int getId() {
		// TODO Auto-generated method stub
		return id;
	}

	public JobState getState() {
		// TODO Auto-generated method stub
		return state;
	}

	public Float getStartTime() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getReason() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getKeepaliveHost() {
		// TODO Auto-generated method stub
		return null;
	}

	public BoardLocation whereIs(int x, int y) {
		// TODO Auto-generated method stub
		return null;
	}

	public ChipLocation getRootChip() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getOwner() {
		// TODO Auto-generated method stub
		return owner;
	}

}
