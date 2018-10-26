package uk.ac.manchester.spinnaker.storage;

/**
 * A description of information about a particular region.
 *
 * @author Donal Fellows
 */
public class RegionDescriptor {
	/**
	 * The address of the first byte in the memory region.
	 */
	public final int baseAddress;
	/**
	 * The number of (contiguous) bytes in the memory region.
	 */
	public final int size;

	/**
	 * Create a memory region descriptor.
	 *
	 * @param baseAddress
	 *            Where does the region start?
	 * @param size
	 *            How big is the region?
	 */
	public RegionDescriptor(int baseAddress, int size) {
		this.baseAddress = baseAddress;
		this.size = size;
	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof RegionDescriptor) {
			RegionDescriptor d = (RegionDescriptor) o;
			return d.baseAddress == baseAddress && d.size == size;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (37 * baseAddress) ^ (41 * size);
	}

	@Override
	public String toString() {
		return "addr:" + baseAddress + ",size:" + size;
	}
}
