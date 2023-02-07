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

import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.IntStream.range;
import static uk.ac.manchester.spinnaker.messages.bmp.BMPInfo.CAN_STATUS;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_INFO;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * SCP Request for the CAN bus status data from the BMP. The response payload is
 * an {@linkplain MappableIterable iterable} of enabled {@linkplain BMPBoard
 * board numbers}.
 * <p>
 * Handled in {@code cmd_bmp_info()} (in {@code bmp_cmd.c}) by reading from
 * {@code can_status}.
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
	protected final class Response
			extends BMPRequest.PayloadedResponse<MappableIterable<BMPBoard>> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read CAN Status", CMD_BMP_INFO, buffer);
		}

		/**
		 * @return Ordered sequence of board numbers available to be managed by
		 *         the BMP.
		 */
		@Override
		protected MappableIterable<BMPBoard> parse(ByteBuffer buffer) {
			/*
			 * The status data. The byte at {@code x} is zero if the BMP with
			 * that index is disabled.
			 */
			var statusData = new byte[buffer.remaining()];
			buffer.get(statusData);
			var boards = range(0, MAX_BOARDS_PER_FRAME)
					.filter(i -> statusData[i] != 0).mapToObj(BMPBoard::new)
					.collect(toUnmodifiableList());
			return boards::iterator;
		}
	}
}
