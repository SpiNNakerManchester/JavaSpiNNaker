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
package uk.ac.manchester.spinnaker.data_spec.impl;

import uk.ac.manchester.spinnaker.data_spec.DataSpecificationException;

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
