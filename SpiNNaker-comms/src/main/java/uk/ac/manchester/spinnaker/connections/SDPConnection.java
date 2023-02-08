/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.connections;

import static uk.ac.manchester.spinnaker.connections.UDPConnection.TrafficClass.IPTOS_THROUGHPUT;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

/** A UDP socket connection that talks SDP to SpiNNaker. */
public class SDPConnection extends UDPConnection<SDPMessage> {
	private ChipLocation chip;

	/**
	 * @param remoteChip
	 *            Which chip are we talking to? This is not necessarily the chip
	 *            that is connected to the Ethernet connector on the SpiNNaker
	 *            board, or even on the same board.
	 * @param remoteHost
	 *            The address of the SpiNNaker board to route UDP packets to.
	 * @param remotePort
	 *            The UDP port on the SpiNNaker board to use.
	 * @throws IOException
	 *             If anything goes wrong with the setup.
	 */
	public SDPConnection(HasChipLocation remoteChip, InetAddress remoteHost,
			Integer remotePort) throws IOException {
		this(remoteChip, null, null, remoteHost, remotePort);
	}

	/**
	 * @param remoteChip
	 *            Which chip are we talking to? This is not necessarily the chip
	 *            that is connected to the Ethernet connector on the SpiNNaker
	 *            board, or even on the same board.
	 * @param localHost
	 *            The local host address to bind to, or {@code null} to bind to
	 *            all relevant local addresses.
	 * @param localPort
	 *            The local port to bind to, or {@code null} to pick a random
	 *            free port.
	 * @param remoteHost
	 *            The address of the SpiNNaker board to route UDP packets to.
	 * @param remotePort
	 *            The UDP port on the SpiNNaker board to use.
	 * @throws IOException
	 *             If anything goes wrong with the setup (e.g., if the local
	 *             port is specified and already bound).
	 */
	public SDPConnection(HasChipLocation remoteChip, InetAddress localHost,
			Integer localPort, InetAddress remoteHost, Integer remotePort)
			throws IOException {
		super(localHost, localPort, remoteHost, remotePort, IPTOS_THROUGHPUT);
		this.chip = remoteChip.asChipLocation();
	}

	/**
	 * Constructor for delegation.
	 *
	 * @param remoteChip
	 *            Which chip are we talking to? This is not necessarily the chip
	 *            that is connected to the Ethernet connector on the SpiNNaker
	 *            board, or even on the same board.
	 * @param canSend
	 *            Whether sending on this connection is possible.
	 */
	SDPConnection(HasChipLocation remoteChip, boolean canSend) {
		super(canSend);
		this.chip = remoteChip.asChipLocation();
	}

	/**
	 * Sends an SDP message down this connection.
	 *
	 * @param sdpMessage
	 *            The SDP message to be sent
	 * @throws IOException
	 *             If there is an error sending the message.
	 */
	public void send(SDPMessage sdpMessage) throws IOException {
		send(sdpMessage.getMessageData(chip));
	}

	@Override
	public SDPMessage receiveMessage(int timeout)
			throws IOException, InterruptedIOException, InterruptedException {
		var buffer = receive(timeout);
		buffer.getShort(); // SKIP TWO PADDING BYTES
		return new SDPMessage(buffer, false);
	}

	/**
	 * @return The SpiNNaker chip that we are talking to with this connection.
	 */
	public ChipLocation getChip() {
		return chip;
	}

	/**
	 * Set the SpiNNaker chip that we are talking to with this connection.
	 *
	 * @param chip
	 *            The chip to talk to.
	 */
	public void setChip(HasChipLocation chip) {
		this.chip = chip.asChipLocation();
	}
}
