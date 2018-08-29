package uk.ac.manchester.spinnaker.data_spec.exceptions;

import uk.ac.manchester.spinnaker.data_spec.Commands;

@SuppressWarnings("serial")
public class NoRegionSelectedException extends DataSpecificationException {
	public NoRegionSelectedException(String msg) {
		super(msg);
	}

	public NoRegionSelectedException(Commands command) {
		super("no region has been selected for writing by " + command);
	}
}
