/*
 * Copyright (c) 2018 The University of Manchester
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
 * An exception that indicates that there is no more space for the requested
 * item.
 */
public class NoMoreException extends DataSpecificationException {
	private static final long serialVersionUID = 1924179276762267554L;

	/**
	 * Create an instance.
	 *
	 * @param remainingSpace
	 *            How much space is available
	 * @param length
	 *            How much space was asked for
	 * @param currentRegion
	 *            What region are we talking about
	 */
	NoMoreException(int remainingSpace, int length, int currentRegion) {
		super("Space unavailable to write all the elements requested by the "
				+ "write operation. Space available: " + remainingSpace
				+ "; space requested: " + length + " for region "
				+ currentRegion + ".");
	}
}
