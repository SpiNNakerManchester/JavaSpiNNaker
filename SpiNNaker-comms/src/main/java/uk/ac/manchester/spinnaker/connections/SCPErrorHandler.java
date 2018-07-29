package uk.ac.manchester.spinnaker.connections;

import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/**
 * A callback handler for reporting errors in receiving an SCP (or BMP) message.
 *
 * @author Donal Fellows
 */
@FunctionalInterface
public interface SCPErrorHandler {
	/**
	 * A callback function to call when an error is found when processing an SCP
	 * message.
	 *
	 * @param request
	 *            the original SCPRequest
	 * @param exception
	 *            the exception caught while sending the request.
	 */
	void handleError(SCPRequest<?> request, Throwable exception);
}
