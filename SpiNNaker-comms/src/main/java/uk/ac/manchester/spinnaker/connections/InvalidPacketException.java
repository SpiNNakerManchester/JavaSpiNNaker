package uk.ac.manchester.spinnaker.connections;

import java.io.IOException;

/**
 * Indicates that a packet with an unsupported format was received.
 *
 * @author Donal Fellows
 */
public class InvalidPacketException extends IOException {
	private static final long serialVersionUID = -2509633246846245166L;

	public InvalidPacketException(String message) {
		super(message);
	}

	public InvalidPacketException(String message, Throwable cause) {
		super(message, cause);
	}
}
