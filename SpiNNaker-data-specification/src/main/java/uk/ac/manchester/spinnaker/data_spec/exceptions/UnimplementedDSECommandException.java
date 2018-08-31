package uk.ac.manchester.spinnaker.data_spec.exceptions;

import static java.lang.String.format;

import uk.ac.manchester.spinnaker.data_spec.Commands;

/**
 * An exception which occurs when trying to execute an unimplemented command.
 */
@SuppressWarnings("serial")
public class UnimplementedDSECommandException
		extends UnsupportedOperationException {
	/**
	 * Create an instance.
	 *
	 * @param command
	 *            The command that was unimplemented.
	 */
	public UnimplementedDSECommandException(Commands command) {
		super(format("Command %s in the data specification executor "
				+ "has not yet been implemented", command));
	}
}
