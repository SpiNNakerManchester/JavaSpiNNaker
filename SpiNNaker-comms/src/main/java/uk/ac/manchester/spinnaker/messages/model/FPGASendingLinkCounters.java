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
 * FPGA link send counters. Counters are always read-only.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/spio/tree/master/designs/spinnaker_fpgas">
 *      Spio documentation</a>
 * @author Donal Fellows
 */
@SARKStruct("spio")
public enum FPGASendingLinkCounters {
	/** Packets sent on link. */
	PSTL(0),
	/** Unexpected acknowledges on link. */
	ACKE(16),
	/** Timeouts on link. */
	TMOE(32);

	private static final int BASE_ADDRESS = 0x00050000;

	private static final int COUNTER_SIZE = 4;

	private final int offset;

	FPGASendingLinkCounters(int offset) {
		this.offset = offset;
	}

	/**
	 * Get the address of the counter for a particular link.
	 *
	 * @param linkNumber
	 *            The link number.
	 * @return The address (in FPGA space).
	 */
	public int address(int linkNumber) {
		return BASE_ADDRESS + offset + linkNumber * COUNTER_SIZE;
	}
}
