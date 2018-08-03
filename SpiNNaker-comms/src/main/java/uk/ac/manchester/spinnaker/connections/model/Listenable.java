package uk.ac.manchester.spinnaker.connections.model;

import static uk.ac.manchester.spinnaker.messages.Constants.MS_PER_S;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * How to listen for a message on a connection.
 *
 * @param <MessageType>
 *            The type of message being listened for (typically fixed for a
 *            particular connection).
 */
public interface Listenable<MessageType> {
	/** @return the method that receives for this connection. */
	MessageReceiver<MessageType> getReceiver();

	/**
	 * Do a non-blocking poll of whether there is a message ready to be received
	 * without blocking.
	 *
	 * @return true when there is a packet waiting to be received
	 * @throws IOException
	 *             If anything goes wrong, e.g., if the socket is closed under
	 *             our feet.
	 */
	default boolean isReadyToReceive() throws IOException {
		return isReadyToReceive(null);
	}

	/**
	 * Do a blocking poll of whether there is a message ready to be received
	 * without blocking.
	 *
	 * @param timeout
	 *            How many seconds to wait for a message to be receivable.
	 * @return true when there is a packet waiting to be received
	 * @throws IOException
	 *             If anything goes wrong, e.g., if the socket is closed under
	 *             our feet.
	 */
	default boolean isReadyToReceive(double timeout) throws IOException {
		return isReadyToReceive((int) (timeout * MS_PER_S));
	}

	/**
	 * Determines if there is a message available to be received without
	 * blocking.
	 *
	 * @param timeout
	 *            How long to wait, in milliseconds; if zero or null, a
	 *            non-blocking poll is performed.
	 * @return true when there is a message waiting to be received
	 * @throws IOException
	 *             If anything goes wrong, e.g., if the socket is closed under
	 *             our feet.
	 */
	boolean isReadyToReceive(Integer timeout) throws IOException;

	/**
	 * How to actually receive a message of a given type.
	 *
	 * @author Donal Fellows
	 * @param <MessageType>
	 *            The type of message received
	 */
	@FunctionalInterface
	interface MessageReceiver<MessageType> {
		/**
		 * Receive a message from its connection.
		 *
		 * @return The message received
		 * @throws SocketTimeoutException
		 *             If a timeout occurs before any data is received
		 * @throws IOException
		 *             If an error occurs receiving the data
		 */
		MessageType receive() throws SocketTimeoutException, IOException;
	}
}
