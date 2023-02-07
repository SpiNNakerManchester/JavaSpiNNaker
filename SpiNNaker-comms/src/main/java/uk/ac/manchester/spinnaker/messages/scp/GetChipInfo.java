/*
 * Copyright (c) 2018 The University of Manchester
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
	public Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new Response(buffer);
	}

	/** An SCP response to a request for the version of software running. */
	protected final class Response
			extends PayloadedResponse<ChipSummaryInfo, RuntimeException> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Version", CMD_INFO, buffer);
		}

		/** @return The chip summary information. */
		@Override
		protected ChipSummaryInfo parse(ByteBuffer buffer) {
			return new ChipSummaryInfo(buffer, sdpHeader.getSource());
		}
	}
}
