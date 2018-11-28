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

/** A multicast packet routing table entry. */
public class MulticastRoutingEntry extends RoutingEntry {
    private final int key;
    private final int mask;
    private final boolean defaultable;

    /**
     * Create a multicast routing table entry.
     *
     * @param key
     *            The key of the entry.
     * @param mask
     *            The mask of the entry.
     * @param route
     *            The route descriptor.
     * @param defaultable
     *            Whether this entry is default routable.
     */
    public MulticastRoutingEntry(int key, int mask, int route,
            boolean defaultable) {
        super(route);
        this.key = key;
        this.mask = mask;
        this.defaultable = defaultable;
    }

    /**
     * Create a multicast routing table entry.
     *
     * @param key
     *            The key of the entry.
     * @param mask
     *            The mask of the entry.
     * @param processorIDs
     *            The IDs of the processors that this entry routes to.
     *            The Duplicate IDs are ignored.
     * @param linkIDs
     *            The IDs of the links that this entry routes to.
     *            The Duplicate IDs are ignored.
     * @param defaultable
     *            Whether this entry is default routable.
     */
    public MulticastRoutingEntry(
            int key, int mask, Iterable<Integer> processorIDs,
            Iterable<Direction> linkIDs, boolean defaultable) {
        super(processorIDs, linkIDs);
        this.key = key;
        this.mask = mask;
        this.defaultable = defaultable;
    }

    /** @return the key of this entry */
    public int getKey() {
        return key;
    }

    /** @return the mask of this entry */
    public int getMask() {
        return mask;
    }

    /** @return whether this entry is default routable */
    public boolean isDefaultable() {
        return defaultable;
    }
}
