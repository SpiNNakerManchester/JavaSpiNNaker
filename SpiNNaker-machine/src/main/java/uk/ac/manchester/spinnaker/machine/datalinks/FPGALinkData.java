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

import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 *
 * @author Christian-B
 */
@Immutable
public final class FPGALinkData extends AbstractDataLink {

	/** The link ID from the FPGA prospective. */
	public final int fpgaLinkId;

	/** The ID of the FPGA port being used. */
	public final FpgaId fpgaId;

	/**
	 * Main Constructor of an FPGALinkData.
	 *
	 * @param fpgaLinkId
	 *            The link ID from the FPGA prospective.
	 * @param fpgaId
	 *            The ID if the FPGA port being used.
	 * @param location
	 *            The location/Chip being linked to
	 * @param linkId
	 *            The ID/Direction coming out of the Chip
	 * @param boardAddress
	 *            IP address of the Datalink on the board.
	 */
	public FPGALinkData(int fpgaLinkId, FpgaId fpgaId, HasChipLocation location,
			Direction linkId, InetAddress boardAddress) {
		super(location, linkId, boardAddress);
		this.fpgaLinkId = fpgaLinkId;
		this.fpgaId = fpgaId;
	}

	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = 53 * hash + fpgaLinkId;
		hash = 53 * hash + fpgaId.id;
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
		var other = (FPGALinkData) obj;
		if (sameAs(other)) {
			if (fpgaLinkId != other.fpgaLinkId) {
				return false;
			}
			return fpgaId == other.fpgaId;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return "FPGALinkData {" + "X:" + getX() + " Y:" + getY()
				+ ", boardAddress=" + boardAddress + ", linkId=" + direction
				+ ", fpgaLinkId=" + fpgaLinkId + ", fpgaId=" + fpgaId + "}";
	}

}
