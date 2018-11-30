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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static java.util.stream.IntStream.range;

/** A basic SpiNNaker routing entry. */
public class RoutingEntry {
    private static final int NUM_PROCESSORS = 32 - Direction.values().length;
    private static final int NUM_LINKS = Direction.values().length;

    private final List<Integer> processorIDs = new ArrayList<>();
    private final List<Direction> linkIDs = new ArrayList<>();

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
        range(0, NUM_PROCESSORS).filter(pi -> bitset(route, NUM_LINKS + pi))
                .forEach(processorIDs::add);
        range(0, NUM_LINKS).filter(li -> bitset(route, li))
                .forEach(this::addLinkID);
    }

   /**
     * Create a routing entry from its expanded description.
     *
     * @param processorIDs
     *            The IDs of the processors that this entry routes to.
     *            The Duplicate IDs are ignored.
     * @param linkIDs
     *            The IDs of the links that this entry routes to.
     *            The Duplicate IDs are ignored.
     */
    public RoutingEntry(
            Iterable<Integer> processorIDs, Iterable<Direction> linkIDs) {
        addProcessorIDs(processorIDs);
        addLinkIDs(linkIDs);
    }

    /**
     * gets the Entry as a single word.
     *
     * @return The word-encoded form of the routing entry.
     */
    public int encode() {
        int route = 0;
        for (Integer processorID : processorIDs) {
            route |= 1 << (NUM_LINKS + processorID);
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
    public List<Direction> getLinkIDs() {
        return Collections.unmodifiableList(linkIDs);
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
    public List<Integer> getProcessorIDs() {
        return Collections.unmodifiableList(processorIDs);
    }

    /**
     * Adds extra processors to this routing entry.
     *
     * @param newValues
     *            The IDs of the processors to add.
     *            The Duplicate IDs are ignored.
     */
    public void addProcessorIDs(Iterable<Integer> newValues) {
        for (int newValue : newValues) {
            addProcessorID(newValue);
        }
    }

    /**
     * Adds an extra processor to this routing entry.
     *
     * @param newValue
     *            The ID of the processors to add.
     *            The Duplicate IDs will not effect the encode value,
     *            and may or may not be ignore prior to that.
     */
    public void addProcessorID(Integer newValue) {
        if (newValue >= NUM_PROCESSORS || newValue < 0) {
            throw new IllegalArgumentException(
                    "Processor IDs must be between 0 and "
                    + NUM_PROCESSORS + " found " + newValue);
        }
        if (!processorIDs.contains(newValue)) {
            processorIDs.add(newValue);
        }
    }

    /**
     * Removes a processor ID if it existed.
     *
     * @param oldValue The Id of the processor to remove.
     * @return {@code true} If this entry contained the specified processor.
     */
    public boolean removeProcessorID(Integer oldValue) {
        return processorIDs.remove(oldValue);
    }

    /**
     * Adds extra link ID/Directions to the entry.
     *
     * @param newValues
     *            The IDs of the links to add.
     *            Duplicate ID will be ignored.
     */
    public void addLinkIDs(Iterable<Direction> newValues) {
        for (Direction newValue: newValues) {
            addLinkID(newValue);
        }
    }

    /**
     * Adds an extra link ID/Direction to the entry.
     *
     * @param newValue
     *            The ID of the links to add.
     *            The Duplicate IDs are ignored..
     */
    public void addLinkID(Direction newValue) {
        linkIDs.add(newValue);
    }

    /**
     * Adds an extra link ID/Direction to the entry.
     *
     * @param newValue
     *            The ID of the links to add.
     *            The Duplicate IDs are ignored.
      * @throws ArrayIndexOutOfBoundsException
     *      If the new Value does not map to a Direction.
     */
    public void addLinkID(int newValue) {
        Direction d = Direction.byId(newValue);
        if (!linkIDs.contains(d)) {
            linkIDs.add(d);
        }
    }

    /**
     * Removes a link ID/Direction if it existed.
     *
     * @param oldValue The Id of the processor to remove.
     * @return {@code true} If this entry contained the specified
     *      link/direction.
     */
    public boolean removeLinkID(Direction oldValue) {
        return linkIDs.remove(oldValue);
    }
}
