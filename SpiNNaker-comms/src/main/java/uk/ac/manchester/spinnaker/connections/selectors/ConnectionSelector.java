package uk.ac.manchester.spinnaker.connections.selectors;

import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/**
 * A connection selector for (especially multi-connection) processes.
 *
 * @param <T>
 *            The type of connections handled by this selector.
 */
public interface ConnectionSelector<T extends Connection> {
	/**
	 * Get the next connection for the process from a list of connections that
	 * might satisfy the request.
	 *
	 * @param request
	 *            The SCP message to be sent
	 * @return The connection on which the message should be sent.
	 */
	T getNextConnection(SCPRequest<?> request);
}
