/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.scp;

/** Constants for working with tags. */
abstract class IPTagFieldDefinitions {
	private IPTagFieldDefinitions() {
	}

	/** The index of the use sender bit in argument 1. */
	static final int USE_SENDER_BIT = 30;

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
