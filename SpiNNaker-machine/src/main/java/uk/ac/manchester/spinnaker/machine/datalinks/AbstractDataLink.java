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

import javax.validation.Valid;

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
// Should be sealed, but tests interfere
public abstract class AbstractDataLink implements HasChipLocation {
	/** IP address of the data link on the board. */
	public final InetAddress boardAddress;

	/** Coordinates of the location/chip being linked to. */
	@Valid
	public final ChipLocation location;

	/** Direction/ID for this link. */
	public final Direction direction;

	/**
	 * @param location
	 *            The location/chip being linked to
	 * @param linkId
	 *            The ID/direction coming out of the chip
	 * @param boardAddress
	 *            IP address of the data link on the board.
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
	 * Determines if the objects can be considered the same.
	 *
	 * @param other
	 *            Another data link
	 * @return True if and only if the IDs match, the {@link #boardAddress}
	 *         match and the links are on the same chip
	 */
	boolean sameAs(AbstractDataLink other) {
		return Objects.equals(direction, other.direction)
				&& Objects.equals(boardAddress, other.boardAddress)
				&& location.onSameChipAs(other.location);
	}
}
