package uk.ac.manchester.spinnaker.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.SDRAMAlloc;

/** A process for allocating a block of SDRAM on a SpiNNaker chip. */
public class MallocSDRAMProcess extends MultiConnectionProcess {
	public MallocSDRAMProcess(ConnectionSelector connectionSelector) {
		super(connectionSelector);
	}

	/** Allocate space in the SDRAM space. */
	public int mallocSDRAM(HasChipLocation chip, int size, int appID, int tag)
			throws IOException, Exception {
		return synchronousCall(new SDRAMAlloc(chip, tag, tag)).baseAddress;
	}
}
