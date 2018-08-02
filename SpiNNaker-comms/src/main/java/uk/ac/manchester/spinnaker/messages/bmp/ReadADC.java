package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.bmp.BMPInfo.ADC;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_INFO;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * SCP Request for the data from the BMP including voltages and temperature.
 */
public class ReadADC extends BMPRequest<ReadADC.Response> {
	/**
	 * @param board
	 *            which board to request the ADC register from
	 */
	public ReadADC(int board) {
		super(board, CMD_BMP_INFO, (int) ADC.value, null, null);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for ADC information. */
	public final class Response extends BMPRequest.BMPResponse {
		/** The ADC information. */
		public final ADCInfo adcInfo;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read ADC", CMD_BMP_INFO, buffer);
			adcInfo = new ADCInfo(buffer);
		}
	}
}
