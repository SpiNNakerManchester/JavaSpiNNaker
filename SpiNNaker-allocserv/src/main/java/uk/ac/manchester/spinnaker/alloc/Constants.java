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

import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.NSEC_PER_SEC;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.NSEC_PER_USEC;

/**
 * Miscellaneous constants.
 *
 * @author Donal Fellows
 */
public interface Constants {
	/** Nanoseconds per microsecond. */
	double NS_PER_US = NSEC_PER_USEC;

	/** Nanoseconds per millisecond. */
	double NS_PER_MS = NSEC_PER_SEC / MSEC_PER_SEC;

	/** Nanoseconds per second. */
	double NS_PER_S = NSEC_PER_SEC;
}
