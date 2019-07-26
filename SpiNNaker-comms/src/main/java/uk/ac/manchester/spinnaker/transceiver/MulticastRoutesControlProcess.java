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

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Integer.toUnsignedLong;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.ROUTER_AVAILABLE_ENTRIES;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MulticastRoutingEntry;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.messages.scp.RouterAlloc;
import uk.ac.manchester.spinnaker.messages.scp.RouterInit;

/**
 * A process for reading and writing the multicast routing table of a SpiNNaker
 * chip.
 */
class MulticastRoutesControlProcess extends WriteMemoryProcess {
	private static final long INVALID_ROUTE_MARKER = 0xFF000000L;
	/** Each routing table entry is 16 bytes long. */
	private static final int BYTES_PER_ENTRY = 16;
	/** 16 entries fit in a 256-byte read. */
	private static final int ENTRIES_PER_READ =
			UDP_MESSAGE_MAX_SIZE / BYTES_PER_ENTRY;
	/** 64 reads of 16 entries are required for 1024 entries. */
	private static final int NUM_READS =
			ROUTER_AVAILABLE_ENTRIES / ENTRIES_PER_READ;
	private static final int END = 0xFFFFFFFF;
	/**
	 * The maximum number of router entries we can write. A hardware constraint
	 * for SpiNNaker 1 chips.
	 */
	private static final int MAX_ROUTER_ENTRIES = 1023;
	private static final int ROUTING_TABLE_ADDRESS = 0x67800000;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public MulticastRoutesControlProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * 16 bytes per entry plus one for the end entry.
	 *
	 * @param entries
	 *            The number of <em>information-containing</em> entries we want
	 *            to write.
	 * @return A buffer big enough to hold everything.
	 */
	private static ByteBuffer allocateBuffer(int entries) {
		return allocate(BYTES_PER_ENTRY * (entries + 1)).order(LITTLE_ENDIAN);
	}

	private static void writeEntryToBuffer(ByteBuffer buffer, short index,
			MulticastRoutingEntry route) {
		buffer.putShort(index);
		buffer.putShort((short) 0);
		buffer.putInt(route.encode());
		buffer.putInt(route.getKey());
		buffer.putInt(route.getMask());
	}

	private static void writeEndToBuffer(ByteBuffer buffer) {
		buffer.putInt(END);
		buffer.putInt(END);
		buffer.putInt(END);
		buffer.putInt(END);
	}

	private static ByteBuffer serializeRoutingData(
			Collection<MulticastRoutingEntry> routes) {
		ByteBuffer buffer = allocateBuffer(routes.size());
		short index = 0;
		for (MulticastRoutingEntry route : routes) {
			writeEntryToBuffer(buffer, index++, route);
		}

		// Add an entry to mark the end
		writeEndToBuffer(buffer);
		buffer.flip();
		return buffer;
	}

	/**
	 * Load some multicast routes into the chip's router.
	 *
	 * @param chip
	 *            The chip whose router is to be updated.
	 * @param routes
	 *            The routes to load. There must not be more than 1023 routes
	 *            (given hardware constraints).
	 * @param appID
	 *            The application ID associated with the routes.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public void setRoutes(HasChipLocation chip,
			Collection<MulticastRoutingEntry> routes, AppID appID)
			throws IOException, ProcessException {
		if (routes.size() > MAX_ROUTER_ENTRIES) {
			throw new RuntimeException(
					"too many router entries: " + routes.size());
		}
		// Create the routing data
		ByteBuffer routingData = serializeRoutingData(routes);

		// Upload the data
		writeMemory(chip.getScampCore(), ROUTING_TABLE_ADDRESS, routingData);

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

	/**
	 * Get the multicast routes from a chip's router.
	 *
	 * @param chip
	 *            The chip to read from.
	 * @param baseAddress
	 *            Where the routing table is.
	 * @param appID
	 *            What application is associated with the routes we are
	 *            interested in. Use {@code null} to read all routes.
	 * @return The list of routes.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public List<MulticastRoutingEntry> getRoutes(HasChipLocation chip,
			int baseAddress, AppID appID) throws IOException, ProcessException {
		Map<Integer, MulticastRoutingEntry> routes = new TreeMap<>();
		for (int i = 0; i < NUM_READS; i++) {
			int offset = i * ENTRIES_PER_READ;
			sendRequest(
					new ReadMemory(chip, baseAddress + offset * BYTES_PER_ENTRY,
							UDP_MESSAGE_MAX_SIZE),
					response -> addRoutes(response.data, offset, routes,
							appID));
		}
		finish();
		checkForError();
		return new ArrayList<>(routes.values());
	}

	private void addRoutes(ByteBuffer data, int offset,
			Map<Integer, MulticastRoutingEntry> routes, AppID appID) {
		for (int r = 0; r < ENTRIES_PER_READ; r++) {
			data.getShort(); // Ignore
			AppID appid = new AppID(toUnsignedInt(data.get()));
			data.get(); // Ignore
			int route = data.getInt();
			int key = data.getInt();
			int mask = data.getInt();

			if (toUnsignedLong(route) < INVALID_ROUTE_MARKER
					&& (appID == null || appID.equals(appid))) {
				routes.put(r + offset,
						new MulticastRoutingEntry(key, mask, route, false));
			}
		}
	}
}
