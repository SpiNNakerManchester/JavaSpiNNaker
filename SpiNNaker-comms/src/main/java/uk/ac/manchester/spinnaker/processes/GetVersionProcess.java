package uk.ac.manchester.spinnaker.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.messages.scp.GetVersion;

/** A process for getting the version of the machine. */
public class GetVersionProcess extends SingleConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 */
	public GetVersionProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
	}

	/**
	 * Get the version of the software on a particular core. Should usually be a
	 * SCAMP core.
	 *
	 * @param core
	 *            The core to query.
	 * @return The version description.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects the message.
	 */
	public VersionInfo getVersion(HasCoreLocation core)
			throws IOException, Exception {
		return synchronousCall(new GetVersion(core)).versionInfo;
	}
}
