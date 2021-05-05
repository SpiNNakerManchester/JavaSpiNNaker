package uk.ac.manchester.spinnaker.alloc.web;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.messages.model.Version;

public class ServiceDescription {
	public Version version;
	public URI jobsRef;
	public URI machinesRef;

	public ServiceDescription() {
	}

	public ServiceDescription(Version version, UriInfo ui) {
		this.version = version;
		UriBuilder ub = ui.getAbsolutePathBuilder().path("{resource}");
		jobsRef = ub.build("jobs");
		machinesRef = ub.build("machines");
	}
}
