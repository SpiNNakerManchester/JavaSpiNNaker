package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;

import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

/** A sender of SDP messages. */
public interface SDPSender extends Connection {
	/**
	 * Sends an SDP message down this connection.
	 *
	 * @param sdpMessage
	 *            The SDP message to be sent
	 * @throws IOException
	 *             If there is an error sending the message.
	 */
	void sendSDPMessage(SDPMessage sdpMessage) throws IOException;
}
