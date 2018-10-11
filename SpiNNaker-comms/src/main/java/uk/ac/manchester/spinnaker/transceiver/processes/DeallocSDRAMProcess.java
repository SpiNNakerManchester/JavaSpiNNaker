package uk.ac.manchester.spinnaker.transceiver.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.SDRAMDeAlloc;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/** Deallocate space in the SDRAM. */
public class DeallocSDRAMProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public DeallocSDRAMProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Free the memory associated with a given application ID.
	 *
	 * @param chip
	 *            the chip to allocate on
	 * @param appID
	 *            The ID of the application, between 0 and 255
	 * @return the number of blocks freed
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects the message.
	 */
	public int deallocSDRAM(HasChipLocation chip, int appID)
			throws IOException, Exception {
		return synchronousCall(new SDRAMDeAlloc(chip, appID)).numFreedBlocks;
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
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects the message.
	 */
	public void deallocSDRAM(HasChipLocation chip, int appID, int baseAddress)
			throws IOException, Exception {
		synchronousCall(new SDRAMDeAlloc(chip, appID, baseAddress));
	}
}
