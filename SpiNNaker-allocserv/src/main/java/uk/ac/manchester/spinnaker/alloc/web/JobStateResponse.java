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
package uk.ac.manchester.spinnaker.alloc.web;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.allocator.JobState;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;

/**
 * The state of a job.
 *
 * @author Donal Fellows
 */
public class JobStateResponse {
	private JobState state;

	private String owner;

	private Instant startTime;

	@JsonInclude(NON_NULL)
	private Instant finishTime;

	@JsonInclude(NON_NULL)
	private String reason;

	private String keepaliveHost;

	@JsonInclude(NON_NULL)
	private Instant keepaliveTime;

	/** Where the keepalives go. */
	@JsonInclude(NON_NULL)
	public final URI keepaliveRef;

	/** Where the machine details are. */
	@JsonInclude(NON_NULL)
	public final URI machineRef;

	/** Where the power controls are. */
	@JsonInclude(NON_NULL)
	public final URI powerRef;

	/** Where the chip info is. */
	@JsonInclude(NON_NULL)
	public final URI chipRef;

	@JsonInclude(NON_NULL)
	public final CreateJobRequest originalRequest;

	public JobStateResponse() {
		keepaliveRef = null;
		machineRef = null;
		powerRef = null;
		chipRef = null;
		originalRequest = null;
	}

	private static CreateJobRequest origRequest(JsonMapper mapper, Job job) {
		try {
			byte[] data = job.getOriginalRequest();
			if (data == null) {
				return null;
			}
			return mapper.readValue(data, CreateJobRequest.class);
		} catch (IOException | SQLException e) {
			// Non-critical; this can be just dropped if it doesn't work
			return null;
		}
	}

	JobStateResponse(Job job, UriInfo ui, JsonMapper mapper)
			throws SQLException {
		state = job.getState();
		startTime = job.getStartTime();
		reason = job.getReason().orElse(null);
		finishTime = job.getFinishTime().orElse(null);
		keepaliveHost = job.getKeepaliveHost();
		keepaliveTime = job.getKeepaliveTimestamp();
		owner = job.getOwner();
		originalRequest = origRequest(mapper, job);

		UriBuilder b = ui.getAbsolutePathBuilder().path("{resource}");
		keepaliveRef = b.build("keepalive");
		machineRef = b.build("machine");
		powerRef = b.build("machine/power");
		chipRef = b.build("chip");
	}

	/** @return The formal state of the job */
	public JobState getState() {
		return state;
	}

	public void setState(JobState state) {
		this.state = state;
	}

	/** @return When the job started */
	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	/** @return The reason the job was destroyed */
	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	/** @return What host is keeping the job alive? */
	public String getKeepaliveHost() {
		return keepaliveHost;
	}

	public void setKeepaliveHost(String keepaliveHost) {
		this.keepaliveHost = keepaliveHost;
	}

	/** @return Who claimed to create the job? */
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
}
