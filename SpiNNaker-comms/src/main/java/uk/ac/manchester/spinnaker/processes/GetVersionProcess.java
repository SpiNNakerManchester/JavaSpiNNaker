package uk.ac.manchester.spinnaker.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.messages.scp.GetVersion;

/** A process for getting the version of the machine. */
public class GetVersionProcess extends SingleConnectionProcess<SCPConnection> {
	public GetVersionProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
	}

	public VersionInfo getVersion(HasCoreLocation core)
			throws IOException, Exception {
		return synchronousCall(new GetVersion(core)).versionInfo;
	}
}
