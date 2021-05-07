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
 * The interface supported by any object that is associated with a chip.
 *
 * @author Donal Fellows
 */
public interface HasChipLocation {
	/**
	 * @return The X coordinate of the chip.
	 */
	int getX();

	/**
	 * @return The X coordinate of the chip.
	 */
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
