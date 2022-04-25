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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.time.Instant;

/** A firmware descriptor. */
public class FirmwareDescriptor {
	private static final int MAX_OLD_STYLE = 65535;

	private static final int OLD_SPLIT = 100;

	private static final int BYTE_MASK = 255;

	private static final int BYTE_SHIFT = 8;

	private final FirmwareDescriptors type;

	private final IntBuffer descriptorData;

	public FirmwareDescriptor(FirmwareDescriptors type, ByteBuffer buffer) {
		this.type = type;
		descriptorData = buffer.asIntBuffer().asReadOnlyBuffer();
	}

	public Version getVersion() {
		int n = descriptorData.get(type.versionIndex);

		if (n <= MAX_OLD_STYLE) {
			int major = n / OLD_SPLIT;
			int minor = n % OLD_SPLIT;

			return new Version(major, minor, 0);
		} else {
			int major = n >> BYTE_SHIFT >> BYTE_SHIFT;
			int minor = (n >> BYTE_SHIFT) & BYTE_MASK;
			int rev = n & BYTE_MASK;

			return new Version(major, minor, rev);
		}
	}

	public Instant getTimestamp() {
		return Instant.ofEpochSecond(descriptorData.get(type.timestampIndex));
	}
}
