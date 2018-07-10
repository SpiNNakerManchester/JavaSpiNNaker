package uk.ac.manchester.spinnaker.messages.scp;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_READ;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** An SCP request to read a region of memory */
public class ReadMemory extends SCPRequest<ReadMemory.Response> {
	private enum ReadType {
		BYTE, HALF_WORD, WORD
	}

	private static ReadType efficientTransferUnit(int a, int b) {
		if (a % 4 == 0 && b % 4 == 0) {
			return ReadType.WORD;
		} else if (a % 4 == 2 || b % 4 == 2) {
			return ReadType.HALF_WORD;
		} else {
			return ReadType.BYTE;
		}
	}

	/**
	 * @param core
	 *            the core to read via
	 * @param baseAddress
	 *            The positive base address to start the read from
	 * @param size
	 *            The number of bytes to read, between 1 and 256
	 */
	public ReadMemory(HasCoreLocation core, int baseAddress, int size) {
		super(new SDPHeader(REPLY_EXPECTED, core, 0),
				new SCPRequestHeader(CMD_READ), baseAddress, size & 0xFF,
				efficientTransferUnit(baseAddress, size).ordinal());
	}

	/**
	 * @param chip
	 *            the chip to read via
	 * @param baseAddress
	 *            The positive base address to start the read from
	 * @param size
	 *            The number of bytes to read, between 1 and 256
	 */
	public ReadMemory(HasChipLocation chip, int baseAddress, int size) {
		super(new SDPHeader(REPLY_EXPECTED, chip.getScampCore(), 0),
				new SCPRequestHeader(CMD_READ), baseAddress, size & 0xFF,
				efficientTransferUnit(baseAddress, size).ordinal());
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request to read a region of memory on a chip
	 */
	public static class Response extends CheckOKResponse {
		/** The data read */
		public final ByteBuffer data;

		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Read", CMD_READ, buffer);
			this.data = buffer.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		}
	}
}
