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
package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.bmp.BMPInfo.SERIAL;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_INFO;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * SCP Request for the serial data vector from the BMP.
 */
public class ReadSerialVector extends BMPRequest<ReadSerialVector.Response> {
	/**
	 * @param board
	 *            which board to request the serial data from
	 */
	public ReadSerialVector(BMPBoard board) {
		super(board, CMD_BMP_INFO, SERIAL.value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for serial data. */
	public static final class Response
			extends BMPRequest.PayloadedResponse<SerialVector> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read serial data vector", CMD_BMP_INFO, buffer);
		}

		/** @return The serial data. */
		@Override
		protected SerialVector parse(ByteBuffer buffer) {
			return new SerialVector(buffer.asIntBuffer());
		}
	}

	/** The data read from the serial vector. */
	public static final class SerialVector {
		private static final int HW_VERSION_INDEX = 0;

		private static final int SERIAL_INDEX = 1;

		/** The length of the serial number, in words. */
		public static final int SERIAL_LENGTH = 4;

		private static final int FLASH_BUFFER_INDEX = 5;

		private static final int BOARD_STATUS_INDEX = 6;

		private static final int CORTEX_VECTOR_INDEX = 7;

		private final IntBuffer buffer;

		private SerialVector(IntBuffer buffer) {
			this.buffer = buffer;
		}

		/** @return The hardware version. */
		public int getHardwareVersion() {
			return buffer.get(HW_VERSION_INDEX);
		}

		/** @return The serial number data, as a read-only buffer. */
		public IntBuffer getSerialNumber() {
			// TODO use slice(int,int) once base Java version recent enough
			var b = buffer.asReadOnlyBuffer();
			b.position(SERIAL_INDEX).limit(SERIAL_INDEX + SERIAL_LENGTH);
			return b.slice();
		}

		/** @return The location of the flash buffer. */
		public MemoryLocation getFlashBuffer() {
			return new MemoryLocation(buffer.get(FLASH_BUFFER_INDEX));
		}

		/** @return The board status bit vector. */
		public int getBoardStatus() {
			// TODO what's the right return type?
			return buffer.get(BOARD_STATUS_INDEX);
		}

		/** @return The location of the cortex vector. */
		public MemoryLocation getCortexVector() {
			return new MemoryLocation(buffer.get(CORTEX_VECTOR_INDEX));
		}
	}
}
