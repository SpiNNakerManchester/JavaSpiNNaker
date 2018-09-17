package uk.ac.manchester.spinnaker.data_spec.exceptions;

import uk.ac.manchester.spinnaker.data_spec.Commands;

/**
 * An exception which occurs when trying to write to an unallocated region of
 * memory.
 */
@SuppressWarnings("serial")
public class RegionNotAllocatedException extends DataSpecificationException {
	/**
	 * Create an instance.
	 *
	 * @param currentRegion
	 *            What is the current region.
	 * @param command
	 *            What command was trying to use the region.
	 */
	public RegionNotAllocatedException(int currentRegion, Commands command) {
		super("Region " + currentRegion
				+ " has not been allocated during execution of command "
				+ command);
	}
}
