package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;
import java.net.SocketTimeoutException;

import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;

/**
 * Receives an SCP response from a socket.
 *
 * @author Donal Fellows
 */
public interface SCPReceiver extends SocketHolder {
	/**
	 * Receives an SCP response from this connection. Blocks until a message has
	 * been received, or a timeout occurs.
	 *
	 * @param timeout
	 *            The time in seconds to wait for the message to arrive; if not
	 *            specified, will wait forever, or until the connection is
	 *            closed.
	 * @return The SCP result, the sequence number, and the data of the
	 *         response. The buffer pointer will be positioned at the point
	 *         where the payload starts.
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws SocketTimeoutException
	 *             If there is a timeout before a message is received
	 */
	SCPResultMessage receiveSCPResponse(Integer timeout)
			throws SocketTimeoutException, IOException;
}
