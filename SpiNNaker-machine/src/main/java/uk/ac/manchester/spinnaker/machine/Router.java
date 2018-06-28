/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.Collection;
import java.util.EnumMap;

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

    public Router(Collection<Link> links, int clockSpeed,
            int nAvailableMulticastEntries) {
        for (Link link:links){
            if (this.links.containsKey(link.sourceLinkDirection)) {
                throw new IllegalArgumentException(
                        "Link already exists: " + link);
            }
            this.links.put(link.sourceLinkDirection, link);
        }
        this.clockSpeed = clockSpeed;
        this.nAvailableMulticastEntries = nAvailableMulticastEntries;
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
        return links.values();
    }
}
