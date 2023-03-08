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
