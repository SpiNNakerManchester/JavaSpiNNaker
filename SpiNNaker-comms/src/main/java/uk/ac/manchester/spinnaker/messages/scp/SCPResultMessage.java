package uk.ac.manchester.spinnaker.messages.scp;

import java.nio.ByteBuffer;

public class SCPResultMessage {
	public final SCPResult result;
	public final int sequenceNumber;
	public final ByteBuffer responseData;

	public SCPResultMessage(SCPResult result, int sequenceNumber,
			ByteBuffer responseData) {
		this.result = result;
		this.sequenceNumber = sequenceNumber;
		this.responseData = responseData;
	}

	public <T extends SCPResponse> T getResponse(SCPRequest<T> request)
			throws Exception {
		return request.getSCPResponse(responseData);
	}
}
