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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * A NMPI project.
 */
public class Project {

	/**
	 * The quotas of the project.
	 */
	private List<Quota> quotas;

	/**
	 * @return the quotas.
	 */
	public List<Quota> getQuotas() {
		return quotas;
	}

	/**
	 * @param quotas the quotas to set.
	 */
	public void setQuotas(List<Quota> quotas) {
		this.quotas = quotas;
	}

	/**
	 * Used for JSON serialisation;
	 * ignores other properties we don't care about.
	 *
	 * @param name
	 *            The parameter to set.
	 * @param value
	 *            The value to set it to.
	 */
	@JsonAnySetter
	public void set(final String name, final Object value) {
		// Ignore any other values
	}
}
