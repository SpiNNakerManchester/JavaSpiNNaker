package uk.ac.manchester.spinnaker.transceiver.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.messages.scp.GetVersion;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/** A process for getting the version of the machine. */
public class GetVersionProcess extends SingleConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public GetVersionProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
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
