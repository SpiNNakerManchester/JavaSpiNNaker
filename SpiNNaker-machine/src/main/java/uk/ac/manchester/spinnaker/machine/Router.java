/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 *
 * @author Christian-B
 */
public final class Router {

    private final EnumMap<Direction, Link> links =
            new EnumMap<>(Direction.class);

    /** The router clock speed in cycles per second */
    public final int clockSpeed;

    /** The number of entries available in the routing table */
    public final int nAvailableMulticastEntries;

    // Note: emergency_routing_enabled not implemented as not used
    // TODO convert_routing_table_entry_to_spinnaker_route

    public Router(Collection<Link> links, int clockSpeed,
            int nAvailableMulticastEntries) {
        for (Link link:links){
            addLink(link);
        }
        this.clockSpeed = clockSpeed;
        this.nAvailableMulticastEntries = nAvailableMulticastEntries;
    }

    public final void addLink(Link link) {
        if (this.links.containsKey(link.sourceLinkDirection)) {
            throw new IllegalArgumentException(
                    "Link already exists: " + link);
        }
        this.links.put(link.sourceLinkDirection, link);
    }

    public Router(Collection<Link> links) {
        this(links, MachineDefaults.ROUTER_CLOCK_SPEED,
                MachineDefaults.ROUTER_AVAILABLE_ENTRIES);
    }

    public boolean hasLink(Direction direction) {
        return links.containsKey(direction);
    }

    public Link get_link(Direction direction) {
        return links.get(direction);
    }

    public Collection<Link> links() {
        return Collections.unmodifiableCollection(links.values());
    }

    public Stream<HasChipLocation> streamNeighbouringChipsCoords(){
        return links.values().stream().map(
            link -> {
                return link.destination;
            });
    }

    public Iterable<HasChipLocation> iterNeighbouringChipsCoords(){
        return new Iterable<HasChipLocation>() {
            @Override
            public Iterator<HasChipLocation> iterator() {
                return new NeighbourIterator(links.values().iterator());
            }
        };
    }

    private class NeighbourIterator implements Iterator<HasChipLocation> {

        private Iterator<Link> linksIter;

        NeighbourIterator(Iterator<Link> linksIter){
            this.linksIter = linksIter;
        }

        @Override
        public boolean hasNext() {
            return linksIter.hasNext();
        }

        @Override
        public HasChipLocation next() {
            return linksIter.next().destination;
        }

    }

}
