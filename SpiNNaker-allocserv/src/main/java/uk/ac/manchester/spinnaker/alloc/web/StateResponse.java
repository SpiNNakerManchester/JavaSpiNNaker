package uk.ac.manchester.spinnaker.alloc.web;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.alloc.Job;
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
