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
package uk.ac.manchester.spinnaker.transceiver;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_INFO_BYTES;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_INFO_OFFSET;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;

/**
 * Support utilities.
 */
public abstract class Utils {
	/**
	 * The size of buffer to allocate for SpiNNaker messages.
	 */
	public static final int SPINNAKER_MESSAGE_BUFFER_SIZE = 300;

	private Utils() {
	}

	/**
	 * Work out the BMP connection IP address given the machine details. This is
	 * assumed to be the IP address of the machine, with 1 subtracted from the
	 * final part e.g. if the machine IP address is 192.168.0.5, the BMP IP
	 * address is assumed to be 192.168.0.4
	 *
	 * @param host
	 *            the SpiNNaker machine main host
	 * @param numberOfBoards
	 *            the number of boards in the machine
	 * @return The BMP connection data
	 * @throws UnknownHostException
	 *             If the IP address computations fail.
	 */
	public static BMPConnectionData defaultBMPforMachine(InetAddress host,
			Integer numberOfBoards) throws UnknownHostException {
		return new BMPConnectionData(host,
				numberOfBoards == null ? 0 : numberOfBoards);
	}

	/**
	 * Get the address of the {@code vcpu_t} structure for the given core.
	 *
	 * @param p
	 *            The core number
	 * @return the address
	 */
	public static int getVcpuAddress(int p) {
		return CPU_INFO_OFFSET + (CPU_INFO_BYTES * p);
	}

	/**
	 * Get the address of the {@code vcpu_t} structure for the given core.
	 *
	 * @param core
	 *            The core
	 * @return the address
	 */
	public static int getVcpuAddress(HasCoreLocation core) {
		return CPU_INFO_OFFSET + (CPU_INFO_BYTES * core.getP());
	}

	/**
	 * Sends a port trigger message using a connection to (hopefully) open a
	 * port in a NAT and/or firewall to allow incoming packets to be received.
	 *
	 * @param connection
	 *            The UDP connection down which the trigger message should be
	 *            sent
	 * @param host
	 *            The address of the SpiNNaker board to which the message should
	 *            be sent
	 * @deprecated Call
     *             {@link UDPConnection#sendPortTriggerMessage(InetAddress)}
	 *             directly instead.
	 * @throws IOException
	 *             If anything goes wrong
	 */
	@Deprecated
	public static void sendPortTriggerMessage(UDPConnection<?> connection,
			InetAddress host) throws IOException {
		connection.sendPortTriggerMessage(host);
	}

	/**
	 * @return Get a new little-endian buffer sized suitably for SpiNNaker
	 *         messages.
	 */
	public static ByteBuffer newMessageBuffer() {
		// TODO How big should this buffer be? 256 or (256 + header size)?
		return allocate(SPINNAKER_MESSAGE_BUFFER_SIZE).order(LITTLE_ENDIAN);
	}
}
