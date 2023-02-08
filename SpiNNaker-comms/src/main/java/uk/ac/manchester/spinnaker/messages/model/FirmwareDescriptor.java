/*
 * Copyright (c) 2022 The University of Manchester
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

	/**
	 * Create a description of some firmware on a SpiNNaker system.
	 *
	 * @param type
	 *            What type of firmware is this.
	 * @param buffer
	 *            What was the descriptor buffer read from the system?
	 */
	public FirmwareDescriptor(FirmwareDescriptors type, ByteBuffer buffer) {
		this.type = type;
		descriptorData = buffer.asIntBuffer().asReadOnlyBuffer();
	}

	/** @return The version of the firmware. */
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

	/** @return The timestamp in the firmware. */
	public Instant getTimestamp() {
		return Instant.ofEpochSecond(descriptorData.get(type.timestampIndex));
	}
}
