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
import static java.lang.String.format;

import com.google.errorprone.annotations.Immutable;

/**
 * A location in SpiNNaker or BMP memory. Does not say which address space this
 * is in.
 *
 * @author Donal Fellows
 */
@Immutable
public final class MemoryLocation implements Comparable<MemoryLocation> {
	/** Number of bytes in a SpiNNaker (ARM) word. */
	private static final int WORD_SIZE = 4;

	/** The zero memory location. Often means "no actual address". */
	public static final MemoryLocation NULL = new MemoryLocation(0);

	/** The actual location. */
	public final int address;

	/** @param address The actual location. */
	public MemoryLocation(int address) {
		this.address = address;
	}

	/** @param address The actual location. */
	public MemoryLocation(long address) {
		this.address = convert(address);
	}

	/**
	 * Add an offset to this location to get a new memory location.
	 *
	 * @param offset
	 *            The offset to add.
	 * @return The new memory location.
	 */
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

	/** @return Whether this location is really {@linkplain #NULL null}. */
	public boolean isNull() {
		return address == 0;
	}

	/**
	 * How many bytes is this location's address above a word-aligned address?
	 *
	 * @return The number of bytes offset.
	 */
	public int subWordAlignment() {
		return address % WORD_SIZE;
	}

	/** @return Whether this is a word-aligned location. */
	public boolean isAligned() {
		return subWordAlignment() == 0;
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
		var o = (MemoryLocation) other;
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
		// Leading '0x' then exactly 8 hexadecimal characters; 10 chars total
		return format("%#010x", address);
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
