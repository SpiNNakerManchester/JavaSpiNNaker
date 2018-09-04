/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;

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
        links.forEach((link) -> this.addLink(link));
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
        return links.values().stream().map(
            link -> {
                return link.destination;
            });
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
        return new Iterable<ChipLocation>() {
            @Override
            public Iterator<ChipLocation> iterator() {
                return new NeighbourIterator(links.values().iterator());
            }
        };
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

}
