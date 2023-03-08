/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Integer.toHexString;
import static java.lang.Short.toUnsignedInt;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_LEN;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_P2P_NOREPLY;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_P2P_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_TIMEOUT;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** The low-level format of SCP result message. */
public final class SCPResultMessage {
	private static final int SDP_HEADER_LENGTH = 8;

	private static final int SKIP_HEADER_BYTES = 2 + SDP_HEADER_LENGTH;

	private static final Set<SCPResult> RETRY_CODES =
			EnumSet.of(RC_TIMEOUT, RC_P2P_TIMEOUT, RC_LEN, RC_P2P_NOREPLY);

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
		var peek = response.duplicate().order(LITTLE_ENDIAN);
		// Skip the padding bytes and the SDP header
		peek.position(SKIP_HEADER_BYTES);
		result = SCPResult.get(peek.getShort());
		sequenceNumber = toUnsignedInt(peek.getShort());
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
	 * @return The correlated request, or {@code null} if no correlation
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
	 * @throws IllegalStateException
	 *             If the response has already been parsed.
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

	/** @return The result code from the header. */
	public SCPResult getResult() {
		return result;
	}

	/** @return The sequence number from the header. */
	public int getSequenceNumber() {
		return sequenceNumber;
	}

	@Override
	public String toString() {
		Object contents;
		if (responseData != null) {
			var data = new ArrayList<String>();
			for (int i = 0; i < responseData.limit(); i++) {
				data.add(toHexString(toUnsignedInt(responseData.get(i))));
			}
			contents = data;
		} else {
			contents = "message data purged";
		}
		return "SCPResultMessage(" + contents + ")";
	}
}
