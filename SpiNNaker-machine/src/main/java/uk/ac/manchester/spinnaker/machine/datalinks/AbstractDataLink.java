/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.datalinks;

import java.net.InetAddress;
import java.util.Objects;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MachineDefaults;

/**
 * Abstract Root of all Data Links.
 *
 * @author Christian-B
 */
public class AbstractDataLink implements HasChipLocation {

    /** IP address of the Datalink on the board. */
    public final InetAddress boardAddress;

    /** Coordinates of the location/Chip being linked to. */
    public final ChipLocation location;

    /** link Direction/id for this link. */
    public final Direction direction;


    /**
     * Main Constructor of a DataLink.
     *
     * @param location The location/Chip being linked to
     * @param linkId The id/Direction coming out of the Chip
     * @param boardAddress IP address of the Datalink on the board.
     */
    AbstractDataLink(HasChipLocation location, Direction linkId,
            InetAddress boardAddress) {
        if (location == null) {
            throw new IllegalArgumentException("location was null");
        }
        if (linkId == null) {
            throw new IllegalArgumentException("linkId was null");
        }
        this.location = location.asChipLocation();
        this.direction = linkId;
        this.boardAddress = boardAddress;
    }

    @Override
    public int getX() {
        return location.getX();
    }

    @Override
    public int getY() {
        return location.getY();
    }

    @Override
    public ChipLocation asChipLocation() {
        return location;
    }

    @Override
    public int hashCode() {
        int hash = 41 * (getX() << MachineDefaults.COORD_SHIFT) ^ getY();
        hash = 41 * hash + Objects.hashCode(this.boardAddress);
        hash = 41 * hash + this.direction.hashCode();
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
        final AbstractDataLink other = (AbstractDataLink) obj;
        return sameAs(other);
    }

    /**
     * Determines if the Objects can be considered the same.
     * <p>
     * Two links where their locations return onSameChipAs can be considered
     *      the same even if the location is a different class.
     * This is consistent with hashCode that only considers the location's
     *      X and Y
     * @param other Another DataLink
     * @return True if and only if the ids match, the boardAddress match and
     *      the links are on the same chip
     */
    boolean sameAs(AbstractDataLink other) {
        if (!this.direction.equals(other.direction)) {
            return false;
        }
        if (!Objects.equals(this.boardAddress, other.boardAddress)) {
            return false;
        }
        return location.onSameChipAs(other.location);
    }

}
