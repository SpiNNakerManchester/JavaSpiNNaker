package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LINK_READ;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * Requests the data from a FPGA's register.
 */
public class ReadFPGARegister extends BMPRequest<ReadFPGARegister.Response> {
	private static final int MASK = 0b00000011;

	/**
	 * @param fpgaNum
	 *            FPGA number (0, 1 or 2 on SpiNN-5 board) to communicate with.
	 * @param register
	 *            Register address to read to (will be rounded down to the
	 *            nearest 32-bit word boundary).
	 * @param board
	 *            which board to request the ADC register from
	 */
	public ReadFPGARegister(int fpgaNum, int register, int board) {
		super(board, CMD_LINK_READ, register & ~MASK, WORD_SIZE, fpgaNum);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for the contents of an FPGA register. */
	public final class Response extends BMPRequest.BMPResponse {
		/** The ADC information. */
		public final int fpgaRegister;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read FPGA register", CMD_LINK_READ, buffer);
			fpgaRegister = buffer.getInt();
		}
	}
}
