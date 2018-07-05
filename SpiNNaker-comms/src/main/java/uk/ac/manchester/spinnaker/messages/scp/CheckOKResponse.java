package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_OK;

import java.nio.ByteBuffer;

/** An SCP response to a request which returns nothing other than OK */
public class CheckOKResponse extends SCPResponse {
	public CheckOKResponse(String operation, SCPCommand command,
			ByteBuffer buffer) throws UnexpectedResponseCodeException {
		super(buffer);
		SCPResult result = scpResponseHeader.result;
		if (result != RC_OK) {
			throw new UnexpectedResponseCodeException(operation, command,
					result);
		}
	}
}
