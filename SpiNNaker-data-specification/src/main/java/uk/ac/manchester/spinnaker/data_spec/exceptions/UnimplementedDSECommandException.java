package uk.ac.manchester.spinnaker.data_spec.exceptions;

import uk.ac.manchester.spinnaker.data_spec.Commands;

/**
 * An exception which occurs when trying to execute an unimplemented command.
 */
@SuppressWarnings("serial")
public class UnimplementedDSECommandException
		extends UnsupportedOperationException {
	public UnimplementedDSECommandException(Commands command) {
		super(String.format("Command %s in the data specification executor "
				+ "has not yet been implemented", command));
	}
}
