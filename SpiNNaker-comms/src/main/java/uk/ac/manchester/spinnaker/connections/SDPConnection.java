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
	 * @param remoteHost
	 *            The remote host name or IP address to send packets to. If
	 *            {@code null}, the socket will be available for listening only,
	 *            and will throw an exception if used for sending.
	 * @param remotePort
	 *            The remote port to send packets to. If {@code remoteHost} is
	 *            {@code null}, this is ignored. If {@code remoteHost} is
	 *            specified, this must also be specified as non-zero for the
	 *            connection to allow sending.
	 */
	SDPConnection(HasChipLocation remoteChip, InetAddress remoteHost,
			Integer remotePort) {
		super(remoteHost, remotePort);
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
