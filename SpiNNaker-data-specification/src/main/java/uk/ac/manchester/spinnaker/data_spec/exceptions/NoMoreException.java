package uk.ac.manchester.spinnaker.data_spec.exceptions;

/**
 * An exception that indicates that there is no more space for the requested
 * item.
 */
@SuppressWarnings("serial")
public class NoMoreException extends DataSpecificationException {
	/**
	 * Create an instance.
	 *
	 * @param remainingSpace
	 *            How much space is available
	 * @param length
	 *            How much space was asked for
	 * @param currentRegion
	 *            What region are we talking about
	 */
	public NoMoreException(int remainingSpace, int length, int currentRegion) {
		super("Space unavailable to write all the elements requested by the "
				+ "write operation. Space available: " + remainingSpace
				+ "; space requested: " + length + " for region "
				+ currentRegion + ".");
	}
}
