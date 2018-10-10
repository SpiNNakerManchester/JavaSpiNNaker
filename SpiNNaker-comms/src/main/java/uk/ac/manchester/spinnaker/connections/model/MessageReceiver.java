package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;

/**
 * A receiver of SpiNNaker messages.
 *
 * @param <MessageType>
 *            The type of message to be received. It's possible for the received
 *            information to even be metadata about the message, and not the
 *            content of the message.
 * @author Donal Fellows
 */
public interface MessageReceiver<MessageType> extends SocketHolder {
	/**
	 * Receives a SpiNNaker message from this connection. Blocks until a message
	 * has been received.
	 *
	 * @return the received message
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws IllegalArgumentException
	 *             If one of the fields of the SpiNNaker message is invalid
	 */
	default MessageType receiveMessage() throws IOException {
		return receiveMessage(null);
	}

	/**
	 * Receives a SpiNNaker message from this connection. Blocks until a message
	 * has been received, or a timeout occurs.
	 *
	 * @param timeout
	 *            The time in seconds to wait for the message to arrive; if
	 *            {@code null}, will wait forever, or until the connection is
	 *            closed.
	 * @return the received message
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws IllegalArgumentException
	 *             If one of the fields of the SpiNNaker message is invalid
	 */
	MessageType receiveMessage(Integer timeout) throws IOException;
}
