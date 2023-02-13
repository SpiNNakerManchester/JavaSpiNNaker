/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;

/**
 * Sends an SCP request and receives a response to it.
 *
 * @author Donal Fellows
 */
public interface SCPSenderReceiver extends Connection {
	/**
	 * Returns the data of an SCP request as it would be sent down this
	 * connection.
	 *
	 * @param scpRequest
	 *            message packet to serialise
	 * @return The buffer holding the data. The data should be written into the
	 *         start of the buffer and should end at the <i>position</i>.
	 */
	default ByteBuffer getSCPData(SCPRequest<?> scpRequest) {
		return scpRequest.getMessageData(getChip());
	}

	/**
	 * Sends an SCP request down this connection.
	 * <p>
	 * Messages must have the following properties:
	 * <ul>
	 * <li><i>sourcePort</i> is {@code null} or 7
	 * <li><i>sourceCpu</i> is {@code null} or 31
	 * <li><i>sourceChipX</i> is {@code null} or 0
	 * <li><i>sourceChipY</i> is {@code null} or 0
	 * </ul>
	 * <i>tag</i> in the message is optional; if not set, the default set in the
	 * constructor will be used.
	 * <p>
	 * <i>sequence</i> in the message is optional; if not set, <i>(sequence
	 * number last assigned + 1) % 65536</i> will be used
	 *
	 * @param request
	 *            message packet to send
	 * @throws IOException
	 *             If there is an error sending the message
	 */
	default void send(SCPRequest<?> request) throws IOException {
		var msg = getSCPData(request);
		switch (request.sdpHeader.getFlags()) {
		case REPLY_EXPECTED:
		case REPLY_EXPECTED_NO_P2P:
			send(msg, request.scpRequestHeader.getSequence());
			break;
		default:
			send(msg);
		}
	}

	/**
	 * @return The chip at which messages sent down this connection will arrive
	 *         at first.
	 */
	ChipLocation getChip();

	/**
	 * Receives an SCP response from this connection. Blocks until a message has
	 * been received, or a timeout occurs.
	 *
	 * @param timeout
	 *            The time in milliseconds to wait for the message to arrive; if
	 *            {@code null}, will wait forever, or until the connection is
	 *            closed.
	 * @return The SCP result, the sequence number, and the data of the
	 *         response. The buffer pointer will be positioned at the point
	 *         where the payload starts.
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws SocketTimeoutException
	 *             If there is a timeout before a message is received
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	default SCPResultMessage receiveSCPResponse(Integer timeout)
			throws SocketTimeoutException, IOException, InterruptedException {
		return receiveSCPResponse(convertTimeout(timeout));
	}

	/**
	 * Receives an SCP response from this connection. Blocks until a message has
	 * been received, or a timeout occurs.
	 *
	 * @param timeout
	 *            The time in milliseconds to wait for the message to arrive, or
	 *            until the connection is closed.
	 * @return The SCP result, the sequence number, and the data of the
	 *         response. The buffer pointer will be positioned at the point
	 *         where the payload starts.
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws SocketTimeoutException
	 *             If there is a timeout before a message is received
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	SCPResultMessage receiveSCPResponse(int timeout)
			throws SocketTimeoutException, IOException, InterruptedException;

	/**
	 * Send a request that expects a response to be appropriately directed to
	 * the thread that is calling this method.
	 *
	 * @param requestData
	 *            The message data to send.
	 * @param seq
	 *            The sequence number to come in the response.
	 * @throws IOException
	 *             If there is an error sending the message
	 * @see #send(ByteBuffer)
	 */
	void send(ByteBuffer requestData, int seq) throws IOException;
}
