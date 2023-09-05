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
package uk.ac.manchester.spinnaker.machine;

/**
 * The interface supported by any object that is associated with a core.
 *
 * @author dkf
 */
public interface HasCoreLocation extends HasChipLocation {
	/**
	 * @return The processor coordinate of the core on its chip.
	 */
	@ValidP
	int getP();

	/**
	 * Check if two locations are co-located at the core level. This does
	 * <i>not</i> imply that the two are equal (but does imply that the results
	 * of calling {@link #asCoreLocation()} on each will produce equal objects).
	 *
	 * @param other
	 *            The other location to compare to.
	 * @return If the two locations have the same X, Y and P coordinates.
	 */
	default boolean onSameCoreAs(HasCoreLocation other) {
		return onSameChipAs(other) && (getP() == other.getP());
	}

	/**
	 * Converts (if required) this to a simple X, Y, P tuple.
	 *
	 * @return A CoreLocation representation of the X, Y, P tuple
	 */
	default CoreLocation asCoreLocation() {
		return new CoreLocation(getX(), getY(), getP());
	}
}
