package uk.ac.manchester.spinnaker.transceiver;

/**
 * Basic exception from the transceiver.
 */
public class SpinnmanException extends Exception {
	private static final long serialVersionUID = 4307580491294281556L;

	public SpinnmanException(String message) {
		super(message);
	}

	public SpinnmanException(String message, Throwable cause) {
		super(message, cause);
	}
}
