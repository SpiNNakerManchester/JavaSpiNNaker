/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_INFO;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.ChipSummaryInfo;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP request to read the chip information from a core. The response payload
 * is the {@linkplain ChipSummaryInfo summary information} for the chip.
 * <p>
 * Calls {@code cmd_info()} in {@code scamp-cmd.c}.
 */
public class GetChipInfo extends SCPRequest<GetChipInfo.Response> {
	private static final int FLAGS = 0x5F;

	private static final int SIZE_FLAG = 0x20;

	/**
	 * @param chip
	 *            the chip to read from
	 */
	public GetChipInfo(HasChipLocation chip) {
		this(chip, false);
	}

	/**
	 * @param chip
	 *            the chip to read from
	 * @param withSize
	 *            Whether the size should be included in the response. <em>Might
	 *            be ignored by SCAMP.</em>
	 */
	public GetChipInfo(HasChipLocation chip, boolean withSize) {
		super(chip.getScampCore(), CMD_INFO, informationSelect(withSize));
	}

	private static int informationSelect(boolean withSize) {
		// Bits 0-4 + bit 6 = all information except size
		// Bits 0-6 = all information including size
		return FLAGS | (withSize ? SIZE_FLAG : 0);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for the version of software running. */
	protected static final class Response
			extends PayloadedResponse<ChipSummaryInfo, RuntimeException> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Version", CMD_INFO, buffer);
		}

		@Override
		protected ChipSummaryInfo parse(ByteBuffer buffer) {
			return new ChipSummaryInfo(buffer, sdpHeader.getSource());
		}
	}
}
