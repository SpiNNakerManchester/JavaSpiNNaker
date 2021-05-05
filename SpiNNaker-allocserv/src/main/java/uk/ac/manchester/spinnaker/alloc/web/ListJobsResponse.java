package uk.ac.manchester.spinnaker.alloc.web;

import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.alloc.JobCollection;

public class ListJobsResponse {
	public List<URI> jobs = new ArrayList<>();

	public ListJobsResponse(JobCollection jc, UriInfo ui) {
		UriBuilder b = ui.getAbsolutePathBuilder().path("{id}");
		jobs = jc.ids().stream().map(id -> b.build(id)).collect(toList());
	}

}
