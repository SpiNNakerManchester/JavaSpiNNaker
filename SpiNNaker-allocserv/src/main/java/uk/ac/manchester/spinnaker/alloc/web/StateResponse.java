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

public class StateResponse {
	public State state;
	public Float startTime;
	public String reason;
	public String keepaliveHost;

	public URI keepaliveRef;
	public URI machineRef;
	public URI powerRef;
	public URI chipRef;

	public StateResponse(Job j, UriInfo ui) {
		state = j.getState();
		startTime = j.getStartTime();
		reason = j.getReason();
		keepaliveHost = j.getKeepaliveHost();

		UriBuilder b = ui.getAbsolutePathBuilder().path("{resource}");
		keepaliveRef = b.build("keepalive");
		machineRef = b.build("machine");
		powerRef = b.build("machine/power");
		chipRef = b.build("chip");
	}

}
