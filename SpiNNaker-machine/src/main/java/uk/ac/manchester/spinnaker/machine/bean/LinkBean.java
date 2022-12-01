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
package uk.ac.manchester.spinnaker.machine.bean;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.manchester.spinnaker.machine.Chip;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.Link;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * A {@linkplain Link link} between two {@linkplain Chip chips}. At this point
 * of the JSON model, the chip coordinates of the source are implicit.
 *
 * @author Christian-B
 * @param destination
 *            Where the link is going.
 * @param sourceDirection
 *            What direction the link is going in.
 */
@UsedInJavadocOnly({ Chip.class, Link.class })
public record LinkBean(@Valid ChipLocation destination,
		@NotNull Direction sourceDirection) {
	/**
	 * Create a link description.
	 *
	 * @param sourceLinkId
	 *            What is the direction of the link?
	 * @param destinationX
	 *            Where is the link going to? X coordinate.
	 * @param destinationY
	 *            Where is the link going to? Y coordinate.
	 */
	@JsonCreator
	public LinkBean(
			@JsonProperty(value = "sourceLinkId", required = true)
			int sourceLinkId,
			@JsonProperty(value = "destinationX", required = true)
			int destinationX,
			@JsonProperty(value = "destinationY", required = true)
			int destinationY) {
		this(new ChipLocation(destinationX, destinationY),
				Direction.byId(sourceLinkId));
	}
}
