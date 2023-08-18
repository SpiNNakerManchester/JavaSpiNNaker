/*
 * Copyright (c) 2014 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.machinemanager.responses;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Describes a connection by its chip and hostname.
 */
@JsonPropertyOrder({"chip", "hostname"})
@JsonFormat(shape = ARRAY)
public class Connection {

	/**
	 * The chip connected to.
	 */
	private Chip chip;

	/**
	 * The host name connected to.
	 */
	private String hostname;

	/**
	 * Get the chip connected to.
	 *
	 * @return The chip
	 */
	public Chip getChip() {
		return chip;
	}

	/**
	 * Set the chip connected to.
	 *
	 * @param chipParam The chip to set
	 */
	public void setChip(final Chip chipParam) {
		this.chip = chipParam;
	}

	/**
	 * Get the host name connected to.
	 *
	 * @return The host name
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * Set the host name connected to.
	 *
	 * @param hostnameParam The host name to set
	 */
	public void setHostname(final String hostnameParam) {
		this.hostname = hostnameParam;
	}
}
