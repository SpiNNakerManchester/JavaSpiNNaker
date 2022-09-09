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

import static java.net.InetAddress.getByAddress;
import static uk.ac.manchester.spinnaker.messages.bmp.BMPInfo.IP_ADDR;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_INFO;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * SCP Request for the IP address data from a BMP.
 */
public class ReadIPAddress extends BMPRequest<ReadIPAddress.Response> {
	/**
	 * @param board
	 *            which board to request the IP address data from
	 */
	public ReadIPAddress(BMPBoard board) {
		super(board, CMD_BMP_INFO, IP_ADDR.value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for IP address information. */
	public final class Response extends BMPRequest.BMPResponse {
		/** The IP address of the BMP. */
		public final InetAddress bmpIPAddress;

		/** The IP address of the managed SpiNNaker board. */
		public final InetAddress spinIPAddress;

		private static final int CHUNK_LEN = 32;

		private static final int IP_OFFSET = 8;

		private static final int IP_LEN = 4;

		private byte[] getChunk(ByteBuffer buffer) {
			byte[] chunk = new byte[CHUNK_LEN];
			buffer.get(chunk);
			return chunk;
		}

		private InetAddress getIP(byte[] chunk) throws UnknownHostException {
			byte[] bytes = new byte[IP_LEN];
			System.arraycopy(chunk, IP_OFFSET, bytes, 0, IP_LEN);
			return getByAddress(bytes);
		}

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException, UnknownHostException {
			super("Read IP Address Data", CMD_BMP_INFO, buffer);
			byte[] bmpChunk = getChunk(buffer);
			byte[] spinChunk = getChunk(buffer);
			bmpIPAddress = getIP(bmpChunk);
			spinIPAddress = getIP(spinChunk);
		}
	}
}
