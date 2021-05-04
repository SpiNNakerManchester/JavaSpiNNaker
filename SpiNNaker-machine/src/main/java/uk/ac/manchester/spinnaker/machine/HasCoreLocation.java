/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
	int getP();

	/**
	 * Check if two locations are co-located at the core level. This does
	 * <i>not</i> imply that the two are equal.
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
