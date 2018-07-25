package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;

import uk.ac.manchester.spinnaker.messages.boot.SpinnakerBootMessage;

/** A sender of SpiNNaker Boot messages */
public interface SpinnakerBootSender extends SocketHolder {
	/**
	 * Sends a SpiNNaker boot message using this connection.
	 *
	 * @param bootMessage
	 *            The message to be sent
	 * @throws IOException
	 *             If there is an error sending the message
	 */
	void sendBootMessage(SpinnakerBootMessage bootMessage) throws IOException;
}
