/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.model;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;

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

	private static final MemoryLocation BASE_ADDRESS =
			new MemoryLocation(0x00060000);

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
	public MemoryLocation address(int linkNumber) {
		if (linkNumber < 0 || linkNumber > 2) {
			throw new IllegalArgumentException("linkNumber must be 0, 1, or 2");
		}
		return BASE_ADDRESS.add(offset + linkNumber * COUNTER_SIZE);
	}
}
