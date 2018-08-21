package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_WRITE;
import static uk.ac.manchester.spinnaker.messages.scp.TransferUnit.efficientTransferUnit;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/** A request to write memory on a chip. */
public class WriteMemory extends SCPRequest<CheckOKResponse> {
	/**
	 * @param core
	 *            the core to write via
	 * @param baseAddress
	 *            The positive base address to start the read from
	 * @param data
	 *            Between 1 and 256 bytes to write
	 */
	public WriteMemory(HasCoreLocation core, int baseAddress, byte[] data) {
		super(core, CMD_WRITE, baseAddress, data.length,
				efficientTransferUnit(baseAddress, data.length).value, data);
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
		super(chip.getScampCore(), CMD_WRITE, baseAddress, data.length,
				efficientTransferUnit(baseAddress, data.length).value, data);
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
		super(core, CMD_WRITE, baseAddress, data.remaining(),
				efficientTransferUnit(baseAddress, data.remaining()).value,
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
		super(chip.getScampCore(), CMD_WRITE, baseAddress, data.remaining(),
				efficientTransferUnit(baseAddress, data.remaining()).value,
				data);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Write", CMD_WRITE, buffer);
	}
}
