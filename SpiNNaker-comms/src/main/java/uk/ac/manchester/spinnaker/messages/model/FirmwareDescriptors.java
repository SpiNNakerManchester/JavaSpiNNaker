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
