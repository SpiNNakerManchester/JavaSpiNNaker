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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.google.errorprone.annotations.Immutable;

/**
 * Represents a directional link between SpiNNaker chips in the machine.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/link.py">
 *      Python Version</a>
 * @author Christian-B
 * @param source
 *            The coordinates of the source chip of the link.
 * @param sourceLinkDirection
 *            The direction of the link in the source chip.
 * @param destination
 *            The coordinate of the destination chip of the link.
 */
@Immutable
public record Link(@Valid ChipLocation source,
		@NotNull Direction sourceLinkDirection,
		@Valid ChipLocation destination) {
	// Note: multicast_default_from and multicast_default_to not implemented
	/**
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
	public String toString() {
		return "Link{" + "source=" + source + ", source_link_id="
				+ sourceLinkDirection + ", destination=" + destination + '}';
	}
}
