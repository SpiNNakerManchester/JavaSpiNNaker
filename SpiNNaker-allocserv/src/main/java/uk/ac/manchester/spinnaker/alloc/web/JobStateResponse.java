/*
 * Copyright (c) 2021-2022 The University of Manchester
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
import static uk.ac.manchester.spinnaker.alloc.model.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB_BOARD_BY_CHIP;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB_KEEPALIVE;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB_MACHINE;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB_MACHINE_POWER;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import javax.ws.rs.core.UriInfo;

import org.springframework.dao.DataAccessException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.proxy.SpinWSHandler;

/**
 * The state of a job.
 *
 * @author Donal Fellows
 */
public class JobStateResponse {
	private JobState state;

	private String owner;

	private Instant startTime;

	private Instant finishTime;

	private String reason;

	private String keepaliveHost;

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

	/** How to connect to the proxy. Proxy is its own special protocol. */
	@JsonInclude(NON_NULL)
	public final URI proxyRef;

	/**
	 * The original request that created the job. Or at least the non-ignored
	 * parts of it.
	 */
	@JsonInclude(NON_NULL)
	public final CreateJobRequest originalRequest;

	/** Make an instance without any references to other resources. */
	public JobStateResponse() {
		keepaliveRef = null;
		machineRef = null;
		powerRef = null;
		chipRef = null;
		proxyRef = null;
		originalRequest = null;
	}

	private static CreateJobRequest origRequest(JsonMapper mapper, Job job) {
		try {
			Optional<byte[]> data = job.getOriginalRequest();
			if (!data.isPresent()) {
				return null;
			}
			return mapper.readValue(data.orElseThrow(), CreateJobRequest.class);
		} catch (IOException | DataAccessException e) {
			// Non-critical; this can be just dropped if it doesn't work
			return null;
		}
	}

	JobStateResponse(Job job, UriInfo ui, JsonMapper mapper,
			String servletPath) {
		state = job.getState();
		startTime = job.getStartTime();
		reason = job.getReason().orElse(null);
		finishTime = job.getFinishTime().orElse(null);
		keepaliveHost = job.getKeepaliveHost().orElse(null);
		keepaliveTime = job.getKeepaliveTimestamp();
		owner = job.getOwner().orElse(null);
		originalRequest = origRequest(mapper, job);

		var b = ui.getAbsolutePathBuilder().path("{resource}");
		keepaliveRef = b.build(JOB_KEEPALIVE);
		machineRef = b.build(JOB_MACHINE);
		chipRef = b.build(JOB_BOARD_BY_CHIP);
		// This one has a sub-path
		powerRef =
				b.path("{subresource}").build(JOB_MACHINE, JOB_MACHINE_POWER);

		if ((state == JobState.POWER || state == JobState.READY)
				&& (servletPath != null)) {
			proxyRef = makeProxyURI(job, ui, servletPath);
		} else {
			proxyRef = null;
		}
	}

	private static URI makeProxyURI(Job job, UriInfo ui, String servletPath) {
		// Messy; needs to refer to the other half of the application
		return ui.getBaseUriBuilder().scheme("wss").replacePath(servletPath)
				.path(SpinWSHandler.PATH).build(job.getId());
	}

	/** @return The formal state of the job */
	public JobState getState() {
		return state;
	}

	void setState(JobState state) {
		this.state = state;
	}

	/** @return When the job started. */
	public Instant getStartTime() {
		return startTime;
	}

	void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	/** @return The reason the job was destroyed. */
	@JsonInclude(NON_NULL)
	public String getReason() {
		return reason;
	}

	void setReason(String reason) {
		this.reason = reason;
	}

	/** @return What host (if any) is keeping the job alive? */
	@JsonInclude(NON_NULL)
	public String getKeepaliveHost() {
		if (state == DESTROYED) {
			return null;
		}
		return keepaliveHost;
	}

	void setKeepaliveHost(String keepaliveHost) {
		this.keepaliveHost = keepaliveHost;
	}

	/** @return Who created the job? */
	@JsonInclude(NON_NULL)
	public String getOwner() {
		return owner;
	}

	void setOwner(String owner) {
		this.owner = owner;
	}

	/** @return When the job finished. */
	@JsonInclude(NON_NULL)
	public Instant getFinishTime() {
		return finishTime;
	}

	/** @return When the job last had a keep-alive message. */
	@JsonInclude(NON_NULL)
	public Instant getKeepAliveTime() {
		if (state == DESTROYED) {
			return null;
		}
		return keepaliveTime;
	}
}
