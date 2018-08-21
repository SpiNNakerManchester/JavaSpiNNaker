package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;

/**
 * Indicates that a packet with an unsupported format was received.
 *
 * @author Donal Fellows
 */
public class InvalidPacketException extends IOException {
	private static final long serialVersionUID = -2509633246846245166L;

	/**
	 * Create an instance.
	 *
	 * @param message
	 *            The exception message.
	 */
	public InvalidPacketException(String message) {
		super(message);
	}

	/**
	 * Create an instance.
	 *
	 * @param message
	 *            The exception message.
	 * @param cause
	 *            The cause of the exception.
	 */
	public InvalidPacketException(String message, Throwable cause) {
		super(message, cause);
	}
}
