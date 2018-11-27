/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.boot;

import java.util.HashMap;
import java.util.Map;

/** Boot message operation codes. */
public enum BootOpCode {
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
	private static final Map<Integer, BootOpCode> MAP = new HashMap<>();

	BootOpCode(int value) {
		this.value = value;
	}

	static {
		for (BootOpCode c : values()) {
			MAP.put(c.value, c);
		}
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
