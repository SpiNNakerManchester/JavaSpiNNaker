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
package uk.ac.manchester.spinnaker.connections;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * A packet with an address.
 */
public class UDPPacket {

	private final ByteBuffer byteBuffer;

	private final SocketAddress address;

	/**
	 * Create a buffer with an address.
	 * @param byteBuffer The buffer
	 * @param address The address
	 */
	public UDPPacket(ByteBuffer byteBuffer, SocketAddress address) {
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
	public SocketAddress getAddress() {
		return address;
	}
}
