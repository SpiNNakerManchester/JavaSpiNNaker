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
import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.manchester.spinnaker.machine.Chip;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Used for converting JSON into a {@link Chip}.
 *
 * @author Christian-B
 */
@JsonFormat(shape = ARRAY)
@UsedInJavadocOnly(Chip.class)
public class ChipBean {
	/** The location of this Chip. */
	@Valid
	public final ChipLocation location;

	/** The details for this Chip. */
	@Valid
	public final ChipDetails details;

	/**
	 * The resources for this Chip.
	 *
	 * Not final as will be filled in with the defaults.
	 */
	@Valid
	private ChipResources resources;

	/**
	 * Main constructor with all values as properties.
	 *
	 * @param x
	 *            X coordinate of the Chip
	 * @param y
	 *            Y coordinate of the Chip.
	 * @param details
	 *            The details of what makes this chip special.
	 * @param resources
	 *            Any resources specifically declared for this Chip. May be
	 *            {@code null}.
	 */
	@JsonCreator
	public ChipBean(@JsonProperty(value = "x", required = true) int x,
			@JsonProperty(value = "y", required = true) int y,
			@JsonProperty(value = "details", required = true)
			ChipDetails details,
			@JsonProperty(value = "resources") ChipResources resources) {
		location = new ChipLocation(x, y);
		this.details = details;
		this.resources = resources;
	}

	/** @return Location of the chip. */
	public ChipLocation getLocation() {
		return location;
	}

	/**
	 * @return the resources
	 */
	public ChipResources getResources() {
		return resources;
	}

	/**
	 * @return the resources
	 */
	public ChipDetails getDetails() {
		return details;
	}

	@Override
	public String toString() {
		if (resources != null) {
			return location + ", " + details + " " + resources;
		} else {
			return location + ", " + details + " DEFAULTS";
		}
	}

	/**
	 * Adds the suitable default ChipResources.
	 *
	 * Based on if the Chip is an Ethernet one or not this will add the suitable
	 * resources. Any values specifically set for a Chip have preference over
	 * the default values.
	 *
	 * @param bean
	 *            Main bean to copy defaults from.
	 */
	public void addDefaults(MachineBean bean) {
		ChipResources defaults;
		if (details.getIpAddress() == null) {
			defaults = bean.getStandardResources();
		} else {
			defaults = bean.getEthernetResources();
		}
		if (resources == null) {
			resources = defaults;
		}
		resources.addDefaults(defaults);
	}
}
