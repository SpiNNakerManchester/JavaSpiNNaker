/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.Objects;

/**
 * Represents a directional link between SpiNNaker chips in the machine.
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

    /** The ID/Direction of the link in the source chip. */
    public final Direction sourceLinkDirection;

    /** The coordinate of the destination chip of the link. */
    public final HasChipLocation destination;

    //Note: multicast_default_from and multicast_default_to not implemented

    /**
     * Main Constructor which sets all parameters.
     * <p>
     * Specifically there is NO check that the destination is the typical one
     *    for this source and direction pair.
     *
     * @param source The coordinates of the source chip of the link.
     * @param destination The coordinate of the destination chip of the link.
     * @param sourceLinkDirection The Direction of the link in the source chip.
     */
    public Link(HasChipLocation source, Direction sourceLinkDirection,
            HasChipLocation destination) {
        this.source = source;
        this.sourceLinkDirection = sourceLinkDirection;
        this.destination = destination;
    }

    /**
     * Pass through constructor that sets the destination as the typical one.
     * <p>
     * @see
     * Direction#typicalMove(uk.ac.manchester.spinnaker.machine.HasChipLocation)
     *
     * @param source The coordinates of the source chip of the link.
     * @param sourceLinkDirection The Direction of the link in the source chip.
     */
    @SuppressWarnings("deprecation")
    public Link(HasChipLocation source, Direction sourceLinkDirection) {
        this (source, sourceLinkDirection,
                sourceLinkDirection.typicalMove(source));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.source);
        hash = 47 * hash + Objects.hashCode(this.sourceLinkDirection);
        hash = 47 * hash + Objects.hashCode(this.destination);
        return hash;
    }

    /**
     *
     * @param source The coordinates of the source chip of the link.
     * @param destination The coordinate of the destination chip of the link.
     * @param sourceLinkId The ID of the link in the source chip.
     */
    public Link(HasChipLocation source, int sourceLinkId,
            HasChipLocation destination) {
        this (source, Direction.byId(sourceLinkId), destination);
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
        System.out.println("Equals called " + this + " " + other);
        if (this.sourceLinkDirection != other.sourceLinkDirection) {
            System.out.println("direction");
            return false;
        }
        if (!Objects.equals(this.source, other.source)) {
            System.out.println("source");
            return false;
        }
        if (!Objects.equals(this.destination, other.destination)) {
            System.out.println("destination");
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Link{" + "source=" + source + ", source_link_id="
                + sourceLinkDirection + ", destination=" + destination + '}';
    }

}
