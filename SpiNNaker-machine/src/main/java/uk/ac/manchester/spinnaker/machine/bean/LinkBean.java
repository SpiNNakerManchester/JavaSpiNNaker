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
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
 */
@UsedInJavadocOnly({Chip.class, Link.class})
public class LinkBean {
	/** Where the link is going. */
	@Valid
	public final ChipLocation destination;

	/** What direction the link is going in. */
	@NotNull
	public final Direction sourceDirection;

	/**
	 * Make an instance.
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
		sourceDirection = Direction.byId(sourceLinkId);
	}
}
