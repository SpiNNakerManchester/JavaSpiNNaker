/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.web;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.alloc.model.PowerState;

/**
 * Describes the current state of power of the machine (or at least the portion
 * of it allocated to a job), or a state that the user wants us to switch into.
 *
 * @author Donal Fellows
 */
@Immutable
public class MachinePower {
	@NotNull(message = "power must be specified")
	private final PowerState power;

	/**
	 * Make an instance.
	 *
	 * @param power
	 *            the machine power state
	 */
	public MachinePower(
			@JsonProperty(value = "power", defaultValue = "OFF")
			PowerState power) {
		this.power = power;
	}

	/** @return the machine power state */
	public PowerState getPower() {
		return power;
	}
}
