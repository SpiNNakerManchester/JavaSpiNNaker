package uk.ac.manchester.spinnaker.messages.scp;

import java.nio.ByteBuffer;

/** The low-level format of SCP result message. */
public class SCPResultMessage {
	/** The response code. */
	public final SCPResult result;
	/** The sequence number of the request that this is a response to. */
	public final int sequenceNumber;
	/** The remaining data of the response. */
	public final ByteBuffer responseData;

	/**
	 * @param response
	 *            The payload of the UDP message, which must be an SDP message
	 *            without header stripped.
	 */
	public SCPResultMessage(ByteBuffer response) {
		result = SCPResult.get(response.getShort());
		sequenceNumber = response.getShort();
		responseData = response;
	}

	/**
	 * Parse the payload data of the data into something higher level. Note that
	 * it is assumed that the caller has already done the sequence number
	 * matching (or has good reasons to not do so).
	 *
	 * @param <T>
	 *            The type of response associated with the request.
	 * @param request
	 *            The request that this class was a response to.
	 * @return The response, assuming it was successful.
	 * @throws Exception
	 *             If anything goes wrong with result code checking or
	 *             deserialization.
	 */
	public <T extends SCPResponse> T getResponse(SCPRequest<T> request)
			throws Exception {
		return request.getSCPResponse(responseData);
	}
}
