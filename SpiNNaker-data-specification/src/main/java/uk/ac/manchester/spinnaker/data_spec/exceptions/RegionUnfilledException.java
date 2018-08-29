package uk.ac.manchester.spinnaker.data_spec.exceptions;

import uk.ac.manchester.spinnaker.data_spec.Commands;

@SuppressWarnings("serial")
public class RegionUnfilledException extends DataSpecificationException {

	public RegionUnfilledException(int region, Commands command) {
		super("Region " + region + " was requested unfilled, but command "
				+ command + " requests its use");
	}

}
