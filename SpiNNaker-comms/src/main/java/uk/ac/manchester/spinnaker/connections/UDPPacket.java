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
package uk.ac.manchester.spinnaker.connections;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * A packet with an address.
 */
public class UDPPacket {

	private final ByteBuffer byteBuffer;

	private final InetSocketAddress address;

	/**
	 * Create a buffer with an address.
	 * @param byteBuffer The buffer
	 * @param address The address
	 */
	public UDPPacket(ByteBuffer byteBuffer, InetSocketAddress address) {
		this.byteBuffer = byteBuffer;
		this.address = address;
	}

	/**
	 * Get the buffer.
	 * @return The buffer
	 */
	public ByteBuffer getByteBuffer() {
		return byteBuffer;
	}

	/**
	 * Get the address.
	 * @return The address
	 */
	public InetSocketAddress getAddress() {
		return address;
	}
}
