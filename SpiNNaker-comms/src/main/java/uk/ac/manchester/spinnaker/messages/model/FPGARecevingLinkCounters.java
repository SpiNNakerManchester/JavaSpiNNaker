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
package uk.ac.manchester.spinnaker.messages.model;

/**
 * FPGA link receive counters. Counters are always read-only.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/spio/tree/master/designs/spinnaker_fpgas">
 *      Spio documentation</a>
 * @author Donal Fellows
 */
@SARKStruct("spio")
public enum FPGARecevingLinkCounters {
	/** Packets received on link. */
	PRVL(0),
	/** Incorrect 2-of-7 flits received on link. */
	FLTE(16),
	/** Framing errors on link. */
	FRME(32),
	/** Glitches detected on link. */
	GLTE(48);

	private static final int BASE_ADDRESS = 0x00060000;

	private static final int COUNTER_SIZE = 4;

	private final int offset;

	FPGARecevingLinkCounters(int offset) {
		this.offset = offset;
	}

	/**
	 * Get the address of the counter for a particular link.
	 *
	 * @param linkNumber
	 *            The link number. Must be 0, 1, or 2.
	 * @return The address (in FPGA space).
	 * @throws IllegalArgumentException
	 *             if a bad link number is given.
	 */
	public int address(int linkNumber) {
		if (linkNumber < 0 || linkNumber > 2) {
			throw new IllegalArgumentException("linkNumber must be 0, 1, or 2");
		}
		return BASE_ADDRESS + offset + linkNumber * COUNTER_SIZE;
	}
}
