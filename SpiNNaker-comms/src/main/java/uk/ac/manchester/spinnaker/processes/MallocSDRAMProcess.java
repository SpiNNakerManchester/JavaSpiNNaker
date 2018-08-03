package uk.ac.manchester.spinnaker.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.SDRAMAlloc;

/** A process for allocating a block of SDRAM on a SpiNNaker chip. */
public class MallocSDRAMProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 */
	public MallocSDRAMProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
	}

	/**
	 * Allocate space in the SDRAM space.
	 *
	 * @param chip
	 *            On what chip to allocate.
	 * @param size
	 *            How many bytes to allocate.
	 * @param appID
	 *            What app will own the allocation.
	 * @return Where the start of the allocated memory is.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects the message.
	 */
	public int mallocSDRAM(HasChipLocation chip, int size, int appID)
			throws IOException, Exception {
		return synchronousCall(new SDRAMAlloc(chip, appID, size)).baseAddress;
	}

	/**
	 * Allocate space in the SDRAM space.
	 *
	 * @param chip
	 *            On what chip to allocate.
	 * @param size
	 *            How many bytes to allocate.
	 * @param appID
	 *            What app will own the allocation.
	 * @param tag
	 *            the tag for the SDRAM, a 8-bit (chip-wide) tag that can be
	 *            looked up by a SpiNNaker application to discover the address
	 *            of the allocated block
	 * @return Where the start of the allocated memory is.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects the message.
	 */
	public int mallocSDRAM(HasChipLocation chip, int size, int appID, int tag)
			throws IOException, Exception {
		return synchronousCall(
				new SDRAMAlloc(chip, appID, size, tag)).baseAddress;
	}
}
