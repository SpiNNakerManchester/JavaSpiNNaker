/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.boot;

import static uk.ac.manchester.spinnaker.utils.CollectionUtils.makeEnumBackingMap;

import java.util.Map;

/** Boot message operation codes. */
enum BootOpCode {
	/** Sent by SpiNNaker to announce itself ready for booting. */
	HELLO(0x41),
	/** Start a fill of the boot image (i.e., SCAMP). */
	FLOOD_FILL_START(0x1),
	/** Message contains a block from the boot image. */
	FLOOD_FILL_BLOCK(0x3),
	/** Finish a fill of the boot image (i.e., SCAMP). */
	FLOOD_FILL_CONTROL(0x5);

	/** The encoded form of the opcode. */
	public final int value;

	private static final Map<Integer, BootOpCode> MAP =
			makeEnumBackingMap(values(), v -> v.value);

	BootOpCode(int value) {
		this.value = value;
	}

	/**
	 * @param opcode
	 *            The opcode to convert.
	 * @return The converted opcode, or {@code null} if it was unrecognised.
	 */
	public static BootOpCode get(int opcode) {
		return MAP.get(opcode);
	}
}
