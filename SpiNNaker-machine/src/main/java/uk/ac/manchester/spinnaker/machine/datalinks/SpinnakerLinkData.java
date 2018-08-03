/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.datalinks;

import java.net.InetAddress;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 *
 * @author Christian-B
 */
public class SpinnakerLinkData extends AbstractDataLink {

    /** The link id from the spinnaker prospective.  */
    public final int spinnakerLinkId;

    /**
     * Main Constructor of an FPGALinkData.
     *
     * @param spinnakerLinkId The link id from the spinnaker prospective.
     * @param location The location/Chip being linked to
     * @param linkId The id/Direction coming out of the Chip
     * @param boardAddress IP address of the Datalink on the board.
     */
    public SpinnakerLinkData(int spinnakerLinkId,
            HasChipLocation location, Direction linkId,
            InetAddress boardAddress) {
        super(location, linkId, boardAddress);
        this.spinnakerLinkId = spinnakerLinkId;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 53 * hash + this.spinnakerLinkId;
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
        final SpinnakerLinkData other = (SpinnakerLinkData) obj;
        if (sameAs(other)) {
            return this.spinnakerLinkId == other.spinnakerLinkId;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "SpinnakerLinkData {" + "X:" + getX() + " Y:" + getY()
                + ", boardAddress=" + boardAddress
                + ", linkId=" + direction
                + ", SpinnakerLinkId=" + spinnakerLinkId + "}";
    }

}
