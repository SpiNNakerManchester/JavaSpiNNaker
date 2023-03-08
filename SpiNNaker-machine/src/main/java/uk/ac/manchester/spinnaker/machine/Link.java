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
package uk.ac.manchester.spinnaker.machine;

import static java.util.Objects.hash;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Objects;

import javax.validation.Valid;

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
	@Valid
	public final ChipLocation source;

	/** The ID/Direction of the link in the source chip. */
	public final Direction sourceLinkDirection;

	/** The coordinate of the destination chip of the link. */
	@Valid
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
		return hash(source, sourceLinkDirection, destination);
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
		} else if (!(obj instanceof Link)) {
			return false;
		}
		var other = (Link) obj;
		log.trace("Equals called {} {}", this, other);
		if (sourceLinkDirection != other.sourceLinkDirection) {
			return false;
		}
		return Objects.equals(source, other.source)
				&& Objects.equals(destination, other.destination);
	}

	@Override
	public String toString() {
		return "Link{" + "source=" + source + ", source_link_id="
				+ sourceLinkDirection + ", destination=" + destination + '}';
	}
}
