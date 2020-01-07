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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.model.SDPReceiver;
import uk.ac.manchester.spinnaker.connections.model.SDPSender;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

/** A UDP socket connection that talks SDP to SpiNNaker. */
public class SDPConnection extends UDPConnection<SDPMessage>
		implements SDPReceiver, SDPSender {
	private ChipLocation chip;
	/*
	 * Special constructor used only in delegated connections.
	 */
	SDPConnection(SDPConnection connection) {
		super(connection);
		chip = connection.chip;
	}

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
		super(localHost, localPort, remoteHost, remotePort);
		this.chip = remoteChip.asChipLocation();
	}

	@Override
	public void sendSDPMessage(SDPMessage sdpMessage) throws IOException {
		send(sdpMessage.getMessageData(chip));
	}

	@Override
	public SDPMessage receiveMessage(Integer timeout)
			throws IOException, InterruptedIOException {
		ByteBuffer buffer = receive(timeout);
		buffer.getShort(); // SKIP TWO PADDING BYTES
		return new SDPMessage(buffer);
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
