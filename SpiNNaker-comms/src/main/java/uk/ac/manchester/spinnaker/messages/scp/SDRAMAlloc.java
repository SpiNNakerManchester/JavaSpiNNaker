package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.String.format;
import static uk.ac.manchester.spinnaker.messages.scp.AllocFree.ALLOC_SDRAM;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_ALLOC;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** An SCP Request to allocate space in the SDRAM space */
public class SDRAMAlloc extends SCPRequest<SDRAMAlloc.Response> {
	private final int size;

	/**
	 * @param chip
	 *            the chip to allocate on
	 * @param appID
	 *            The ID of the application, between 0 and 255
	 * @param size
	 *            The size in bytes of memory to be allocated
	 */
	public SDRAMAlloc(HasChipLocation chip, int appID, int size) {
		this(chip, appID, size, 0);
	}

	/**
	 * @param chip
	 *            the chip to allocate on
	 * @param appID
	 *            The ID of the application, between 0 and 255
	 * @param size
	 *            The size in bytes of memory to be allocated
	 * @param tag
	 *            the tag for the SDRAM, a 8-bit (chip-wide) tag that can be
	 *            looked up by a SpiNNaker application to discover the address
	 *            of the allocated block
	 */
	public SDRAMAlloc(HasChipLocation chip, int appID, int size, int tag) {
		super(new SDPHeader(REPLY_EXPECTED,
				new CoreLocation(chip.getX(), chip.getY(), 0), 0),
				new SCPRequestHeader(CMD_ALLOC), argument1(appID), size, tag);
		this.size = size;
		if (tag < 0 || tag >= 256) {
			throw new IllegalArgumentException(
					"The tag parameter needs to be between 0 and 255");
		}
	}

	private static int argument1(int appID) {
		return (appID << 8) | ALLOC_SDRAM.value;
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(size, buffer);
	}

	/**
	 * An SCP response to a request to allocate space in SDRAM
	 */
	public static class Response extends CheckOKResponse {
		/** The base address allocated */
		public final int baseAddress;

		Response(int size, ByteBuffer buffer) throws Exception {
			super("SDRAM Allocation", CMD_ALLOC, buffer);
			baseAddress = buffer.getInt();
			if (baseAddress == 0) {
				throw new MemoryAllocationFailedException(
						format("Could not allocate %d bytes of SDRAM", size));
			}
		}
	}
}
