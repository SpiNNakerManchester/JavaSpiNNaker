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

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * SCP Request for the CAN bus status data from the BMP.
 */
public class ReadCANStatus extends BMPRequest<ReadCANStatus.Response> {
	public ReadCANStatus() {
		// The CAN status is shared between all BMPs; it's how they communicate
		super(0, CMD_BMP_INFO, (int) CAN_STATUS.value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for the CAN status. */
	public final class Response extends BMPRequest.BMPResponse {
		/**
		 * The status data. The byte at {@code x} is zero if the BMP with that
		 * index is disabled.
		 */
		public final byte[] statusData;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read ADC", CMD_BMP_INFO, buffer);
			statusData = new byte[buffer.remaining()];
			buffer.get(statusData);
		}
	}
}
