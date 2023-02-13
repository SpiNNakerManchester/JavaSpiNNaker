/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.data_spec;

/**
 * An exception that indicates that a memory region is being used that was
 * originally requested to be unfilled.
 */
public class RegionUnfilledException extends DataSpecificationException {
	private static final long serialVersionUID = -3485312741873589073L;

	/**
	 * Create an instance.
	 *
	 * @param region
	 *            What region are we talking about.
	 * @param command
	 *            What command wanted to use the region.
	 */
	RegionUnfilledException(int region, Commands command) {
		super("Region " + region + " was requested unfilled, but command "
				+ command + " requests its use");
	}
}
