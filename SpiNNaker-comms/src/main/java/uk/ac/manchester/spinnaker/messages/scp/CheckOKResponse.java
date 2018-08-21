package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_OK;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** An SCP response to a request which returns nothing other than OK. */
public class CheckOKResponse extends SCPResponse {
	/**
	 * Create an instance.
	 *
	 * @param operation
	 *            The overall operation that we are doing.
	 * @param command
	 *            The command that we are handling a response to.
	 * @param buffer
	 *            The buffer holding the response data.
	 * @throws UnexpectedResponseCodeException
	 *             If the response wasn't OK.
	 */
	public CheckOKResponse(String operation, SCPCommand command,
			ByteBuffer buffer) throws UnexpectedResponseCodeException {
		super(buffer);
		if (result != RC_OK) {
			throw new UnexpectedResponseCodeException(operation, command,
					result);
		}
	}
}
