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
package uk.ac.manchester.spinnaker.machine;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The interface supported by any object that is associated with a chip.
 *
 * @author Donal Fellows
 */
public interface HasChipLocation {
	/**
	 * @return The X coordinate of the chip.
	 */
	@ValidX
	int getX();

	/**
	 * @return The X coordinate of the chip.
	 */
	@ValidY
	int getY();

	/**
	 * Check if two locations are colocated at the chip level. This does
	 * <i>not</i> imply that the two are equal.
	 *
	 * @param other
	 *            The other location to compare to.
	 * @return If the two locations have the same X and Y coordinates.
	 */
	default boolean onSameChipAs(HasChipLocation other) {
		return (getX() == other.getX()) && (getY() == other.getY());
	}

	/**
	 * Get the core of the chip that will be running SC&amp;MP. This is always
	 * core 0 of the chip, as the core that runs SC&amp;MP always maps itself to
	 * be virtual core ID 0.
	 *
	 * @return The location of the SC&amp;MP core.
	 */
	@JsonIgnore
	default HasCoreLocation getScampCore() {
		// SCAMP always runs on core 0 or we can't talk to the chip at all
		return new CoreLocation(getX(), getY(), 0);
	}

	/**
	 * Converts (if required) this to a simple X, Y tuple.
	 *
	 * @return A ChipLocation representation of the X and Y tuple
	 */
	default ChipLocation asChipLocation() {
		return new ChipLocation(getX(), getY());
	}
}
