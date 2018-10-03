package uk.ac.manchester.spinnaker.messages.scp;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_LEN;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_P2P_NOREPLY;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_P2P_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_TIMEOUT;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** The low-level format of SCP result message. */
public class SCPResultMessage {
	private static final int SDP_HEADER_LENGTH = 8;
	private static final int SKIP_HEADER_BYTES = 2 + SDP_HEADER_LENGTH;
	private static final Set<SCPResult> RETRY_CODES = new HashSet<>();
	static {
		RETRY_CODES.add(RC_TIMEOUT);
		RETRY_CODES.add(RC_P2P_TIMEOUT);
		RETRY_CODES.add(RC_LEN);
		RETRY_CODES.add(RC_P2P_NOREPLY);
	}

	/** The response code. */
	private final SCPResult result;
	/** The sequence number of the request that this is a response to. */
	private final int sequenceNumber;
	/** The remaining data of the response. */
	private ByteBuffer responseData;

	/**
	 * @param response
	 *            The payload of the UDP message, which must be an SDP message
	 *            without header stripped.
	 */
	public SCPResultMessage(ByteBuffer response) {
		ByteBuffer peek = response.duplicate().order(LITTLE_ENDIAN);
		// Skip the padding bytes and the SDP header
		peek.position(SKIP_HEADER_BYTES);
		result = SCPResult.get(peek.getShort());
		sequenceNumber = peek.getShort();
		responseData = response;
	}

	/**
	 * @return Whether this response indicates that the request should be
	 *         retried.
	 */
	public boolean isRetriable() {
		return RETRY_CODES.contains(result);
	}

	/**
	 * Given a collection of requests, pick the one that correlates to this
	 * result.
	 *
	 * @param <T>
	 *            The type of requests.
	 * @param requestStore
	 *            The store of requests.
	 * @return The correlated request, or <tt>null</tt> if no correlation
	 *         exists.
	 */
	public <T> T pickRequest(Map<Integer, T> requestStore) {
		return requestStore.get(getSequenceNumber());
	}

	/**
	 * Given a collection of requests, remove the one that correlates to this
	 * result.
	 *
	 * @param requestStore
	 *            The store of requests.
	 */
	public void removeRequest(Map<Integer, ?> requestStore) {
		requestStore.remove(getSequenceNumber());
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
	public <T extends SCPResponse> T parsePayload(SCPRequest<T> request)
			throws Exception {
		if (responseData == null) {
			throw new IllegalStateException("can only parse a payload once");
		}
		try {
			return request.getSCPResponse(responseData);
		} finally {
			responseData = null;
		}
	}

	public SCPResult getResult() {
		return result;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}
}
