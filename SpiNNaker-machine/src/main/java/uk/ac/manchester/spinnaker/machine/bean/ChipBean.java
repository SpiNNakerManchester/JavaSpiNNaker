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

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
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
		if (nonNull(resources)) {
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
		if (isNull(details.getIpAddress())) {
			defaults = bean.getStandardResources();
		} else {
			defaults = bean.getEthernetResources();
		}
		if (isNull(resources)) {
			resources = defaults;
		}
		resources.addDefaults(defaults);
	}
}
