package uk.ac.manchester.spinnaker.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.SDRAMDeAlloc;

/** Deallocate space in the SDRAM */
public class DeallocSDRAMProcess extends MultiConnectionProcess {
	private int numBlocksFreed;

	public DeallocSDRAMProcess(ConnectionSelector connectionSelector) {
		super(connectionSelector);
	}

	/**
	 * Free the memory associated with a given application ID.
	 *
	 * @param chip
	 *            the chip to allocate on
	 * @param appID
	 *            The ID of the application, between 0 and 255
	 * @return the number of blocks freed
	 */
	public int deallocSDRAM(HasChipLocation chip, int appID)
			throws IOException, Exception {
		numBlocksFreed = synchronousCall(
				new SDRAMDeAlloc(chip, appID)).numFreedBlocks;
		return numBlocksFreed;
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
	public void deallocSDRAM(HasChipLocation chip, int appID, int baseAddress)
			throws IOException, Exception {
		synchronousCall(new SDRAMDeAlloc(chip, appID, baseAddress));
	}

	public int getNumBlocksFreed() {
		return numBlocksFreed;
	}
}
