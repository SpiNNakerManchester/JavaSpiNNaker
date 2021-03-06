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

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessageFactory.readCommandMessage;
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessageFactory.readDataMessage;
import static uk.ac.manchester.spinnaker.transceiver.Utils.newMessageBuffer;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.model.EIEIOReceiver;
import uk.ac.manchester.spinnaker.connections.model.EIEIOSender;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommand;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandMessage;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOHeader;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessage;

/** A UDP connection for sending and receiving raw EIEIO messages. */
public class EIEIOConnection
		extends UDPConnection<EIEIOMessage<? extends EIEIOHeader>>
		implements EIEIOReceiver, EIEIOSender {
	/**
	 * Create an EIEIO connection only available for listening, using default
	 * local port.
	 *
	 * @param localHost
	 *            The local IP address to bind to. If not specified, it defaults
	 *            to binding to all interfaces, unless remoteHost is specified,
	 *            in which case binding is done to the IP address that will be
	 *            used to send packets.
	 * @throws IOException
	 *             If there is an error setting up the communication channel
	 */
	public EIEIOConnection(InetAddress localHost) throws IOException {
		super(localHost, null, null, null);
	}

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
		super(localHost, localPort, null, null);
	}

	/**
	 * Create an EIEIO connection.
	 *
	 * @param localHost
	 *            The local host to bind to. If not specified, it defaults to
	 *            binding to all interfaces, unless remoteHost is specified, in
	 *            which case binding is done to the IP address that will be used
	 *            to send packets.
	 * @param localPort
	 *            The local port to bind to, 0 or between 1025 and 65535.
	 * @param remoteHost
	 *            The remote host to send packets to. If not specified, the
	 *            socket will be available for listening only, and will throw
	 *            and exception if used for sending.
	 * @param remotePort
	 *            The remote port to send packets to. If remoteHost is
	 *            {@code null}, this is ignored. If remoteHost is specified,
	 *            this must also be specified as non-zero for the connection to
	 *            allow sending.
	 * @throws IOException
	 *             If there is an error setting up the communication channel
	 */
	public EIEIOConnection(InetAddress localHost, Integer localPort,
			InetAddress remoteHost, Integer remotePort) throws IOException {
		super(localHost, localPort, remoteHost, remotePort);
	}

	@Override
	public void sendEIEIOMessage(EIEIOMessage<?> eieioMessage)
			throws IOException {
		ByteBuffer b = newMessageBuffer();
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
	 */
	protected EIEIOCommand receiveCommand() throws IOException {
		EIEIOMessage<?> msg = receiveMessage();
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
		ByteBuffer b = newMessageBuffer();
		eieioMessage.addToBuffer(b);
		b.flip();
		sendTo(b, ipAddress, port);
	}

	private static final int MASK = 0xC000;

	private static final int FLAG = 0x4000;

	@Override
	public EIEIOMessage<? extends EIEIOHeader> receiveMessage(Integer timeout)
			throws IOException {
		ByteBuffer b = receive(timeout);
		short header = b.getShort();
		if ((header & MASK) == FLAG) {
			return readCommandMessage(b);
		} else {
			return readDataMessage(b);
		}
	}
}
