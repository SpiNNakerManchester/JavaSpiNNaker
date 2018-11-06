package uk.ac.manchester.spinnaker.messages.scp;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LINK_READ;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** An SCP request to read a region of memory via a link on a chip. */
public class ReadLink extends SCPRequest<ReadLink.Response> {
	private static final int MAX_SIZE = 256;
	private static int validate(int size) {
		if (size < 1 || size > MAX_SIZE) {
			throw new IllegalArgumentException("size must be in range 1 to 256");
		}
		return size;
	}

	/**
	 * @param core
	 *            the core to read via
	 * @param link
	 *            The ID of the link down which to send the query
	 * @param baseAddress
	 *            The positive base address to start the read from
	 * @param size
	 *            The number of bytes to read, between 1 and 256
	 */
	public ReadLink(HasCoreLocation core, int link, int baseAddress, int size) {
		super(core, CMD_LINK_READ, baseAddress, validate(size), link);
	}

	/**
	 * @param chip
	 *            the chip to read via
	 * @param link
	 *            The ID of the link down which to send the query
	 * @param baseAddress
	 *            The positive base address to start the read from
	 * @param size
	 *            The number of bytes to read, between 1 and 256
	 */
	public ReadLink(HasChipLocation chip, int link, int baseAddress, int size) {
		super(chip.getScampCore(), CMD_LINK_READ, baseAddress, validate(size),
				link);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/**
	 * An SCP response to a request to read a region of memory via a link on a
	 * chip.
	 */
	public static class Response extends CheckOKResponse {
		/** The data read. */
		public final ByteBuffer data;

		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Read Link", CMD_LINK_READ, buffer);
			this.data = buffer.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		}
	}
}
