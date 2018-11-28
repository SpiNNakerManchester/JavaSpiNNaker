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
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/** A sender of SCP messages. */
public interface SCPSender extends Connection {
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
	void sendSCPRequest(SCPRequest<?> scpRequest) throws IOException;

	/**
	 * @return The chip at which messages sent down this connection will arrive
	 *         at first.
	 */
	ChipLocation getChip();
}
