/*
 * Copyright (c) 2024 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.download.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/**
 * A non-recording region to be downloaded.
 */
@JsonFormat(shape = Shape.OBJECT)
public class DownloadRegion {

	private final int index;

	private final MemoryLocation address;

	private final int size;

	public DownloadRegion() {
		this.index = 0;
		this.address = new MemoryLocation(0);
		this.size = 0;
	}

	/**
	 * Constructs a new download region.
	 *
	 * @param index
	 *           The index of the region.
	 * @param address
	 *            The address of the region.
	 * @param size
	 *            The size of the region.
	 */
	public DownloadRegion(
			@JsonProperty(value = "index", required = true) int index,
			@JsonProperty(value = "address", required = true) long address,
			@JsonProperty(value = "size", required = true) int size) {
		this.index = index;
		this.address = new MemoryLocation(address);
		this.size = size;
	}

	/**
	 * @return The index of the region.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @return The address of the region.
	 */
	public MemoryLocation getAddress() {
		return address;
	}

	/**
	 * @return The size of the region.
	 */
	public int getSize() {
		return size;
	}
}
