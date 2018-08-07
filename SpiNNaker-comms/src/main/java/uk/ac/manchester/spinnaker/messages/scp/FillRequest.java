package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_FILL;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/** An SCP request to fill a region of memory on a chip with repeated data. */
public final class FillRequest extends SCPRequest<CheckOKResponse> {
	/**
	 * @param chip
	 *            The chip to read from
	 * @param baseAddress
	 *            The positive base address to start the fill from
	 * @param data
	 *            The data to fill in the space with
	 * @param size
	 *            The number of bytes to fill in
	 */
	public FillRequest(HasChipLocation chip, int baseAddress, int data,
			int size) {
		super(chip.getScampCore(), CMD_FILL, baseAddress, data, size);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Fill", CMD_FILL, buffer);
	}
}
