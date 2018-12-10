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

import java.util.Objects;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.List;

/**
 *
 * @author Christian-B
 */
public final class Router implements Iterable<Link> {

    private final EnumMap<Direction, Link> links =
            new EnumMap<>(Direction.class);

    /** The router clock speed in cycles per second. */
    public final int clockSpeed;

    /** The number of entries available in the routing table. */
    public final int nAvailableMulticastEntries;

    // Note: emergency_routing_enabled not implemented as not used
    // TODO convert_routing_table_entry_to_spinnaker_route

    /**
     * Default Constructor to add links later.
     *
     * @param clockSpeed The router clock speed in cycles per second.
     * @param nAvailableMulticastEntries
     *      The number of entries available in the routing table.
     */
    public Router(int clockSpeed, int nAvailableMulticastEntries)
            throws IllegalArgumentException {
        this.clockSpeed = clockSpeed;
        this.nAvailableMulticastEntries = nAvailableMulticastEntries;
    }

    /**
     * Default Constructor to add links later using default values.
     *
      */
    public Router() throws IllegalArgumentException {
        this(MachineDefaults.ROUTER_CLOCK_SPEED,
                MachineDefaults.ROUTER_AVAILABLE_ENTRIES);
    }

    /**
     * Main Constructor that allows setting of all values.
     *
     * @param links Known Link(s) to add.
     *      All must have unique sourceLinkDirection(s).
     * @param clockSpeed The router clock speed in cycles per second.
     * @param nAvailableMulticastEntries
     *      The number of entries available in the routing table.
     */
    public Router(Iterable<Link> links, int clockSpeed,
            int nAvailableMulticastEntries) throws IllegalArgumentException {
        this(clockSpeed, nAvailableMulticastEntries);
        for (Link link:links) {
            addLink(link);
        }

    }

    /**
     * Main Constructor that allows setting of all values.
     *
     * @param links Known Link(s) to add.
     *      All must have unique sourceLinkDirection(s).
     * @param clockSpeed The router clock speed in cycles per second.
     * @param nAvailableMulticastEntries
     *      The number of entries available in the routing table.
     */
    public Router(Stream<Link> links, int clockSpeed,
            int nAvailableMulticastEntries) throws IllegalArgumentException {
        this(clockSpeed, nAvailableMulticastEntries);
        links.forEach(this::addLink);
    }

    /**
     * Pass through Constructor that uses default values.
     *
     * @param links Known Link(s) to add.
     *      All must have unique sourceLinkDirection(s).
     */
    public Router(Iterable<Link> links) throws IllegalArgumentException {
        this(links, MachineDefaults.ROUTER_CLOCK_SPEED,
                MachineDefaults.ROUTER_AVAILABLE_ENTRIES);
    }

    /**
     * Pass through Constructor that uses some default values.
     *
     * @param links Known Link(s) to add.
     *      All must have unique sourceLinkDirection(s).
     * @param nAvailableMulticastEntries
     *      The number of entries available in the routing table.
     */
    public Router(Iterable<Link> links, int nAvailableMulticastEntries)
            throws IllegalArgumentException {
        this(links, MachineDefaults.ROUTER_CLOCK_SPEED,
                nAvailableMulticastEntries);
    }

    /**
     * Pass through Constructor that uses default values.
     *
     * @param links Known Link(s) to add.
     *      All must have unique sourceLinkDirection(s).
     * @throws IllegalArgumentException Indicates another Link with this
     *     sourceLinkDirection has already been added.
     */
    public Router(Stream<Link> links) throws IllegalArgumentException {
        this(links, MachineDefaults.ROUTER_CLOCK_SPEED,
                MachineDefaults.ROUTER_AVAILABLE_ENTRIES);
    }

    /**
     * Shallow copy of all values except the links.
     *
     * @param router original to copy other parameters from.
     * @param links Known Link(s) to add.
     *      All must have unique sourceLinkDirection(s).
     * @throws IllegalArgumentException Indicates another Link with this
     *     sourceLinkDirection has already been added.
     */
    Router(Router router, ArrayList<Link> links) {
        this(links, router.clockSpeed, router.nAvailableMulticastEntries);
    }

    /**
     * Creates a new Router from this source with links in all but the missing
     *      directions.
     * @param source Chip which links are coming from
     * @param clockSpeed The router clock speed in cycles per second.
     * @param nAvailableMulticastEntries
     *      The number of entries available in the routing table.
     * @param ignoreDirections Directions not to create links for.
     * @param machine The Machine this chip will go on. Used for calculating
     *      wrap arounds
     * @throws NullPointerException if a none valid direction is not in
     *      ignoredLinks
     */
    public Router(HasChipLocation source, int clockSpeed,
                  int nAvailableMulticastEntries,
                  Collection<Direction> ignoreDirections, Machine machine) {
        this(clockSpeed, nAvailableMulticastEntries);
        for (Direction direction: Direction.values()) {
            if (!ignoreDirections.contains(direction)) {
                ChipLocation destination = machine.normalizedLocation(
                        source.getX() + direction.xChange,
                        source.getY() + direction.yChange);
                addLink(new Link(source, direction,
                    Objects.requireNonNull(destination)));
            }
        }
    }

    /**
     * Adds a link with a unique sourceLinkDirection to this router.
     *
     * @param link Link to add,
     *     which must have a sourceLinkDirection not yet used.
     * @throws IllegalArgumentException Indicates another Link with this
     *     sourceLinkDirection has already been added.
     */
    public void addLink(Link link) throws IllegalArgumentException {
        if (this.links.containsKey(link.sourceLinkDirection)) {
            throw new IllegalArgumentException(
                    "Link already exists: " + link);
        }
        this.links.put(link.sourceLinkDirection, link);
    }

    /**
     * Indicates if there is a Link going in this direction.
     *
     * @param direction Direction to find link for.
     * @return True if and only if there is a link in this direction,
     */
    public boolean hasLink(Direction direction) {
        return links.containsKey(direction);
    }

    /**
     * Obtains a Link going in this direction.
     * <p>
     * None is returned if no link found.
     *
     * @param direction Direction to find link for.
     * @return The Link or none
     */
    public Link getLink(Direction direction) {
        return links.get(direction);
    }

    /**
     * Return a View over the links.
     * <p>
     * Each Link is guaranteed to differ in at least the sourceLinkDirection.
     *
     * @return An unmodifiable Collection of Link(s).
     */
    public Collection<Link> links() {
        return Collections.unmodifiableCollection(links.values());
    }

    /**
     * The size of the Router which is the number of Link(s).
     * <p>
     * The number of NeighbouringChipsCoords will always be equal to the
     *     number of links.
     *
     * @return The number of Link(s) and therefor NeighbouringChipsCoords
     */
    public int size() {
        return links.size();
    }

    /**
     * Stream of the destinations of each link.
     * <p>
     * There will be exactly one destination for each Link.
     * While normally all destinations will be unique the is no guarantee.
     *
     * @return A Stream over the destination locations.
     */
    public Stream<ChipLocation> streamNeighbouringChipsCoords() {
        return links.values().stream().map(link -> link.destination);
    }

    /**
     * Iterable over the destinations of each link.
     * <p>
     * There will be exactly one destination for each Link.
     * While normally all destinations will be unique the is no guarantee.
     *
     * @return A Stream over the destination locations.
     */
    public Iterable<ChipLocation> iterNeighbouringChipsCoords() {
        return () -> new NeighbourIterator(links.values().iterator());
    }

    /**
     * List of the destination for all links.
     * <p>
     * Note: Changes to the resulting list will not effect the actual links.
     * This function in the future may return an unmodifiable list.
     *
     * @return The destination locations
     */
    public List<ChipLocation> neighbouringChipsCoords() {
        ArrayList<ChipLocation> neighbours = new ArrayList<>();
        for (Link link: links.values()) {
            neighbours.add(link.destination);
        }
        return neighbours;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Router[");
        for (Entry<Direction, Link> entry:links.entrySet()) {
            result.append(entry.getKey());
            result.append(":");
            result.append(entry.getValue().destination);
            result.append(" ");
        }
        result.setLength(result.length() - 1);
        result.append("]");
        return result.toString();
    }

    @Override
    public Iterator<Link> iterator() {
        return links.values().iterator();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException(
                "hashCode not supported as equals implemented.");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        // TODO compare internal states
        return obj instanceof Router;
    }

}
