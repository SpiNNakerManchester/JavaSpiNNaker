package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.String.format;

/**
 * Indicate that a response code returned from the board was unexpected for the
 * current operation.
 */
@SuppressWarnings("serial")
public class UnexpectedResponseCodeException extends Exception {
	/**
	 * @param operation
	 *            The operation being performed
	 * @param command
	 *            The command being executed
	 * @param response
	 *            The response received in error
	 */
	public UnexpectedResponseCodeException(String operation, String command,
			String response) {
		super(format("Unexpected response %s while performing operation %s "
				+ "using command %s", response, operation, command));
	}
}
