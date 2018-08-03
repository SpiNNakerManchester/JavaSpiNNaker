package uk.ac.manchester.spinnaker.processes;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Integer.toUnsignedLong;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MulticastRoutingEntry;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;

/** A process for reading the multicast routing table of a SpiNNaker chip. */
public class GetMulticastRoutesProcess
		extends MultiConnectionProcess<SCPConnection> {
	private static final long INVALID_ROUTE_MARKER = 0xFF000000L;
	/** There are 1024 entries in a routing table. */
	private static final int NUM_ENTRIES = 1024;
	/** Each routing table entry is 16 bytes long. */
	private static final int BYTES_PER_ENTRY = 16;
	/** 16 entries fit in a 256-byte read. */
	private static final int ENTRIES_PER_READ =
			UDP_MESSAGE_MAX_SIZE / BYTES_PER_ENTRY;
	/** 64 reads of 16 entries are required for 1024 entries. */
	private static final int NUM_READS = NUM_ENTRIES / ENTRIES_PER_READ;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 */
	public GetMulticastRoutesProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
	}

	/**
	 * Get the multicast routes from a chip's router.
	 *
	 * @param chip
	 *            The chip to read from.
	 * @param baseAddress
	 *            Where the routing table is.
	 * @param appID
	 *            What application is associated with the routes.
	 * @return The list of routes.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public List<MulticastRoutingEntry> getRoutes(HasChipLocation chip,
			int baseAddress, Integer appID) throws IOException, Exception {
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
			Map<Integer, MulticastRoutingEntry> routes, Integer appID) {
		for (int r = 0; r < ENTRIES_PER_READ; r++) {
			data.get(); // Ignore
			data.get(); // Ignore
			int appid = toUnsignedInt(data.get());
			data.get(); // Ignore
			int route = data.getInt();
			int key = data.getInt();
			int mask = data.getInt();

			if (toUnsignedLong(route) < INVALID_ROUTE_MARKER
					&& (appID == null || appID == appid)) {
				routes.put(r + offset,
						new MulticastRoutingEntry(key, mask, route, false));
			}
		}
	}
}
