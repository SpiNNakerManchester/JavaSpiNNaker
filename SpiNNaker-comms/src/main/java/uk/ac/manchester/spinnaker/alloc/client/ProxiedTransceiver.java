/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.client;

import static uk.ac.manchester.spinnaker.machine.MachineVersion.TRIAD_NO_WRAPAROUND;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;

import uk.ac.manchester.spinnaker.connections.EIEIOConnection;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/** A transceiver that routes messages across the proxy. */
final class ProxiedTransceiver extends Transceiver {
	private final ProxyProtocolClient websocket;

	private final Map<Inet4Address, ChipLocation> hostToChip;

	/**
	 * @param connections
	 *            The proxied connections we will use.
	 * @param hostToChip
	 *            The mapping from addresses to chip locations, to enable
	 *            manufacturing of proxied {@link EIEIOConnection}s.
	 * @param websocket
	 *            The proxy handle.
	 * @throws IOException
	 *             If we couldn't finish setting up our networking.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws SpinnmanExcception
	 *             If SpiNNaker rejects a message.
	 */
	ProxiedTransceiver(Collection<Connection> connections,
			Map<Inet4Address, ChipLocation> hostToChip,
			ProxyProtocolClient websocket)
			throws IOException, SpinnmanException, InterruptedException {
		// Assume unwrapped
		super(TRIAD_NO_WRAPAROUND, connections, null, null, null, null,
				null);
		this.hostToChip = hostToChip;
		this.websocket = websocket;
	}

	/** {@inheritDoc} */
	@Override
	public void close() throws IOException {
		super.close();
		websocket.close();
	}

	@Override
	public SCPConnection createScpConnection(ChipLocation chip,
			InetAddress addr) throws IOException {
		try {
			return new ProxiedSCPConnection(chip, websocket);
		} catch (InterruptedException e) {
			throw new IOException("failed to proxy connection", e);
		}
	}

	@Override
	protected EIEIOConnection newEieioConnection(InetAddress localHost,
			Integer localPort) throws IOException {
		try {
			return new ProxiedEIEIOListenerConnection(hostToChip, websocket);
		} catch (InterruptedException e) {
			throw new IOException("failed to proxy connection", e);
		}
	}
}
