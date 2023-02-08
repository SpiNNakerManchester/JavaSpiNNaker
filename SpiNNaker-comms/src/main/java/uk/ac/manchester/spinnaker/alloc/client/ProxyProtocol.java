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
