/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
public interface SCPSenderReceiver extends Connection, SocketHolder {
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
	 * number\ last assigned + 1) % 65536</i> will be used
	 *
	 * @param scpRequest
	 *            message packet to send
	 * @throws IOException
	 *             If there is an error sending the message
	 */
	void send(SCPRequest<?> scpRequest) throws IOException;

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
}
