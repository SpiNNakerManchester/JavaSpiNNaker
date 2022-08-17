/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc;

/**
 * Miscellaneous constants.
 *
 * @author Donal Fellows
 */
public interface Constants {
	/** The number of boards in a triad. */
	int TRIAD_DEPTH = 3;

	/** The width and height of a triad, in chips. */
	int TRIAD_CHIP_SIZE = 12;

	/** Nanoseconds per microsecond. */
	double NS_PER_US = 1000;

	/** Nanoseconds per millisecond. */
	double NS_PER_MS = 1000000;

	/** Nanoseconds per second. */
	double NS_PER_S = 1e9;
}
