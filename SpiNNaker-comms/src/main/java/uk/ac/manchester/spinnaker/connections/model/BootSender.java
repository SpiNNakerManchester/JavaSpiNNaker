package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;

import uk.ac.manchester.spinnaker.messages.boot.BootMessage;

/** A sender of SpiNNaker Boot messages. */
public interface BootSender extends SocketHolder {
	/**
	 * Sends a SpiNNaker boot message using this connection.
	 *
	 * @param bootMessage
	 *            The message to be sent
	 * @throws IOException
	 *             If there is an error sending the message
	 */
	void sendBootMessage(BootMessage bootMessage) throws IOException;
}
