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
package uk.ac.manchester.spinnaker.transceiver.processes;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MulticastRoutingEntry;
import uk.ac.manchester.spinnaker.messages.scp.RouterAlloc;
import uk.ac.manchester.spinnaker.messages.scp.RouterInit;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/** A process for loading the multicast routing table on a SpiNNaker chip. */
public class LoadMulticastRoutesProcess
		extends MultiConnectionProcess<SCPConnection> {
	private RetryTracker retryTracker;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public LoadMulticastRoutesProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
		this.retryTracker = retryTracker;
	}

	private static final int UNIT_SIZE = 16;

	private static final int END = 0xFFFFFFFF;

	private static final int ROUTING_TABLE_ADDRESS = 0x67800000;

	private static void writeEntry(ByteBuffer buffer, short index,
			MulticastRoutingEntry route) {
		buffer.putShort(index);
		buffer.putShort((short) 0);
		buffer.putInt(route.encode());
		buffer.putInt(route.getKey());
		buffer.putInt(route.getMask());
	}

	private static void writeEnd(ByteBuffer buffer) {
		buffer.putInt(END);
		buffer.putInt(END);
		buffer.putInt(END);
		buffer.putInt(END);
	}

	/**
	 * Load some multicast routes into the chip's router.
	 *
	 * @param chip
	 *            The chip whose router is to be updated.
	 * @param routes
	 *            The routes to load.
	 * @param appID
	 *            The application ID associated with the routes.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public void loadRoutes(HasChipLocation chip,
			Collection<MulticastRoutingEntry> routes, int appID)
			throws IOException, ProcessException {
		/*
		 * Create the routing data. 16 bytes per entry plus one for the end
		 * entry
		 */
		ByteBuffer buffer =
				allocate(UNIT_SIZE * (routes.size() + 1)).order(LITTLE_ENDIAN);
		short index = 0;
		for (MulticastRoutingEntry route : routes) {
			writeEntry(buffer, index++, route);
		}

		// Add an entry to mark the end
		writeEnd(buffer);
		buffer.flip();

		// Upload the data (delegate to another process)
		new WriteMemoryProcess(selector, retryTracker).writeMemory(
				chip.getScampCore(), ROUTING_TABLE_ADDRESS, buffer);

		// Allocate space in the router table
		int baseAddress = synchronousCall(
				new RouterAlloc(chip, appID, routes.size())).baseAddress;
		if (baseAddress == 0) {
			throw new RuntimeException("Not enough space to allocate "
					+ routes.size() + " entries");
		}

		// Load the entries
		synchronousCall(new RouterInit(chip, routes.size(),
				ROUTING_TABLE_ADDRESS, baseAddress, appID));
	}
}
