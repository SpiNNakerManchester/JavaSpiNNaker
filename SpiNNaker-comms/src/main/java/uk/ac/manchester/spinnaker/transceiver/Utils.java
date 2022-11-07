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
package uk.ac.manchester.spinnaker.transceiver;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_INFO_BYTES;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.transceiver.CommonMemoryLocations.CPU_INFO;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.slice;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

import com.google.errorprone.annotations.InlineMe;

import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.Constants;
import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Support utilities.
 */
public abstract class Utils {
	/**
	 * The size of buffer to allocate for SpiNNaker messages.
	 */
	private static final int SPINNAKER_MESSAGE_BUFFER_SIZE = 300;

	private Utils() {
	}

	/**
	 * Work out the BMP connection IP address given the machine details. This is
	 * assumed to be the IP address of the machine, with 1 subtracted from the
	 * final part e.g. if the machine IP address is 192.168.0.5, the BMP IP
	 * address is assumed to be 192.168.0.4
	 *
	 * @param host
	 *            the SpiNNaker machine main host
	 * @param numberOfBoards
	 *            the number of boards in the machine
	 * @return The BMP connection data
	 * @throws UnknownHostException
	 *             If the IP address computations fail.
	 */
	public static BMPConnectionData defaultBMPforMachine(InetAddress host,
			Integer numberOfBoards) throws UnknownHostException {
		return new BMPConnectionData(host,
				numberOfBoards == null ? 0 : numberOfBoards);
	}

	/**
	 * Get the address of the {@code vcpu_t} structure for the given core.
	 *
	 * @param p
	 *            The core number
	 * @return the address
	 */
	public static MemoryLocation getVcpuAddress(int p) {
		return CPU_INFO.add(CPU_INFO_BYTES * p);
	}

	/**
	 * Get the address of the {@code vcpu_t} structure for the given core.
	 *
	 * @param core
	 *            The core
	 * @return the address
	 */
	public static MemoryLocation getVcpuAddress(HasCoreLocation core) {
		return CPU_INFO.add(CPU_INFO_BYTES * core.getP());
	}

	/**
	 * Sends a port trigger message using a connection to (hopefully) open a
	 * port in a NAT and/or firewall to allow incoming packets to be received.
	 *
	 * @param connection
	 *            The UDP connection down which the trigger message should be
	 *            sent
	 * @param host
	 *            The address of the SpiNNaker board to which the message should
	 *            be sent
	 * @deprecated Call
	 *             {@link UDPConnection#sendPortTriggerMessage(InetAddress)}
	 *             directly instead.
	 * @throws IOException
	 *             If anything goes wrong
	 */
	@Deprecated(forRemoval = true)
	@InlineMe(replacement = "connection.sendPortTriggerMessage(host)")
	public static void sendPortTriggerMessage(UDPConnection<?> connection,
			InetAddress host) throws IOException {
		connection.sendPortTriggerMessage(host);
	}

	/**
	 * @return Get a new little-endian buffer sized suitably for SpiNNaker
	 *         messages.
	 */
	public static ByteBuffer newMessageBuffer() {
		// TODO How big should this buffer be? 256 or (256 + header size)?
		return allocate(SPINNAKER_MESSAGE_BUFFER_SIZE).order(LITTLE_ENDIAN);
	}

	/**
	 * Fill a section of a buffer with a constant value.
	 *
	 * @param buffer
	 *            The buffer to fill a chunk of.
	 * @param start
	 *            Where in the buffer to start.
	 * @param len
	 *            How many bytes to write.
	 * @param value
	 *            The value to write.
	 */
	static void fill(ByteBuffer buffer, int start, int len, byte value) {
		if (buffer.hasArray()) {
			int ao = buffer.arrayOffset();
			Arrays.fill(buffer.array(), ao + start, ao + start + len, value);
		} else {
			var work = new byte[len];
			Arrays.fill(work, value);
			var buf = buffer.duplicate();
			buf.position(start);
			buf.put(work);
		}
	}

	/**
	 * Compute the CRC of a section of buffer.
	 *
	 * @param buffer
	 *            The buffer.
	 * @param start
	 *            Where in the buffer to start.
	 * @param len
	 *            How many bytes to get the CRC of.
	 * @return The CRC (as a signed integer).
	 */
	static int crc(ByteBuffer buffer, int start, int len) {
		var crc = new CRC32();
		crc.update(slice(buffer, start, len));
		return (int) crc.getValue();
	}

	/**
	 * Convert a value to a little-endian word in a buffer.
	 *
	 * @param value
	 *            The value to convert.
	 * @return The newly-allocated buffer, {@link Constants#WORD_SIZE} long,
	 *         flipped.
	 */
	@UsedInJavadocOnly(Constants.class)
	public static ByteBuffer word(int value) {
		return allocate(WORD_SIZE).order(LITTLE_ENDIAN).putInt(value).flip();
	}

	/**
	 * Convert a value to a single byte in a buffer.
	 *
	 * @param value
	 *            The value to convert.
	 * @return The newly-allocated buffer, 1 long, flipped.
	 */
	static ByteBuffer oneByte(int value) {
		return allocate(1).put((byte) value).flip();
	}

	/**
	 * Convert a value to a single byte in a buffer.
	 *
	 * @param value
	 *            The value to convert.
	 * @return The newly-allocated buffer, 8 long, flipped.
	 */
	static ByteBuffer oneLong(long value) {
		return allocate(WORD_SIZE * 2).order(LITTLE_ENDIAN).putLong(value)
				.flip();
	}
}
