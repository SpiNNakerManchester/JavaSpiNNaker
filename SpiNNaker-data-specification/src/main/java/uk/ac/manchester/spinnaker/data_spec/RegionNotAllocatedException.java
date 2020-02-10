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
	public RegionNotAllocatedException(int currentRegion, Commands command) {
		super("Region " + currentRegion
            + " has not been allocated during execution of command "
            + command);
	}
}
