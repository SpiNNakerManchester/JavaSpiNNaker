/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.machine.datalinks;

import java.net.InetAddress;
import java.util.Objects;

import com.google.errorprone.annotations.Immutable;

import jakarta.validation.Valid;
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
	@Valid
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
