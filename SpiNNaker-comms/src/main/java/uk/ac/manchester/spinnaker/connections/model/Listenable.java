package uk.ac.manchester.spinnaker.connections.model;

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
	 */
	default boolean isReadyToReceive(double timeout) throws IOException {
		return isReadyToReceive((int) (timeout * 1000));
	}

	/**
	 * Determines if there is a message available to be received without
	 * blocking.
	 *
	 * @param timeout
	 *            How long to wait, in milliseconds; if zero or null, a
	 *            non-blocking poll is performed.
	 * @return true when there is a message waiting to be received
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
