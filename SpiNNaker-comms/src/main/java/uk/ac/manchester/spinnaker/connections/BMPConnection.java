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
package uk.ac.manchester.spinnaker.connections;

import static uk.ac.manchester.spinnaker.connections.UDPConnection.TrafficClass.IPTOS_RELIABILITY;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.connections.model.SCPSenderReceiver;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
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
	/** The coordinates of the BMP. */
	private final BMPCoords coords;

	/**
	 * The IDs of the specific set of boards managed by the BMPs we can talk to.
	 * Immutable.
	 */
	public final Collection<BMPBoard> boards;

	/**
	 * @param connectionData
	 *            Description of which BMP(s) to talk to.
	 * @throws IOException
	 *             If socket creation fails.
	 */
	public BMPConnection(BMPConnectionData connectionData) throws IOException {
		super(null, null, connectionData.ipAddress,
				(connectionData.portNumber == null ? SCP_SCAMP_PORT
						: connectionData.portNumber), IPTOS_RELIABILITY);
		coords = connectionData.bmp;
		boards = connectionData.boards;
	}

	@Override
	public final void send(SCPRequest<?> scpRequest) throws IOException {
		send((BMPRequest<?>) scpRequest);
	}

	@Override
	public final void send(ByteBuffer data, int seq) {
		throw new UnsupportedOperationException("should not be called");
	}

	/**
	 * Send a request on this connection.
	 *
	 * @param scpRequest
	 *            The request to send.
	 * @throws IOException
	 *             If the request can't be sent.
	 */
	public void send(BMPRequest<?> scpRequest) throws IOException {
		send(getSCPData(scpRequest));
	}

	/**
	 * Defined to satisfy the {@link SCPSenderReceiver} interface. Always
	 * {@linkplain ChipLocation#ZERO_ZERO 0,0} for a BMP.
	 *
	 * @return {@inheritDoc}
	 */
	@Override
	public ChipLocation getChip() {
		return ZERO_ZERO;
	}

	@Override
	public SCPResultMessage receiveSCPResponse(int timeout)
			throws IOException, InterruptedException {
		return new SCPResultMessage(receive(timeout));
	}

	@Override
	public SDPMessage receiveMessage(int timeout)
			throws IOException, InterruptedException {
		return new SDPMessage(receive(timeout), true);
	}

	/**
	 * @return The coordinates of the BMP that this connection talks to.
	 */
	public BMPCoords getCoords() {
		return coords;
	}
}
