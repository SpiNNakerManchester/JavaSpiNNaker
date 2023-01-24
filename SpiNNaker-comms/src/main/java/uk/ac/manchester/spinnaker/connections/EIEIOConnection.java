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
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessageFactory.readCommandMessage;
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessageFactory.readDataMessage;
import static uk.ac.manchester.spinnaker.transceiver.Utils.newMessageBuffer;

import java.io.IOException;
import java.net.InetAddress;

import uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommand;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandMessage;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOHeader;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessage;

/** A UDP connection for sending and receiving raw EIEIO messages. */
public class EIEIOConnection
		extends UDPConnection<EIEIOMessage<? extends EIEIOHeader>> {
	/**
	 * Create an EIEIO connection only available for listening.
	 *
	 * @param localHost
	 *            The local IP address to bind to. If not specified, it defaults
	 *            to binding to all interfaces, unless remoteHost is specified,
	 *            in which case binding is done to the IP address that will be
	 *            used to send packets.
	 * @param localPort
	 *            The local port to bind to, 0 or between 1025 and 65535.
	 * @throws IOException
	 *             If there is an error setting up the communication channel
	 */
	public EIEIOConnection(InetAddress localHost, Integer localPort)
			throws IOException {
		super(localHost, localPort, null, null, IPTOS_THROUGHPUT);
	}

	/**
	 * Create an EIEIO connection where the mechanism for the sending and
	 * listening is implemented by a subclass.
	 */
	protected EIEIOConnection() {
		super(true);
	}

	/**
	 * Sends an EIEIO message down this connection.
	 *
	 * @param eieioMessage
	 *            The EIEIO message to be sent
	 * @throws IOException
	 *             If there is an error sending the message
	 */
	public void sendEIEIOMessage(EIEIOMessage<?> eieioMessage)
			throws IOException {
		var b = newMessageBuffer();
		eieioMessage.addToBuffer(b);
		b.flip();
		send(b);
	}

	/**
	 * Send a raw command.
	 *
	 * @param command
	 *            The command to send.
	 * @throws IOException
	 *             If sending fails.
	 */
	protected void sendCommand(int command) throws IOException {
		sendCommand(EIEIOCommandID.get(command));
	}

	/**
	 * Send a raw command.
	 *
	 * @param command
	 *            The command to send.
	 * @throws IOException
	 *             If sending fails.
	 */
	protected void sendCommand(EIEIOCommand command) throws IOException {
		sendEIEIOMessage(new EIEIOCommandMessage(command));
	}

	/**
	 * Send a raw command.
	 *
	 * @param command
	 *            The command to send.
	 * @param ipAddress
	 *            The host to send to.
	 * @param port
	 *            The port to send to.
	 * @throws IOException
	 *             If sending fails.
	 */
	protected void sendCommand(EIEIOCommandID command, InetAddress ipAddress,
			int port) throws IOException {
		sendEIEIOMessageTo(new EIEIOCommandMessage(command), ipAddress, port);
	}

	/**
	 * Receive a raw command.
	 *
	 * @return the command ID
	 * @throws IOException
	 *             If receiving fails.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	protected EIEIOCommand receiveCommand()
			throws IOException, InterruptedException {
		var msg = receiveMessage();
		if (msg instanceof EIEIOCommandMessage) {
			return ((EIEIOCommandMessage) msg).getHeader().command;
		}
		throw new IOException("unexpected data message");
	}

	/**
	 * Send an EIEIO message to a specific destination.
	 *
	 * @param eieioMessage
	 *            The message to send.
	 * @param ipAddress
	 *            The host to send to.
	 * @param port
	 *            The port to send to.
	 * @throws IOException
	 *             If anything goes wrong in sending.
	 */
	public void sendEIEIOMessageTo(
			EIEIOMessage<? extends EIEIOHeader> eieioMessage,
			InetAddress ipAddress, int port) throws IOException {
		var b = newMessageBuffer();
		eieioMessage.addToBuffer(b);
		b.flip();
		sendTo(b, ipAddress, port);
	}

	private static final int MASK = 0xC000;

	private static final int FLAG = 0x4000;

	@Override
	public EIEIOMessage<? extends EIEIOHeader> receiveMessage(int timeout)
			throws IOException, InterruptedException {
		var b = receive(timeout);
		short header = b.getShort();
		if ((header & MASK) == FLAG) {
			return readCommandMessage(b);
		} else {
			return readDataMessage(b);
		}
	}
}
