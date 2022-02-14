package uk.ac.manchester.spinnaker.connections;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * A packet with an address
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
