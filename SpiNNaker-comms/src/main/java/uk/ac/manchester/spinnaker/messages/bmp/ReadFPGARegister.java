package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LINK_READ;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * Requests the data from a FPGA's register.
 */
public class ReadFPGARegister extends BMPRequest<ReadFPGARegister.Response> {
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
		super(board, CMD_LINK_READ, register & (~0x3), 4, fpgaNum);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for the contents of an FPGA register */
	public class Response extends BMPRequest.BMPResponse {
		/** The ADC information */
		public final int fpgaRegister;

		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Read FPGA register", CMD_LINK_READ, buffer);
			fpgaRegister = buffer.getInt();
		}
	}
}
