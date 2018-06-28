/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.Objects;

/**
 * Represents a directional link between SpiNNaker chips in the machine.
 * <p>
 * Traffic received on the link identified by multicast_default_from will
 * be sent to the link herein defined if no entry is present in the
 * multicast routing table.
 * On SpiNNaker chips, multicast_default_from is usually the same as
 * multicast_default_to. None if no such default exists, or the link does
 * not exist.
 * <p>
 * @see <a
 * href="https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/link.py">
 * Python Version</a>
 *
 * @author Christian-B
 */
public final class Link {

    /** The coordinates of the source chip of the link. */
    public final HasChipLocation source;

    /** The ID of the link in the source chip */
    public final int source_link_id;

    /** The coordinate of the destination chip of the link. */
    public final HasChipLocation destination;

    /**
     * Default from direction.
     */
    public final Direction multicastDefaultFrom;

    /**
     * Default to direction.
     */
    public final Direction multicastDefaultTo;

    /**
     *
     * @param source The coordinates of the source chip of the link.
     * @param destination The coordinate of the destination chip of the link.
     * @param source_link_id The ID of the link in the source chip.
     * @param multicastDefaultFrom Default from direction.
     * @param multicastDefaultTo Default to direction.
     */
    public Link(HasChipLocation source, int source_link_id,
            HasChipLocation destination,
            Direction multicastDefaultFrom, Direction multicastDefaultTo) {
        this.source = source;
        this.source_link_id = source_link_id;
        this.destination = destination;
        this.multicastDefaultFrom = multicastDefaultFrom;
        this.multicastDefaultTo = multicastDefaultTo;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + this.source.hashCode();
        hash = 53 * hash + this.source_link_id;
        hash = 53 * hash + Objects.hashCode(this.destination);
        hash = 53 * hash + Objects.hashCode(this.multicastDefaultFrom);
        hash = 53 * hash + Objects.hashCode(this.multicastDefaultTo);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Link other = (Link) obj;
        if (this.source_link_id != other.source_link_id) {
            return false;
        }
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }
        if (!Objects.equals(this.destination, other.destination)) {
            return false;
        }
        if (this.multicastDefaultFrom != other.multicastDefaultFrom) {
            return false;
        }
        if (this.multicastDefaultTo != other.multicastDefaultTo) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Link{" + "source=" + source + ", source_link_id="
                + source_link_id + ", destination=" + destination
                + ", multicastDefaultFrom=" + multicastDefaultFrom
                + ", multicastDefaultTo=" + multicastDefaultTo + '}';
    }

}
