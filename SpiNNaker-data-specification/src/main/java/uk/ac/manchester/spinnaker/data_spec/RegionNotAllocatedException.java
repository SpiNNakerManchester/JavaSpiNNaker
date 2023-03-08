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
package uk.ac.manchester.spinnaker.data_spec;

/**
 * An exception which occurs when trying to write to an unallocated region of
 * memory.
 */
public class RegionNotAllocatedException extends DataSpecificationException {
	private static final long serialVersionUID = 7075946109066864639L;

	/**
	 * Create an instance.
	 *
	 * @param currentRegion
	 *            What is the current region.
	 * @param command
	 *            What command was trying to use the region.
	 */
	RegionNotAllocatedException(int currentRegion, Commands command) {
		super("Region " + currentRegion
				+ " has not been allocated during execution of command "
				+ command);
	}
}
