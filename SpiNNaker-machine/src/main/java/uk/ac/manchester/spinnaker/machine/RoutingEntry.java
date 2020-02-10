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
package uk.ac.manchester.spinnaker.machine;

import static java.util.Collections.unmodifiableCollection;
import static java.util.stream.IntStream.range;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_LINKS_PER_ROUTER;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_NUM_CORES;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** A basic SpiNNaker routing entry. */
public class RoutingEntry {
    private final Set<Integer> processorIDs = new LinkedHashSet<>();
    private final Set<Direction> linkIDs = new LinkedHashSet<>();

    private static boolean bitset(int word, int bit) {
        return (word & (1 << bit)) != 0;
    }

    /**
     * Create a routing entry from its encoded form.
     *
     * @param route
     *            the encoded route
     */
    public RoutingEntry(int route) {
        range(0, MAX_NUM_CORES)
                .filter(pidx -> bitset(route, MAX_LINKS_PER_ROUTER + pidx))
                .forEach(processorIDs::add);
        range(0, MAX_LINKS_PER_ROUTER).filter(lidx -> bitset(route, lidx))
                .forEach(this::addLinkID);
    }

    /**
     * Create a routing entry from its expanded description.
     *
     * @param processorIDs
     *            The IDs of the processors that this entry routes to. The
     *            Duplicate IDs are ignored.
     * @param linkIDs
     *            The IDs of the links that this entry routes to. The Duplicate
     *            IDs are ignored.
     * @throws IllegalArgumentException
     *             If a bad processor ID is given (i.e., one that doesn't match
     *             SpiNNaker hardware).
     */
    public RoutingEntry(
            Iterable<Integer> processorIDs, Iterable<Direction> linkIDs) {
        for (int procId : processorIDs) {
            if (procId >= MAX_NUM_CORES || procId < 0) {
                throw new IllegalArgumentException(
                    "Processor IDs must be between 0 and "
                            + (MAX_NUM_CORES - 1) + " found " + procId);
            }
            this.processorIDs.add(procId);
        }
        for (Direction linkIds: linkIDs) {
            this.linkIDs.add(linkIds);
        }
    }

    /**
     * gets the Entry as a single word.
     *
     * @return The word-encoded form of the routing entry.
     */
    public int encode() {
        int route = 0;
        for (Integer processorID : processorIDs) {
            route |= 1 << (MAX_LINKS_PER_ROUTER + processorID);
        }
        for (Direction linkID : linkIDs) {
            route |= 1 << linkID.id;
        }
        return route;
    }

    /**
     * The IDs of the processors that this entry routes to.
     * <p>
     * If the RoutingEntry was created from its encoded form
     * and unmodified after that, the list is guaranteed to be sorted in
     * natural order and contain no duplicates.
     *
     * @return An unmodifiable over the processor IDs.
     */
    public Collection<Direction> getLinkIDs() {
        return unmodifiableCollection(linkIDs);
    }

    /**
     * The ID/Directions of the links that this entry routes to.
     * <p>
     * If the RoutingEntry was created from its encoded form
     * and unmodified after that, the list is guaranteed to be sorted in
     * natural order and contain no duplicates.
     *
     * @return An unmodifiable view over the link IDs in natural order.
     */
    public Collection<Integer> getProcessorIDs() {
        return unmodifiableCollection(processorIDs);
    }

    /**
     * Adds extra link ID/Directions to the entry.
     *
     * @param newValues
     *            The IDs of the links to add.
     *            Duplicate ID will be ignored.
     */

    /**
     * Adds an extra link ID/Direction to the entry.
     *
     * @param newValue
     *            The ID of the links to add.
     *            The Duplicate IDs are ignored.
      * @throws ArrayIndexOutOfBoundsException
     *      If the new Value does not map to a Direction.
     */
    private void addLinkID(int newValue) {
        linkIDs.add(Direction.byId(newValue));
    }
}
