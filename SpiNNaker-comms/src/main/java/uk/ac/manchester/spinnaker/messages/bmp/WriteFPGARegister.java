package uk.ac.manchester.spinnaker.messages.bmp;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LINK_WRITE;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A request for writing data to a FPGA register.
 *
 * @see <a href="https://github.com/SpiNNakerManchester/spio/blob/master/designs/spinnaker_fpgas/README.md#spi-interface">
 *      spinnaker_fpga design README</a> for a listing of FPGA registers
 * @see <a href="https://github.com/SpiNNakerManchester/spio/">The SpI/O project
 *      on GitHub</a>
 */
public class WriteFPGARegister extends BMPRequest<WriteFPGARegister.Response> {
	private static final int MASK = 0b00000011;

	/**
	 * @param fpgaNum
	 *            FPGA number (0, 1 or 2 on SpiNN-5 board) to communicate with.
	 * @param register
	 *            Register address to read to (will be rounded down to the
	 *            nearest 32-bit word boundary).
	 * @param value
	 *            A 32-bit value to write to the register.
	 * @param board
	 *            which board to write the ADC register on
	 */
	public WriteFPGARegister(int fpgaNum, int register, int value, int board) {
		super(board, CMD_LINK_WRITE, register & ~MASK, WORD_SIZE, fpgaNum,
				data(value));
	}

	private static ByteBuffer data(int value) {
		return allocate(WORD_SIZE).order(LITTLE_ENDIAN).putInt(value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request to write an FPGA register. */
	public final class Response extends BMPRequest.BMPResponse {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Send FPGA register write", CMD_LINK_WRITE, buffer);
		}
	}
}
