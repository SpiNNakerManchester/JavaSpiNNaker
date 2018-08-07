package uk.ac.manchester.spinnaker.messages.scp;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_READ;
import static uk.ac.manchester.spinnaker.messages.scp.TransferUnit.efficientTransferUnit;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** An SCP request to read a region of memory. */
public class ReadMemory extends SCPRequest<ReadMemory.Response> {
	private static final int SIZE_MASK = 0xFF;

	/**
	 * @param core
	 *            the core to read via
	 * @param address
	 *            The positive base address to start the read from
	 * @param size
	 *            The number of bytes to read, between 1 and 256
	 */
	public ReadMemory(HasCoreLocation core, int address, int size) {
		super(core, CMD_READ, address, size & SIZE_MASK,
				efficientTransferUnit(address, size).value);
	}

	/**
	 * @param chip
	 *            the chip to read via
	 * @param address
	 *            The positive base address to start the read from
	 * @param size
	 *            The number of bytes to read, between 1 and 256
	 */
	public ReadMemory(HasChipLocation chip, int address, int size) {
		super(chip.getScampCore(), CMD_READ, address, size & SIZE_MASK,
				efficientTransferUnit(address, size).value);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request to read a region of memory on a chip.
	 */
	public static class Response extends CheckOKResponse {
		/** The data read. */
		public final ByteBuffer data;

		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Read", CMD_READ, buffer);
			this.data = buffer.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		}
	}
}
