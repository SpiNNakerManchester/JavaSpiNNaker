package uk.ac.manchester.spinnaker.data_spec;

import static java.lang.System.arraycopy;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.stream.IntStream.range;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;

import uk.ac.manchester.spinnaker.data_spec.exceptions.RegionInUseException;

/**
 * A collection of memory regions. Note that the collection cannot be modified
 * by the standard collection API; those modification operations will fail. The
 * {@link #set(int,MemoryRegion) set(...)} operation works.
 *
 * @author Donal Fellows
 */
public final class MemoryRegionCollection implements Collection<MemoryRegion> {
	private final MemoryRegion[] regions;

	public MemoryRegionCollection(int numRegions) {
		regions = new MemoryRegion[numRegions];
	}

	@Override
	public int size() {
		return regions.length;
	}

	@Override
	public boolean isEmpty() {
		return regions.length == 0;
	}

	public MemoryRegion get(int regionID) {
		return regions[regionID];
	}

	public void set(int regionID, MemoryRegion region)
			throws RegionInUseException {
		if (regions[regionID] != null) {
			throw new RegionInUseException(regionID);
		}
		regions[regionID] = region;
	}

	public boolean isEmpty(int regionID) {
		return regions[regionID] == null;
	}

	public boolean isUnfilled(int regionID) {
		return isEmpty() || regions[regionID].isUnfilled();
	}

	public int countUsedRegions() {
		return (int) stream().filter(r -> r != null).count();
	}

	public boolean needsToWriteRegion(int regionID) {
		if (regionID >= regions.length) {
			throw new IllegalArgumentException(
					"the region ID requested is beyond the supported number "
							+ "of available region IDs");
		}
		if (!isUnfilled(regionID)) {
			return true;
		}
		return range(regionID, regions.length).anyMatch(id -> !isUnfilled(id));
	}

	@Override
	public boolean contains(Object o) {
		return stream().anyMatch(r -> r.equals(o));
	}

	@Override
	public Iterator<MemoryRegion> iterator() {
		return asList(regions).iterator();
	}

	@Override
	public Object[] toArray() {
		Object[] objs = new Object[regions.length];
		arraycopy(regions, 0, objs, 0, objs.length);
		return objs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		if (a.length < regions.length) {
			// This line of code copied from ArrayList
			return (T[]) copyOf(regions, regions.length, a.getClass());
		}
		arraycopy(regions, 0, a, 0, regions.length);
		if (a.length > regions.length) {
			a[regions.length] = null;
		}
		return a;
	}

	@Override
	public Stream<MemoryRegion> stream() {
		return Arrays.stream(regions);
	}

	@Override
	public Spliterator<MemoryRegion> spliterator() {
		return Arrays.spliterator(regions);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (MemoryRegion r : regions) {
			if (!c.contains(r)) {
				return false;
			}
		}
		return true;
	}

	@Override
	@Deprecated
	public boolean add(MemoryRegion e) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public boolean addAll(Collection<? extends MemoryRegion> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void clear() {
		throw new UnsupportedOperationException();
	}
}
