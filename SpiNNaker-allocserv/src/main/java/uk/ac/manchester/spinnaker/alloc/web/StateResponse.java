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

import java.net.URI;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.alloc.allocator.Job;
import uk.ac.manchester.spinnaker.spalloc.messages.State;

/**
 * The state of a job.
 *
 * @author Donal Fellows
 */
public class StateResponse {
	private State state;

	private String owner;

	private Float startTime; // TODO Why is this a float? It's a time, damnit!

	private String reason;

	private String keepaliveHost;

	public final URI keepaliveRef;

	public final URI machineRef;

	public final URI powerRef;

	public final URI chipRef;

	public StateResponse() {
		keepaliveRef = null;
		machineRef = null;
		powerRef = null;
		chipRef = null;
	}

	StateResponse(Job j, UriInfo ui) {
		state = j.getState();
		startTime = j.getStartTime();
		reason = j.getReason();
		keepaliveHost = j.getKeepaliveHost();
		owner = j.getOwner();

		UriBuilder b = ui.getAbsolutePathBuilder().path("{resource}");
		keepaliveRef = b.build("keepalive");
		machineRef = b.build("machine");
		powerRef = b.build("machine/power");
		chipRef = b.build("chip");
	}

	/** @return The formal state of the job */
	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	/** @return When the job started */
	public Float getStartTime() {
		return startTime;
	}

	public void setStartTime(Float startTime) {
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
