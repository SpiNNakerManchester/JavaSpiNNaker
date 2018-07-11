package uk.ac.manchester.spinnaker.connections.model;

/**
 * Handles a message received from a connection.
 *
 * @param <MessageType>
 *            the type of message handled.
 * @author Donal Fellows
 */
public interface MessageHandler<MessageType> {
	void handle(MessageType message);
}
