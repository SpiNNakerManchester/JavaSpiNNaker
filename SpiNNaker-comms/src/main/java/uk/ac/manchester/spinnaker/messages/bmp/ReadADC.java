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
package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.bmp.BMPInfo.ADC;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_INFO;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * SCP Request for the ADC data from the BMP including voltages and temperature.
 * The response payload is the {@linkplain ADCInfo information structure} from
 * the hardware.
 * <p>
 * Handled in {@code cmd_bmp_info()} (in {@code bmp_cmd.c}) by reading from the
 * right element of {@code board_stat}. The underlying data is synched from the
 * ADC approximately every 80ms by {@code read_adc()} in {@code bmp_hw.c}.
 */
public class ReadADC extends BMPRequest<ReadADC.Response> {
	/**
	 * @param board
	 *            which board to request the ADC register from
	 */
	public ReadADC(BMPBoard board) {
		super(board, CMD_BMP_INFO, ADC.value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for ADC information. */
	protected final class Response
			extends BMPRequest.PayloadedResponse<ADCInfo> {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read ADC", CMD_BMP_INFO, buffer);
		}

		/** @return The ADC information. */
		@Override
		protected ADCInfo parse(ByteBuffer buffer) {
			return new ADCInfo(buffer);
		}
	}
}
