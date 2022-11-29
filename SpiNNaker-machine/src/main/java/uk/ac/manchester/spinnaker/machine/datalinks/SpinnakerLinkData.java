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
import java.util.Objects;

import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * A description of a data link that uses the SpiNNaker-link protocol.
 *
 * @author Christian-B
 */
@Immutable
public final class SpinnakerLinkData extends AbstractDataLink {

	/** The link ID from the SpiNNaker perspective. */
	public final int spinnakerLinkId;

	/**
	 * Build an instance.
	 *
	 * @param spinnakerLinkId
	 *            The link ID from the SpiNNaker perspective.
	 * @param location
	 *            The location/Chip being linked to
	 * @param linkId
	 *            The ID/Direction coming out of the Chip
	 * @param boardAddress
	 *            IP address of the Datalink on the board.
	 */
	public SpinnakerLinkData(int spinnakerLinkId, HasChipLocation location,
			Direction linkId, InetAddress boardAddress) {
		super(location, linkId, boardAddress);
		this.spinnakerLinkId = spinnakerLinkId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(location, boardAddress, direction, spinnakerLinkId);
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof SpinnakerLinkData other) && sameAs(other)
				&& (spinnakerLinkId == other.spinnakerLinkId);
	}

	@Override
	public String toString() {
		return "SpinnakerLinkData {" + "X:" + getX() + " Y:" + getY()
				+ ", boardAddress=" + boardAddress + ", linkId=" + direction
				+ ", SpinnakerLinkId=" + spinnakerLinkId + "}";
	}
}
