package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;

import uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessage;

/**
 * A receiver of EIEIO data or commands.
 */
public interface EIEIOReceiver extends Connection {
	/**
	 * Receives an EIEIO message from this connection. Blocks until a message
	 * has been received or the connection is closed.
	 *
	 * @return an EIEIO message
	 * @throws IOException
	 *             If there is an error receiving the message.
	 */
	default EIEIOMessage<?> receiveEIEIOMessage() throws IOException {
		return receiveEIEIOMessage(null);
	}

	/**
	 * Receives an EIEIO message from this connection. Blocks until a message
	 * has been received, a timeout occurs, or the connection is closed.
	 *
	 * @param timeout
	 *            The time in seconds to wait for the message to arrive; if
	 *            <tt>null</tt>, will wait forever, or until the connection is
	 *            closed
	 * @return an EIEIO message
	 * @throws IOException
	 *             If there is an error receiving the message.
	 */
	EIEIOMessage<?> receiveEIEIOMessage(Integer timeout) throws IOException;
}
