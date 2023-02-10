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

import static uk.ac.manchester.spinnaker.messages.Constants.UDP_BOOT_CONNECTION_DEFAULT_PORT;

import java.io.IOException;
import java.net.InetAddress;

/**
 * A connection that detects any UDP packet that is transmitted by SpiNNaker
 * boards prior to boot. Note that pre-boot messages contain no useful payload.
 */
public class IPAddressConnection extends UDPConnection<InetAddress> {
	/** Matches SPINN_PORT in spinnaker_bootROM. */
	private static final int BOOTROM_SPINN_PORT = 54321;

	/**
	 * Create a connection listening on the default SpiNNaker pre-boot broadcast
	 * port.
	 *
	 * @throws IOException
	 *             If setting up the network fails.
	 */
	public IPAddressConnection() throws IOException {
		this(null, UDP_BOOT_CONNECTION_DEFAULT_PORT);
	}

	/**
	 * Create a connection listening on the default SpiNNaker pre-boot broadcast
	 * port.
	 *
	 * @param localHost
	 *            Local hostname to bind to.
	 * @throws IOException
	 *             If setting up the network fails.
	 */
	public IPAddressConnection(InetAddress localHost) throws IOException {
		this(localHost, UDP_BOOT_CONNECTION_DEFAULT_PORT);
	}

	/**
	 * Create a connection.
	 *
	 * @param localHost
	 *            Local hostname to bind to.
	 * @param localPort
	 *            Local port to bind to.
	 * @throws IOException
	 *             If setting up the network fails.
	 */
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
			if (addr.getPort() == BOOTROM_SPINN_PORT) {
				return addr.getAddress();
			}
		} catch (IOException e) {
			// Do nothing
		}
		return null;
	}
}
