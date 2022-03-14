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

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * SCP Request for the serial data vector from the BMP.
 */
public class ReadSerialVector extends BMPRequest<ReadSerialVector.Response> {
	/**
	 * @param board
	 *            which board to request the serial data from
	 */
	public ReadSerialVector(int board) {
		super(board, CMD_BMP_INFO, (int) SERIAL.value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for serial data. */
	public final class Response extends BMPRequest.BMPResponse {
		/** The serial data. */
		public final IntBuffer vector;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read ADC", CMD_BMP_INFO, buffer);
			vector = buffer.asIntBuffer().asReadOnlyBuffer();
		}
	}
}
