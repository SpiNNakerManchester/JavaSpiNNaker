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
package uk.ac.manchester.spinnaker.messages.scp;

/** Constants for working with tags. */
abstract class IPTagFieldDefinitions {
	private IPTagFieldDefinitions() {
	}

	/** The index of the reverse flag bit in argument 1. */
	static final int REVERSE_FIELD_BIT = 29;
	/** The index of the strip SDP flag bit in argument 1. */
	static final int STRIP_FIELD_BIT = 28;
	/** The index of the reverse flag bit in argument 1. */
	static final int COMMAND_FIELD = 16;
	/** The index of the command field in argument 1. */
	static final int SDP_PORT_FIELD = 13;
	/** The index of the SDP port field in argument 1. */
	static final int DEST_P_FIELD = 8;
	/** Bottom three bits. */
	static final int THREE_BITS_MASK = 0b00000111;
	/** Bottom five bits. */
	static final int CORE_MASK = 0b00011111;
	/** The index of the X field in argument 2. */
	static final int DEST_X_FIELD = 24;
	/** The index of the Y field in argument 2. */
	static final int DEST_Y_FIELD = 16;
	/** The mask of the port field in argument 2. */
	static final int PORT_MASK = 0xFFFF;
	/** Shift by one byte. */
	static final int BYTE_SHIFT = 8;
	/** Bits in an SDP port. */
	static final int PORT_SHIFT = 5;
}
