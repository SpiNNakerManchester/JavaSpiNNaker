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

import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPRequest.BOOT_CHIP;

import java.io.IOException;
import java.net.InetAddress;

import uk.ac.manchester.spinnaker.connections.model.SCPSenderReceiver;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;

/** A UDP connection to SC&amp;MP on the board. */
public class SCPConnection extends SDPConnection
		implements SCPSenderReceiver {
	/**
	 * Create a connection to a particular instance of SCAMP.
	 *
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(InetAddress remoteHost) throws IOException {
		this(BOOT_CHIP, null, null, remoteHost, SCP_SCAMP_PORT);
	}

	/**
	 * Create a connection to a particular instance of SCAMP.
	 *
	 * @param localHost
	 *            The optional host of the local interface to
	 *            listen on; use {@code null} to listen on all local
	 *            interfaces.
	 * @param localPort
	 *            The optional local port to listen on; use {@code null} to
	 *            pick a random port.
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @param remotePort
	 *            The optional remote port number to send messages to. If
	 *            {@code null}, the default remote port is used.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(InetAddress localHost, Integer localPort,
			InetAddress remoteHost, Integer remotePort) throws IOException {
		this(BOOT_CHIP, localHost, localPort, remoteHost, remotePort);
	}

	/**
	 * Create a connection to a particular instance of SCAMP.
	 *
	 * @param chip
	 *            The location of the chip on the board with this remoteHost
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(HasChipLocation chip, InetAddress remoteHost)
			throws IOException {
		this(chip, null, null, remoteHost, SCP_SCAMP_PORT);
	}

	/**
	 * Create a connection to a particular instance of SCAMP.
	 *
	 * @param chip
	 *            The location of the chip on the board with this remoteHost
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @param remotePort
	 *            The optional remote port number to send messages to. If
	 *            {@code null}, the default remote port is used.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(HasChipLocation chip, InetAddress remoteHost,
			Integer remotePort) throws IOException {
		this(chip, null, null, remoteHost, remotePort);
	}

	/**
	 * Create a connection to a particular instance of SCAMP.
	 *
	 * @param chip
	 *            The location of the chip on the board with this remoteHost
	 * @param localHost
	 *            The optional host of the local interface to
	 *            listen on; use {@code null} to listen on all local
	 *            interfaces.
	 * @param localPort
	 *            The optional local port to listen on; use {@code null} to
	 *            pick a random port.
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @param remotePort
	 *            The optional remote port number to send messages to. If
	 *            {@code null}, the default remote port is used.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(HasChipLocation chip, InetAddress localHost,
			Integer localPort, InetAddress remoteHost, Integer remotePort)
			throws IOException {
		super(chip, localHost, localPort, requireNonNull(remoteHost,
				"SCPConnection only meaningful with a real remote host"),
				(remotePort == null) ? SCP_SCAMP_PORT : remotePort);
	}

	/**
	 * Create a connection where the mechanism for sending and receiving
	 * messages is being overridden by a subclass.
	 *
	 * @param chip
	 *            The location of the target chip on the board.
	 * @throws IOException
	 *             If anything goes wrong with socket manipulation.
	 */
	protected SCPConnection(HasChipLocation chip) throws IOException {
		super(chip, true);
	}

	@Override
	public SCPResultMessage receiveSCPResponse(int timeout)
			throws IOException {
		return new SCPResultMessage(receive(timeout));
	}

	@Override
	public void send(SCPRequest<?> scpRequest) throws IOException {
		send(getSCPData(scpRequest));
	}
}
