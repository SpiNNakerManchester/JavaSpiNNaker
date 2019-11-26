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
package uk.ac.manchester.spinnaker.connections;

import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;

import java.io.IOException;
import java.util.Collection;

import uk.ac.manchester.spinnaker.connections.model.SCPSenderReceiver;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest;
import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;


/**
 * A connection for talking to one or more Board Management Processors (BMPs). A
 * particular connection can only talk to the BMPs of a single frame of a single
 * cabinet.
 */
public class BMPConnection extends UDPConnection<SDPMessage>
		implements SCPSenderReceiver {
	/** Defined to satisfy the SCPSender; always 0,0 for a BMP. */
	private static final ChipLocation BMP_LOCATION = new ChipLocation(0, 0);
	/** The ID of the cabinet that contains the frame that contains the BMPs. */
	public final int cabinet;
	/** The ID of the frame that contains the BMPs. */
	public final int frame;
	/**
	 * The IDs of the specific set of boards managed by the BMPs we can talk to.
	 */
	public final Collection<Integer> boards;

	/**
	 * @param connectionData
	 *            Description of which BMP(s) to talk to.
	 * @throws IOException
	 *             If socket creation fails.
	 */
	public BMPConnection(BMPConnectionData connectionData) throws IOException {
		super(null, null, connectionData.ipAddress,
				(connectionData.portNumber == null ? SCP_SCAMP_PORT
						: connectionData.portNumber));
		cabinet = connectionData.cabinet;
		frame = connectionData.frame;
		boards = connectionData.boards;
	}

	@Override
	public final void sendSCPRequest(SCPRequest<?> scpRequest)
			throws IOException {
		sendBMPRequest((BMPRequest<?>) scpRequest);
	}

	/**
	 * Send a request on this connection.
	 *
	 * @param scpRequest
	 *            The request to send.
	 * @throws IOException
	 *             If the request can't be sent.
	 */
	public void sendBMPRequest(BMPRequest<?> scpRequest) throws IOException {
		send(getSCPData(scpRequest));
	}

	@Override
	public ChipLocation getChip() {
		return BMP_LOCATION;
	}

	@Override
	public SCPResultMessage receiveSCPResponse(Integer timeout)
			throws IOException {
		return new SCPResultMessage(receive(timeout));
	}

	@Override
	public SDPMessage receiveMessage(Integer timeout) throws IOException {
		return new SDPMessage(receive(timeout));
	}
}
