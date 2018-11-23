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
package uk.ac.manchester.spinnaker.data_spec.exceptions;

import uk.ac.manchester.spinnaker.data_spec.Commands;

/**
 * An exception that indicates that a memory region is being used that was
 * originally requested to be unfilled.
 */
@SuppressWarnings("serial")
public class RegionUnfilledException extends DataSpecificationException {
	/**
	 * Create an instance.
	 *
	 * @param region
	 *            What region are we talking about.
	 * @param command
	 *            What command wanted to use the region.
	 */
	public RegionUnfilledException(int region, Commands command) {
		super("Region " + region + " was requested unfilled, but command "
				+ command + " requests its use");
	}
}
