/*
 * Copyright (c) 2021 The University of Manchester
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

import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/**
 * Marker of something that is a memory region.
 */
public abstract class MemoryRegion {

	/** @return the index of the memory region. */
	public abstract int getIndex();

	/**
	 * Get the address of the first byte in the region.
	 *
	 * @return The address.
	 */
	public abstract MemoryLocation getRegionBase();

	/**
	 * Set the address of the first byte in the region.
	 *
	 * @param baseAddress
	 *            The address to set.
	 */
	protected abstract void setRegionBase(MemoryLocation baseAddress);
}
