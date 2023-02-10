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
