package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_WRITE;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** A request to write memory on a chip */
public class WriteMemory extends SCPRequest<CheckOKResponse> {
	private enum WriteType {
		BYTE, HALF_WORD, WORD
	}

	private static WriteType efficientTransferUnit(int a, int b) {
		if (a % 4 == 0 && b % 4 == 0) {
			return WriteType.WORD;
		} else if (a % 4 == 2 || b % 4 == 2) {
			return WriteType.HALF_WORD;
		} else {
			return WriteType.BYTE;
		}
	}

	/**
	 * @param core
	 *            the core to write via
	 * @param baseAddress
	 *            The positive base address to start the read from
	 * @param data
	 *            Between 1 and 256 bytes to write
	 */
	public WriteMemory(HasCoreLocation core, int baseAddress, byte[] data) {
		super(new SDPHeader(REPLY_EXPECTED, core, 0),
				new SCPRequestHeader(CMD_WRITE), baseAddress, data.length,
				efficientTransferUnit(baseAddress, data.length).ordinal(),
				data);
	}

	/**
	 * @param chip
	 *            the chip to write via
	 * @param baseAddress
	 *            The positive base address to start the read from
	 * @param data
	 *            Between 1 and 256 bytes to write
	 */
	public WriteMemory(HasChipLocation chip, int baseAddress, byte[] data) {
		super(new SDPHeader(REPLY_EXPECTED, chip.getScampCore(), 0),
				new SCPRequestHeader(CMD_WRITE), baseAddress, data.length,
				efficientTransferUnit(baseAddress, data.length).ordinal(),
				data);
	}

	/**
	 * @param core
	 *            the core to write via
	 * @param baseAddress
	 *            The positive base address to start the read from
	 * @param data
	 *            Between 1 and 256 bytes to write; the <i>position</i> of the
	 *            buffer must be the point where the data starts.
	 */
	public WriteMemory(HasCoreLocation core, int baseAddress, ByteBuffer data) {
		super(new SDPHeader(REPLY_EXPECTED, core, 0),
				new SCPRequestHeader(CMD_WRITE), baseAddress, data.remaining(),
				efficientTransferUnit(baseAddress, data.remaining()).ordinal(),
				data);
	}

	/**
	 * @param chip
	 *            the chip to write via
	 * @param baseAddress
	 *            The positive base address to start the read from
	 * @param data
	 *            Between 1 and 256 bytes to write; the <i>position</i> of the
	 *            buffer must be the point where the data starts.
	 */
	public WriteMemory(HasChipLocation chip, int baseAddress, ByteBuffer data) {
		super(new SDPHeader(REPLY_EXPECTED, chip.getScampCore(), 0),
				new SCPRequestHeader(CMD_WRITE), baseAddress, data.remaining(),
				efficientTransferUnit(baseAddress, data.remaining()).ordinal(),
				data);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Write", CMD_WRITE, buffer);
	}
}
