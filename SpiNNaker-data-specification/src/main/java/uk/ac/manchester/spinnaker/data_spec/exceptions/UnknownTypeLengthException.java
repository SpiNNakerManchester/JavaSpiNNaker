package uk.ac.manchester.spinnaker.data_spec.exceptions;

import uk.ac.manchester.spinnaker.data_spec.Commands;

/**
 * An exception that indicates that the value of the requested type is unknown.
 */
@SuppressWarnings("serial")
public class UnknownTypeLengthException extends DataSpecificationException {

	public UnknownTypeLengthException(int dataLen, Commands command) {
		super("Unknown data length " + dataLen + " during command " + command);
	}

}
