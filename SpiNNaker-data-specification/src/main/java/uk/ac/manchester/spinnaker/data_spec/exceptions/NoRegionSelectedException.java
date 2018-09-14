package uk.ac.manchester.spinnaker.data_spec.exceptions;

import uk.ac.manchester.spinnaker.data_spec.Commands;

/**
 * An exception that indicates that a memory region has not been selected.
 */
@SuppressWarnings("serial")
public class NoRegionSelectedException extends DataSpecificationException {
	/**
	 * Create an instance.
	 *
	 * @param msg
	 *            The message in the exception.
	 */
	public NoRegionSelectedException(String msg) {
		super(msg);
	}

	/**
	 * Create an instance.
	 *
	 * @param command
	 *            What command was using memory without a region selected.
	 */
	public NoRegionSelectedException(Commands command) {
		super("no region has been selected for writing by " + command);
	}
}
