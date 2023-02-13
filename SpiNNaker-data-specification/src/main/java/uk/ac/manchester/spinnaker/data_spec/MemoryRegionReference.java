/*
 * Copyright (c) 2021-2023 The University of Manchester
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

/**
 * A reference to another region.
 */
public final class MemoryRegionReference extends MemoryRegion {
	/** The index of the memory region. */
	private final int index;

	/** The reference of the region. */
	private final Reference reference;

	/**
	 * Create a reference to another region.
	 *
	 * @param index
	 *            The index of this region.
	 * @param reference
	 *            The reference to make.
	 */
	MemoryRegionReference(int index, Reference reference) {
		this.index = index;
		this.reference = requireNonNull(reference);
	}

	@Override
	public int getIndex() {
		return index;
	}

	/** @return The reference of the region. */
	public Reference getReference() {
		return reference;
	}

	@Override
	public String toString() {
		return "MemoryRegionReference(index:" + index + ", reference:"
			+ reference + ", Region Base:" + getRegionBase() + ")";
	}
}
