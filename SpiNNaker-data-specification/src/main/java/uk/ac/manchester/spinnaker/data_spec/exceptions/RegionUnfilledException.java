package uk.ac.manchester.spinnaker.data_spec.exceptions;

import uk.ac.manchester.spinnaker.data_spec.Commands;

/**
 * An exception that indicates that a memory region is being used that was
 * originally requested to be unfilled.
 */
@SuppressWarnings("serial")
public class RegionUnfilledException extends DataSpecificationException {
	/**
	 * Create an instance.
	 *
	 * @param region
	 *            What region are we talking about.
	 * @param command
	 *            What command wanted to use the region.
	 */
	public RegionUnfilledException(int region, Commands command) {
		super("Region " + region + " was requested unfilled, but command "
				+ command + " requests its use");
	}
}
