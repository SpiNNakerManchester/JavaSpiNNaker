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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;

/**
 *
 * @author Christian-B
 */
public class LinkBean {
	/** Where the link is going. */
	public final ChipLocation destination;

	/** What direction the link is going in. */
	public final Direction sourceDirection;

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
		destination = new ChipLocation(destinationX, destinationY);
		this.sourceDirection = Direction.byId(sourceLinkId);
	}
}
