/*
 * Copyright (c) 2022 The University of Manchester
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

/** Basic information about firmware. */
public enum FirmwareDescriptors {
	/** The main firmware copy. */
	Primary(0x10000, 256, 54, 53),
	/** The backup firmware copy. */
	Backup(0x20000, 256, 54, 53),
	/** The firmware used for booting. */
	Boot(0, 32, 6, 5);

	/** The location of the firmware descriptor in BMP memory. */
	public final MemoryLocation address;

	/**
	 * The amount of data to read to understand the firmware. The size of the
	 * firmware's header block.
	 */
	public final int blockSize;

	/** The index of the version in the header block. */
	public final int versionIndex;

	/** The index of the timestamp in the header block. */
	public final int timestampIndex;

	FirmwareDescriptors(int address, int blockSize, int versionIndex,
			int timestampIndex) {
		this.address = new MemoryLocation(address);
		this.blockSize = blockSize;
		this.versionIndex = versionIndex;
		this.timestampIndex = timestampIndex;
	}
}
