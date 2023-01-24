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

import static uk.ac.manchester.spinnaker.messages.bmp.BMPInfo.CAN_STATUS;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_INFO;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * SCP Request for the CAN bus status data from the BMP.
 */
public class ReadCANStatus extends BMPRequest<ReadCANStatus.Response> {
	private static final int MAX_BOARDS_PER_FRAME = 24;

	private static final BMPBoard FRAME_ROOT = new BMPBoard(0);

	/** Create a request. */
	public ReadCANStatus() {
		// The CAN status is shared between all BMPs; it's how they communicate
		super(FRAME_ROOT, CMD_BMP_INFO, CAN_STATUS.value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for the CAN status. */
	public static final class Response
			extends BMPRequest.PayloadedResponse<MappableIterable<Integer>> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read CAN Status", CMD_BMP_INFO, buffer);
		}

		/**
		 * @return Ordered sequence of board numbers available to be managed by
		 *         the BMP.
		 */
		@Override
		protected MappableIterable<Integer> parse(ByteBuffer buffer) {
			/*
			 * The status data. The byte at {@code x} is zero if the BMP with
			 * that index is disabled.
			 */
			var statusData = new byte[buffer.remaining()];
			buffer.get(statusData);
			var boards = new ArrayList<Integer>();
			for (int i = 0; i < MAX_BOARDS_PER_FRAME; i++) {
				if (statusData[i] != 0) {
					boards.add(i);
				}
			}
			return boards::iterator;
		}
	}
}
