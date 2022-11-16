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
package uk.ac.manchester.spinnaker.alloc.client;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.nio.ByteBuffer;

/** Message IDs in the proxy protocol. */
enum ProxyProtocol {
	/** Open a connected channel, or its response. Two-way. */
	OPEN(20),
	/** Close a channel, or its response. Two-way. */
	CLOSE(12),
	/** Send a message on a connected channel, or receive a message. One-way. */
	MSG(1600),
	/** Open an unconnected channel, or its response. Two-way. */
	OPEN_U(8),
	/** Send a message on an unconnected channel. Never received. One-way. */
	MSG_TO(1600);

	private final int size;

	ProxyProtocol(int size) {
		this.size = size;
	}

	/**
	 * Create a buffer big enough to hold a message and fill the first word with
	 * the protocol ID.
	 *
	 * @return A little endian buffer of sufficient size. Position will be after
	 *         first word.
	 */
	ByteBuffer allocate() {
		var b = ByteBuffer.allocate(size).order(LITTLE_ENDIAN);
		b.putInt(ordinal());
		return b;
	}
}
