package uk.ac.manchester.spinnaker.spalloc.messages;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * Request to get the location of a chip in a machine.
 */
public class WhereIsMachineChipCommand extends Command<Integer> {
	/**
	 * Create a request to locate a chip on a machine.
	 *
	 * @param machine
	 *            The machine to request about.
	 * @param chip
	 *            The coordinates of the chip to ask about.
	 */
	public WhereIsMachineChipCommand(String machine, HasChipLocation chip) {
		super("where_is");
		addKwArg("machine", machine);
		addKwArg("chip_x", chip.getX());
		addKwArg("chip_y", chip.getY());
	}
}
