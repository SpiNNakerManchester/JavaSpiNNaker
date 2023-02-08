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
	protected static final class Response
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
