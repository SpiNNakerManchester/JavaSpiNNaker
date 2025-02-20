/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Integer.toUnsignedLong;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.ROUTER_AVAILABLE_ENTRIES;
import static uk.ac.manchester.spinnaker.transceiver.CommonMemoryLocations.ROUTING_TABLE_DATA;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.MulticastRoutingEntry;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.scp.RouterAlloc;
import uk.ac.manchester.spinnaker.messages.scp.RouterInit;

/**
 * A process for reading and writing the multicast routing table of a SpiNNaker
 * chip.
 */
class MulticastRoutesControlProcess extends TxrxProcess {
    private static final long INVALID_ROUTE_MARKER = 0xFF000000L;

    /** Each routing table entry is 16 bytes long. */
    private static final int BYTES_PER_ENTRY = 16;

    private static final int END = 0xFFFFFFFF;

    /**
     * The maximum number of router entries we can write. A hardware constraint
     * for SpiNNaker 1 chips.
     */
    private static final int MAX_ROUTER_ENTRIES = 1023;

    private final Transceiver txrx;

    /**
     * @param connectionSelector
     *            How to select how to communicate.
     * @param retryTracker
     *            Object used to track how many retries were used in an
     *            operation. May be {@code null} if no suck tracking is
     *            required.
     * @param txrx
     * 			  A transceiver to do the writing of memory.
     */
    MulticastRoutesControlProcess(
            ConnectionSelector<? extends SCPConnection> connectionSelector,
            RetryTracker retryTracker, Transceiver txrx) {
        super(connectionSelector, retryTracker);
        this.txrx = txrx;
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
        var buffer = allocateBuffer(routes.size());
        short index = 0;
        for (var route : routes) {
            writeEntryToBuffer(buffer, index++, route);
        }

        // Add an entry to mark the end
        writeEndToBuffer(buffer);
        return buffer.flip();
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
     * @throws InterruptedException
     *             If the communications were interrupted.
     * @throws IllegalArgumentException
     *             If there are more routes to apply than can fit in a router.
     * @throws RuntimeException
     *             If on-chip memory allocation fails.
     */
    void setRoutes(HasChipLocation chip,
            Collection<MulticastRoutingEntry> routes, AppID appID)
            throws IOException, ProcessException, InterruptedException {
        if (routes.size() > MAX_ROUTER_ENTRIES) {
            throw new IllegalArgumentException(
                    "too many router entries: " + routes.size());
        }
        // Create the routing data
        var routingData = serializeRoutingData(routes);

        // Upload the data
        txrx.writeMemory(chip.getScampCore(), ROUTING_TABLE_DATA, routingData);

        // Allocate space in the router table
        int baseIndex = retrieve(new RouterAlloc(chip, appID, routes.size()));

        // Load the entries
        txrx.call(new RouterInit(chip, routes.size(),
                ROUTING_TABLE_DATA, baseIndex, appID));
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
     * @throws InterruptedException
     *             If the communications were interrupted.
     */
    List<MulticastRoutingEntry> getRoutes(HasChipLocation chip,
            MemoryLocation baseAddress, AppID appID)
            throws IOException, ProcessException, InterruptedException {
        var routes = new ArrayList<MulticastRoutingEntry>();
        var nBytes = ROUTER_AVAILABLE_ENTRIES * BYTES_PER_ENTRY;
        var data = txrx.readMemory(chip, baseAddress, nBytes);
        for (int r = 0; r < ROUTER_AVAILABLE_ENTRIES; r++) {
            data.getShort(); // Ignore
            var appid = new AppID(toUnsignedInt(data.get()));
            data.get(); // Ignore
            int route = data.getInt();
            int key = data.getInt();
            int mask = data.getInt();

            if (toUnsignedLong(route) < INVALID_ROUTE_MARKER
                    && (appID == null || appID.equals(appid))) {
                routes.add(new MulticastRoutingEntry(key, mask, route, false));
            }
        }
        return Collections.unmodifiableList(routes);
    }
}
