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

	@Override
	public boolean equals(Object o) {
		if (o!=null&&o instanceof RegionDescriptor) {
			RegionDescriptor d = (RegionDescriptor) o;
			return d.baseAddress == baseAddress && d.size==size;
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
