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
package uk.ac.manchester.spinnaker.machine;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Objects;

import org.slf4j.Logger;

import com.google.errorprone.annotations.Immutable;

/**
 * Represents a directional link between SpiNNaker chips in the machine.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/link.py">
 *      Python Version</a>
 * @author Christian-B
 */
@Immutable
public final class Link {
	private static final Logger log = getLogger(Link.class);

	/** The coordinates of the source chip of the link. */
	public final ChipLocation source;

	/** The ID/Direction of the link in the source chip. */
	public final Direction sourceLinkDirection;

	/** The coordinate of the destination chip of the link. */
	public final ChipLocation destination;

	// Note: multicast_default_from and multicast_default_to not implemented

	/**
	 * Main Constructor which sets all parameters.
	 * <p>
	 * Specifically there is <em>no</em> check that the destination is the
	 * typical one for this source and direction pair.
	 *
	 * @param source
	 *            The coordinates of the source chip of the link.
	 * @param destination
	 *            The coordinate of the destination chip of the link.
	 * @param sourceLinkDirection
	 *            The Direction of the link in the source chip.
	 */
	public Link(ChipLocation source, Direction sourceLinkDirection,
			ChipLocation destination) {
		this.source = source;
		this.sourceLinkDirection = sourceLinkDirection;
		this.destination = destination;
	}

	/**
	 * Main Constructor which sets all parameters.
	 * <p>
	 * Specifically there is <em>no</em> check that the destination is the
	 * typical one for this source and direction pair.
	 *
	 * @param source
	 *            The coordinates of the source chip of the link.
	 * @param destination
	 *            The coordinate of the destination chip of the link.
	 * @param sourceLinkDirection
	 *            The Direction of the link in the source chip.
	 */
	public Link(HasChipLocation source, Direction sourceLinkDirection,
			HasChipLocation destination) {
		this(source.asChipLocation(), sourceLinkDirection,
				destination.asChipLocation());
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 47 * hash + Objects.hashCode(source);
		hash = 47 * hash + Objects.hashCode(sourceLinkDirection);
		hash = 47 * hash + Objects.hashCode(destination);
		return hash;
	}

	/**
	 * @param source
	 *            The coordinates of the source chip of the link.
	 * @param destination
	 *            The coordinate of the destination chip of the link.
	 * @param sourceLinkId
	 *            The ID of the link in the source chip.
	 */
	public Link(HasChipLocation source, int sourceLinkId,
			HasChipLocation destination) {
		this(source, Direction.byId(sourceLinkId), destination);
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
		var other = (Link) obj;
		log.trace("Equals called {} {}", this, other);
		if (sourceLinkDirection != other.sourceLinkDirection) {
			return false;
		}
		if (!Objects.equals(source, other.source)) {
			return false;
		}
		return Objects.equals(destination, other.destination);
	}

	@Override
	public String toString() {
		return "Link{" + "source=" + source + ", source_link_id="
				+ sourceLinkDirection + ", destination=" + destination + '}';
	}
}
