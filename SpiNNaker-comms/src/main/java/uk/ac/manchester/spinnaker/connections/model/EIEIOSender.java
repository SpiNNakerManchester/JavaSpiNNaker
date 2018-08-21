package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;

import uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessage;

/** A sender of EIEIO messages. */
public interface EIEIOSender extends Connection {
	/**
	 * Sends an EIEIO message down this connection.
	 *
	 * @param eieioMessage
	 *            The EIEIO message to be sent
	 * @throws IOException
	 *             If there is an error sending the message
	 */
	void sendEIEIOMessage(EIEIOMessage<?> eieioMessage) throws IOException;
}
