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

import static java.util.Objects.isNull;

import java.net.InetAddress;
import java.util.Objects;

import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * Abstract Root of all Data Links.
 *
 * @author Christian-B
 */
@Immutable
public abstract class AbstractDataLink implements HasChipLocation {
	/** IP address of the Datalink on the board. */
	public final InetAddress boardAddress;

	/** Coordinates of the location/Chip being linked to. */
	public final ChipLocation location;

	/** Direction/id for this link. */
	public final Direction direction;

	/**
	 * Main Constructor of a DataLink.
	 *
	 * @param location
	 *            The location/Chip being linked to
	 * @param linkId
	 *            The ID/Direction coming out of the Chip
	 * @param boardAddress
	 *            IP address of the Datalink on the board.
	 */
	AbstractDataLink(HasChipLocation location, Direction linkId,
			InetAddress boardAddress) {
		if (isNull(location)) {
			throw new IllegalArgumentException("location was null");
		}
		if (isNull(linkId)) {
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
	public abstract int hashCode();

	/** {@inheritDoc} */
	@Override
	public abstract boolean equals(Object obj);

	/**
	 * Determines if the Objects can be considered the same.
	 * <p>
	 * Two links where their locations return onSameChipAs can be considered the
	 * same even if the location is a different class. This is consistent with
	 * hashCode that only considers the location's X and Y
	 *
	 * @param other
	 *            Another DataLink
	 * @return True if and only if the IDs match, the boardAddress match and the
	 *         links are on the same chip
	 */
	boolean sameAs(AbstractDataLink other) {
		return Objects.equals(direction, other.direction)
				&& Objects.equals(boardAddress, other.boardAddress)
				&& location.onSameChipAs(other.location);
	}
}
