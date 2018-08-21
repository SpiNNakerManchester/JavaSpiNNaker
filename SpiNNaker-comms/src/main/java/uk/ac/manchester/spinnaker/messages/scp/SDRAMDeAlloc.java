package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.model.AllocFree.FREE_SDRAM_BY_APP_ID;
import static uk.ac.manchester.spinnaker.messages.model.AllocFree.FREE_SDRAM_BY_POINTER;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_ALLOC;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.MemoryAllocationFailedException;

/** An SCP Request to free space in the SDRAM. */
public class SDRAMDeAlloc extends SCPRequest<SDRAMDeAlloc.Response> {
	private final boolean readNumFreedBlocks;

	/**
	 * Free the memory associated with a given application ID.
	 *
	 * @param chip
	 *            the chip to allocate on
	 * @param appID
	 *            The ID of the application, between 0 and 255
	 */
	public SDRAMDeAlloc(HasChipLocation chip, int appID) {
		super(chip.getScampCore(), CMD_ALLOC, argument1(appID), null, null);
		readNumFreedBlocks = true;
	}

	/**
	 * Free a block of memory of known size.
	 *
	 * @param chip
	 *            the chip to allocate on
	 * @param appID
	 *            The ID of the application, between 0 and 255 (ignored)
	 * @param baseAddress
	 *            The start address in SDRAM to which the block needs to be
	 *            deallocated
	 */
	public SDRAMDeAlloc(HasChipLocation chip, int appID, int baseAddress) {
		super(chip.getScampCore(), CMD_ALLOC, (int) FREE_SDRAM_BY_POINTER.value,
				baseAddress, null);
		readNumFreedBlocks = false;
	}

	private static int argument1(int appID) {
		return (appID << BYTE1) | (FREE_SDRAM_BY_APP_ID.value << BYTE0);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(readNumFreedBlocks, buffer);
	}

	/** An SCP response to a request to deallocate SDRAM. */
	public static class Response extends CheckOKResponse {
		/**
		 * The number of allocated blocks that have been freed from the appID
		 * given, or zero for when the direct amount of space to deallocate was
		 * given.
		 */
		public final int numFreedBlocks;

		Response(boolean readFreedBlocks, ByteBuffer buffer) throws Exception {
			super("SDRAM Deallocation", CMD_ALLOC, buffer);
			if (readFreedBlocks) {
				numFreedBlocks = buffer.getInt();
				if (numFreedBlocks == 0) {
					throw new MemoryAllocationFailedException(
							"Could not deallocate SDRAM");
				}
			} else {
				numFreedBlocks = 0;
			}
		}
	}
}
