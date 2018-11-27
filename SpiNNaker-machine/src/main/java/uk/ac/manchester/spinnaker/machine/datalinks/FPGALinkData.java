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
package uk.ac.manchester.spinnaker.machine.datalinks;

import java.net.InetAddress;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 *
 * @author Christian-B
 */
public final class FPGALinkData extends AbstractDataLink {

    /** The link id from the fdga prospective.  */
    public final int fpgaLinkId;

    /** The id if the fpga port being used. */
    public final FpgaId fpgaId;

    /**
     * Main Constructor of an FPGALinkData.
     *
     * @param fpgaLinkId The link id from the fdga prospective.
     * @param fpgaId The id if the fpga port being used.
     * @param location The location/Chip being linked to
     * @param linkId The id/Direction comeing out of the Chip
     * @param boardAddress IP address of the Datalink on the board.
     */
    public FPGALinkData(int fpgaLinkId, FpgaId fpgaId,
            HasChipLocation location, Direction linkId,
            InetAddress boardAddress) {
        super(location, linkId, boardAddress);
        this.fpgaLinkId = fpgaLinkId;
        this.fpgaId = fpgaId;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 53 * hash + this.fpgaLinkId;
        hash = 53 * hash + this.fpgaId.id;
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
        final FPGALinkData other = (FPGALinkData) obj;
        if (sameAs(other)) {
            if (this.fpgaLinkId != other.fpgaLinkId) {
                return false;
            }
            return this.fpgaId == other.fpgaId;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "FPGALinkData {" + "X:" + getX() + " Y:" + getY()
                + ", boardAddress=" + boardAddress
                + ", linkId=" + direction + ", fpgaLinkId=" + fpgaLinkId
                + ", fpgaId=" + fpgaId + "}";
    }

}
