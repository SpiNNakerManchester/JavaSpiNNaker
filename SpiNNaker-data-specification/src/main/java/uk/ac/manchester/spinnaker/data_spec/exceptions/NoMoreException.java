package uk.ac.manchester.spinnaker.data_spec.exceptions;

/**
 * An exception that indicates that there is no more space for the requested
 * item.
 */
@SuppressWarnings("serial")
public class NoMoreException extends DataSpecificationException {

	public NoMoreException(int remainingSpace, int length, int currentRegion) {
		super("Space unavailable to write all the elements requested by the "
				+ "write operation. Space available: " + remainingSpace
				+ "; space requested: " + length + " for region "
				+ currentRegion + ".");
	}

}
