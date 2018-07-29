package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;
import java.io.InterruptedIOException;

import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

/** A receiver of SDP messages. */
public interface SDPReceiver extends Connection {
	/**
	 * Receives an SDP message from this connection. Blocks until the message
	 * has been received.
	 *
	 * @return The received SDP message
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws InvalidPacketException
	 *             If the received packet is not a valid SDP message
	 * @throws IllegalArgumentException
	 *             If one of the fields of the SDP message is invalid
	 */
	default SDPMessage receiveSDPMessage()
			throws IOException, InvalidPacketException {
		return receiveSDPMessage(null);
	}

	/**
	 * Receives an SDP message from this connection. Blocks until the message
	 * has been received, or a timeout occurs.
	 *
	 * @param timeout
	 *            The time in seconds to wait for the message to arrive; if not
	 *            specified, will wait forever, or until the connection is
	 *            closed.
	 * @return The received SDP message
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws InterruptedIOException
	 *             If there is a timeout before a message is received
	 * @throws InvalidPacketException
	 *             If the received packet is not a valid SDP message
	 * @throws IllegalArgumentException
	 *             If one of the fields of the SDP message is invalid
	 */
	SDPMessage receiveSDPMessage(Integer timeout)
			throws IOException, InvalidPacketException, InterruptedIOException;

}
