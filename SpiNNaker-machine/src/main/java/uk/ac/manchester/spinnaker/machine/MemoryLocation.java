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
package uk.ac.manchester.spinnaker.machine;

import static java.lang.Integer.compareUnsigned;

/**
 * A location in SpiNNaker or BMP memory. Does not say which address space this
 * is in.
 *
 * @author Donal Fellows
 */
public final class MemoryLocation implements Comparable<MemoryLocation> {
	/** The zero memory location. Often means "no actual address". */
	public static final MemoryLocation NULL = new MemoryLocation(0);

	/** The actual location. */
	public final int address;

	public MemoryLocation(int address) {
		this.address = address;
	}

	public MemoryLocation(long address) {
		this.address = convert(address);
	}

	public MemoryLocation add(int offset) {
		return new MemoryLocation(address + offset);
	}

	/**
	 * Get the difference between this location and another.
	 *
	 * @param other
	 *            The other location.
	 * @return This location's address minus the other location's address.
	 */
	public int diff(MemoryLocation other) {
		return address - other.address;
	}

	public boolean isNull() {
		return address == 0;
	}

	@Override
	public int hashCode() {
		return address;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof MemoryLocation)) {
			return false;
		}
		MemoryLocation o = (MemoryLocation) other;
		return o.address == address;
	}

	@Override
	public int compareTo(MemoryLocation other) {
		return compareUnsigned(address, other.address);
	}

	/**
	 * Test if this location is less than another location.
	 *
	 * @param other
	 *            The other location.
	 * @return True if this location's address comes before.
	 */
	public boolean lessThan(MemoryLocation other) {
		return compareTo(other) < 0;
	}

	/**
	 * Test if this location is greater than another location.
	 *
	 * @param other
	 *            The other location.
	 * @return True if this location's address comes after.
	 */
	public boolean greaterThan(MemoryLocation other) {
		return compareTo(other) > 0;
	}

	@Override
	public String toString() {
		return "0x" + Integer.toHexString(address);
	}

	/** Maximum legal SpiNNaker address. It's a 32-bit machine. */
	private static final long MAX_ADDR = 0xFFFFFFFFL;

	/**
	 * Convert an address to a 32-bit integer.
	 *
	 * @param baseAddress
	 *            The address as a long.
	 * @return the address as an int.
	 * @throws IllegalArgumentException
	 *             if the value is outside the unsigned 32-bit integer range.
	 */
	private static int convert(long baseAddress) {
		if (baseAddress < 0 || baseAddress > MAX_ADDR) {
			throw new IllegalArgumentException(
					"address must be in 32-bit unsigned integer range");
		}
		return (int) baseAddress;
	}
}
