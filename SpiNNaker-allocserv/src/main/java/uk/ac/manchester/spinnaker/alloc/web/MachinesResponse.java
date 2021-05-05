package uk.ac.manchester.spinnaker.alloc.web;

import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

public class MachinesResponse {
	public List<URI> machines;

	public MachinesResponse(Map<String, ?> machines, UriInfo ui) {
		UriBuilder ub = ui.getAbsolutePathBuilder().path("{name}");
		this.machines = machines.keySet().stream().map(s -> ub.build(s))
				.collect(toList());
	}
}
