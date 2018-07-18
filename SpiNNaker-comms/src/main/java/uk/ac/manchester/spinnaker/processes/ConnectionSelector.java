package uk.ac.manchester.spinnaker.processes;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/** A connection selector for multi-connection processes */
public interface ConnectionSelector {
	/**
	 * Get the next connection for the process from a list of connections that
	 * might satisfy the request.
	 *
	 * @param request
	 *            The SCP message to be sent
	 */
	SCPConnection getNextConnection(SCPRequest<?> request);
}
