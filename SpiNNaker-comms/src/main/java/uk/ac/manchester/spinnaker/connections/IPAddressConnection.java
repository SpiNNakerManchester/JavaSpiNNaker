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

import static uk.ac.manchester.spinnaker.messages.Constants.UDP_BOOT_CONNECTION_DEFAULT_PORT;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import uk.ac.manchester.spinnaker.connections.model.MessageReceiver;

/**
 * A connection that detects any UDP packet that is transmitted by SpiNNaker
 * boards prior to boot.
 */
public class IPAddressConnection extends UDPConnection<InetAddress>
		implements MessageReceiver<InetAddress> {
	/** Matches SPINN_PORT in spinnaker_bootROM. */
	private static final int BOOTROM_SPINN_PORT = 54321;

	public IPAddressConnection() throws IOException {
		this(null, UDP_BOOT_CONNECTION_DEFAULT_PORT);
	}

	public IPAddressConnection(InetAddress localHost) throws IOException {
		this(localHost, UDP_BOOT_CONNECTION_DEFAULT_PORT);
	}

	public IPAddressConnection(InetAddress localHost, int localPort)
			throws IOException {
		super(localHost, localPort, null, null, null);
	}

	/**
	 * @return The IP address, or {@code null} if none was forthcoming.
	 */
	@Override
	public final InetAddress receiveMessage() {
		return receiveMessage(Integer.MAX_VALUE);
	}

	/**
	 * @param timeout
	 *            How long to wait for an IP address; {@code null} for forever.
	 * @return The IP address, or {@code null} if none was forthcoming.
	 */
	@Override
	public InetAddress receiveMessage(int timeout) {
		try {
			var packet = receiveWithAddress(timeout);
			var addr = packet.getAddress();
			if (addr instanceof InetSocketAddress) {
				var inetAddr = (InetSocketAddress) addr;
				if (inetAddr.getPort() == BOOTROM_SPINN_PORT) {
					return inetAddr.getAddress();
				}
			}
		} catch (IOException e) {
			// Do nothing
		}
		return null;
	}
}
