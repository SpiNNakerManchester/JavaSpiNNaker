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

import static java.net.InetAddress.getByAddress;
import static uk.ac.manchester.spinnaker.messages.bmp.BMPInfo.IP_ADDR;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_INFO;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * SCP Request for the IP address data from a BMP. The response payload is the
 * {@linkplain Addresses pair of addresses} that the board is configured to
 * have.
 * <p>
 * Handled by {@code cmd_bmp_info()} in {@code bmp_cmd.c}.
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

	/** The IP addresses associated with a SpiNNaker board. */
	// TODO convert to record in 17
	public static final class Addresses {
		/** The IPv4 address of the BMP. */
		public final InetAddress bmpIPAddress;

		/** The IPv4 address of the managed SpiNNaker board. */
		public final InetAddress spinIPAddress;

		private static final int CHUNK_LEN = 32;

		private static final int IP_OFFSET = 8;

		private static final int IP_LEN = 4;

		private static byte[] getChunk(ByteBuffer buffer) {
			byte[] chunk = new byte[CHUNK_LEN];
			buffer.get(chunk);
			return chunk;
		}

		private static InetAddress getIP(byte[] chunk)
				throws UnknownHostException {
			byte[] bytes = new byte[IP_LEN];
			System.arraycopy(chunk, IP_OFFSET, bytes, 0, IP_LEN);
			return getByAddress(bytes);
		}

		private Addresses(ByteBuffer buffer) throws UnknownHostException {
			byte[] bmpChunk = getChunk(buffer);
			byte[] spinChunk = getChunk(buffer);
			bmpIPAddress = getIP(bmpChunk);
			spinIPAddress = getIP(spinChunk);
		}
	}

	/** An SCP response to a request for IP address information. */
	public static final class Response
			extends BMPRequest.PayloadedResponse<Addresses> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read IP Address Data", CMD_BMP_INFO, buffer);
		}

		/** @return The addresses of the SpiNNaker board. */
		@Override
		protected Addresses parse(ByteBuffer buffer) {
			try {
				return new Addresses(buffer);
			} catch (UnknownHostException e) {
				// Should be unreachable
				return null;
			}
		}
	}
}
