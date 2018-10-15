package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LINK_WRITE;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/** A request to write memory on a neighbouring chip. */
public class WriteLink extends SCPRequest<CheckOKResponse> {
	/**
	 * @param core
	 *            the core to write via
	 * @param link
	 *            The ID of the link down which to send the write
	 * @param baseAddress
	 *            The positive base address to start the write at
	 * @param data
	 *            The data to write (up to 256 bytes); the <i>position</i> of
	 *            the buffer must be the point where the data starts.
	 */
	public WriteLink(HasCoreLocation core, int link, int baseAddress,
			ByteBuffer data) {
		super(core, CMD_LINK_WRITE, baseAddress, data.remaining(), link, data);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Write Memory", CMD_LINK_WRITE, buffer);
	}
}
