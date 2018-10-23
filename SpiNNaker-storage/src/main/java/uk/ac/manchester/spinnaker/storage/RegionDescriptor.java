package uk.ac.manchester.spinnaker.storage;

/**
 * A description of information about a particular region.
 *
 * @author Donal Fellows
 */
public class RegionDescriptor {
	public final int baseAddress;
	public final int size;

	public RegionDescriptor(int baseAddress, int size) {
		this.baseAddress = baseAddress;
		this.size = size;
	}
}
