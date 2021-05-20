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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.JobsEpoch;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

public final class Job {
	@JsonIgnore
	private JobsEpoch epoch;

	@JsonIgnore
	private final Spalloc coreService;

	/** Job ID */
	int id;

	/** If not {@code null}, the allocated width of the job's rectangle. */
	private Integer width;

	/** If not {@code null}, the allocated height of the job's rectangle. */
	private Integer height;

	/** The state of the job. */
	private JobState state;

	/** If not {@code null}, the ID of the root board of the job. */
	private Integer root;

	/** The creator of the job. */
	private String owner;

	/** Host address that issued last keepalive event, if any. */
	private String keepaliveHost;

	Job(Spalloc coreService, JobsEpoch epoch, int id) {
		this.coreService = coreService;
		this.epoch = epoch;
		this.id = id;
	}

	Job(Spalloc coreService, JobsEpoch epoch, ResultSet row)
			throws SQLException {
		this(coreService, epoch, row.getInt("machine_id"));
		width = (Integer) row.getObject("width");
		height = (Integer) row.getObject("height");
		root = (Integer) row.getObject("root_id");
		state = JobState.values()[row.getInt("job_state")];
		keepaliveHost = row.getString("keepalive_host");
		// TODO fill this out
	}

	public void access(String keepaliveAddress) throws SQLException {
		coreService.jobAccess(this, new Date(), keepaliveAddress);
	}

	public void destroy(String reason) throws SQLException {
		coreService.jobDestroy(this, new Date(), reason);
	}

	public void waitForChange(long timeout) {
		try {
			epoch.waitForChange(timeout);
		} catch (InterruptedException ignored) {
		}
	}

	public int getId() {
		return id;
	}

	public JobState getState() {
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
		return keepaliveHost;
	}

	public SubMachine getMachine() {
		if (root == null) {
			return null;
		}
		// TODO Auto-generated method stub
		return null;
	}

	public BoardLocation whereIs(int x, int y) {
		if (root == null) {
			return null;
		}
		// TODO Auto-generated method stub
		return null;
	}

	public ChipLocation getRootChip() {
		if (root == null) {
			return null;
		}
		// TODO Auto-generated method stub
		return null;
	}

	public String getOwner() {
		return owner;
	}

	public Integer getWidth() {
		return width;
	}

	public Integer getHeight() {
		return height;
	}
}
