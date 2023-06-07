/*
 * Copyright (c) 2023 The University of Manchester
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

package uk.ac.manchester.spinnaker.alloc.nmpi;

/**
 * An NMPI Resource Usage object.
 */
public class ResourceUsage {

	public static final String CORE_HOURS = "core-hours";

	private double value;

	private String units;

	/**
	 * Get the value of the usage.
	 * @return The value.
	 */
	public double getValue() {
		return value;
	}

	/**
	 * Set the value of the usage.
	 * @param value The value to set.
	 */
	public void setValue(double value) {
		this.value = value;
	}

	/**
	 * Get the units of the usage.
	 * @return The units.
	 */
	public String getUnits() {
		return units;
	}

	/**
	 * Set the units of the usage.
	 * @param units The units to set.
	 */
	public void setUnits(String units) {
		this.units = units;
	}
}
