/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.data_spec;

import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.machine.MemoryLocation.NULL;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/**
 * Marker of something that is a memory region. All memory regions have an
 * address in memory where they start (which is only chosen late in the region
 * creation process) and an index in the process's table of regions.
 */
public abstract sealed class MemoryRegion
		permits MemoryRegionReal, MemoryRegionReference {
	/** The base address of the region. Set after the fact. */
	private MemoryLocation baseAddress = NULL;

	/** @return the index of the memory region. */
	public abstract int getIndex();

	/**
	 * Get the address of the first byte in the region.
	 *
	 * @return The address. Never {@code null}, but may be
	 *         {@link MemoryLocation#NULL NULL} if not yet otherwise set.
	 */
	public final MemoryLocation getRegionBase() {
		return baseAddress;
	}

	/**
	 * Set the address of the first byte in the region.
	 *
	 * @param baseAddress
	 *            The address to set. Must not be {@code null}.
	 */
	public final void setRegionBase(MemoryLocation baseAddress) {
		this.baseAddress =
				requireNonNull(baseAddress, "base address must not be null");
	}
}
