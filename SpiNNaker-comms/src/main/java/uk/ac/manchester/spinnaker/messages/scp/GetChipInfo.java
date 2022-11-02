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

/** An SCP request to read the chip information from a core. */
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
	 *            Whether the size should be included in the response
	 */
	public GetChipInfo(HasChipLocation chip, boolean withSize) {
		super(chip.getScampCore(), CMD_INFO, argument1(withSize));
	}

	private static int argument1(boolean withSize) {
		// Bits 0-4 + bit 6 = all information except size
		// Bits 0-6 = all information including size
		return FLAGS | (withSize ? SIZE_FLAG : 0);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for the version of software running. */
	public static final class Response extends CheckOKResponse {
		/** The chip information received. */
		public final ChipSummaryInfo chipInfo;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Version", CMD_INFO, buffer);
			chipInfo = new ChipSummaryInfo(buffer, sdpHeader.getSource());
		}
	}
}
