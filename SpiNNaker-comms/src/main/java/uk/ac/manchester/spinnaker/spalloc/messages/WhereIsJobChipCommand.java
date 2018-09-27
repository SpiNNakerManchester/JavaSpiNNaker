package uk.ac.manchester.spinnaker.spalloc.messages;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * Request to get the location of a chip in a job's allocation relative to a
 * machine.
 */
public class WhereIsJobChipCommand extends Command<Integer> {
	/**
	 * Create a request to locate a chip within a job's allocation.
	 *
	 * @param jobId
	 *            The job to request about.
	 * @param chip
	 *            The coordinates of the chip to ask about.
	 */
	public WhereIsJobChipCommand(int jobId, HasChipLocation chip) {
		super("where_is");
		addKwArg("job_id", jobId);
		addKwArg("chip_x", chip.getX());
		addKwArg("chip_y", chip.getY());
	}
}
