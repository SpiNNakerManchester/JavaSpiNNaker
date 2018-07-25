package uk.ac.manchester.spinnaker.selectors;

import uk.ac.manchester.spinnaker.machine.Machine;

/**
 * This indicates a class that can be told about the machine.
 */
public interface MachineAware {
	Machine getMachine();

	void setMachine(Machine machine);
}
