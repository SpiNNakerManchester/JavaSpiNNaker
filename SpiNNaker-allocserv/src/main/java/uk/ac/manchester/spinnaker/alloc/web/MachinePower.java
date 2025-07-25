/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.web;

import jakarta.validation.constraints.NotNull;

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
