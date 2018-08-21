package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.String.format;
import static uk.ac.manchester.spinnaker.messages.model.AllocFree.ALLOC_ROUTING;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_ALLOC;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.MemoryAllocationFailedException;

/** An SCP Request to allocate space for routing entries. */
public class RouterAlloc extends SCPRequest<RouterAlloc.Response> {
	private final int numEntries;

	/**
	 * @param chip
	 *            the chip to allocate on
	 * @param appID
	 *            The ID of the application, between 0 and 255
	 * @param numEntries
	 *            The number of entries to allocate
	 *
	 */
	public RouterAlloc(HasChipLocation chip, int appID, int numEntries) {
		super(chip.getScampCore(), CMD_ALLOC, argument1(appID), numEntries,
				null);
		this.numEntries = numEntries;
	}

	private static int argument1(int appID) {
		return (appID << BYTE1) | (ALLOC_ROUTING.value << BYTE0);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(numEntries, buffer);
	}

	/** An SCP response to a request to allocate router entries. */
	public static class Response extends CheckOKResponse {
		/** The base address allocated, or 0 if none. */
		public final int baseAddress;

		Response(int size, ByteBuffer buffer) throws Exception {
			super("Router Allocation", CMD_ALLOC, buffer);
			baseAddress = buffer.getInt();
			if (baseAddress == 0) {
				throw new MemoryAllocationFailedException(
						format("Could not allocate %d router entries", size));
			}
		}
	}
}
