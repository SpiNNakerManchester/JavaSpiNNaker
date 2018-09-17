package uk.ac.manchester.spinnaker.data_spec.exceptions;

/**
 * An exception that indicates that a region has already been allocated.
 */
@SuppressWarnings("serial")
public class RegionInUseException extends DataSpecificationException {
	/**
	 * State that a particular region is in use.
	 *
	 * @param key
	 *            The region key for the region that is in use
	 */
	public RegionInUseException(int key) {
		super("region " + key + " was already allocated");
	}

	/**
	 * State that a particular region is in use.
	 *
	 * @param key
	 *            The region key for the region that is in use
	 * @param label
	 *            The label for the region
	 */
	public RegionInUseException(int key, String label) {
		super("region " + key + " (" + label + ") was already allocated");
	}
}
