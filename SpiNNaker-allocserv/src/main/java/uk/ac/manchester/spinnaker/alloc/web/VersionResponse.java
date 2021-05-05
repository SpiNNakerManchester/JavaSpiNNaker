package uk.ac.manchester.spinnaker.alloc.web;

import uk.ac.manchester.spinnaker.messages.model.Version;

public class VersionResponse {
	public Version version;

	public VersionResponse(String version) {
		this.version = new Version(version);
	}

	public VersionResponse(Version version) {
		this.version = version;
	}
}
