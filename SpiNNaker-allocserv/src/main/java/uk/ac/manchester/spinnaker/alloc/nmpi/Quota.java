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

import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * A NMPI collab quota.
 */
public class Quota {

	/**
	 * The total amount of quota assigned.
	 */
	private double limit;

	/**
	 * The usage of the quota assigned.
	 */
	private double usage;

	/**
	 * The platform that the quota is assigned to.
	 */
	private String platform;

	/**
	 * The units in which the quota is measured.
	 */
	private String units;

	/**
	 * @return the limit.
	 */
	public double getLimit() {
		return limit;
	}

	/**
	 * @param limit
	 *            the limit to set.
	 */
	public void setLimit(double limit) {
		this.limit = limit;
	}

	/**
	 * @return the usage.
	 */
	public double getUsage() {
		return usage;
	}

	/**
	 * @param usage
	 *            the usage to set.
	 */
	public void setUsage(double usage) {
		this.usage = usage;
	}

	/**
	 * @return the platform.
	 */
	public String getPlatform() {
		return platform;
	}

	/**
	 * @param platform
	 *            the platform to set.
	 */
	public void setPlatform(String platform) {
		this.platform = platform;
	}

	/**
	 * @return the units.
	 */
	public String getUnits() {
		return units;
	}

	/**
	 * @param units
	 *            the units to set.
	 */
	public void setUnits(String units) {
		this.units = units;
	}

	/**
	 * Used for JSON serialisation; ignores other properties we don't care
	 * about.
	 *
	 * @param name
	 *            The parameter to set.
	 * @param value
	 *            The value to set it to.
	 */
	@JsonAnySetter
	void set(String name, Object value) {
		// Ignore any other values
	}
}
