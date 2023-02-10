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
package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.bmp.BMPInfo.SERIAL;
import static uk.ac.manchester.spinnaker.messages.bmp.SerialVector.SERIAL_LENGTH;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_INFO;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * SCP Request for the serial data vector from the BMP. The response payload is
 * the {@linkplain SerialVector serial vector} from the BMP.
 * <p>
 * Handled by {@code cmd_bmp_info()} in {@code bmp_cmd.c}.
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
	protected final class Response
			extends BMPRequest.PayloadedResponse<SerialVector> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read serial data vector", CMD_BMP_INFO, buffer);
		}

		/** @return The serial data. */
		@Override
		protected SerialVector parse(ByteBuffer buffer) {
			var b = buffer.asIntBuffer();
			var hardwareVersion = b.get();
			var sn = new int[SERIAL_LENGTH];
			b.get(sn);
			var serialNumber = IntBuffer.wrap(sn);
			var flashBuffer = new MemoryLocation(b.get());
			var boardStat = new MemoryLocation(b.get());
			var cortexBoot = new MemoryLocation(b.get());
			return new SerialVector(hardwareVersion, serialNumber, flashBuffer,
					boardStat, cortexBoot);
		}
	}
}
