package uk.ac.manchester.spinnaker.alloc.web;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.alloc.Job;

public class CreateJobResponse {
	public int jobId;
	public URI jobRef;

	public CreateJobResponse(Job j, UriInfo ui) {
		jobId = j.getId();
		jobRef = ui.getRequestUriBuilder().path("{id}").build(j.getId());
	}
}
