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

/**
 * A reference to another region.
 */
public final class MemoryRegionReference extends MemoryRegion {
	/** The index of the memory region. */
	private final int index;

	/** The reference of the region. */
	private final int reference;

	/** The base address of the actual region referenced. */
	private int baseAddress;

	/**
	 * Create a reference to another region.
	 *
	 * @param index
	 *            The index of this region.
	 * @param reference
	 *            The reference to make.
	 */
	MemoryRegionReference(int index, int reference) {
		this.index = index;
		this.reference = reference;
	}

	@Override
	public int getIndex() {
		return index;
	}

	/** @return The reference of the region. */
	public int getReference() {
		return reference;
	}

	@Override
	public void setRegionBase(int baseAddress) {
		this.baseAddress = baseAddress;
	}

	@Override
	public int getRegionBase() {
		return baseAddress;
	}
}
