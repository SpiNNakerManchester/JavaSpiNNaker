/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.data_spec;

import static java.lang.System.arraycopy;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.Objects.requireNonNull;
import static java.util.stream.IntStream.range;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;

/**
 * A collection of memory regions. Note that the collection cannot be modified
 * by the standard collection API; those modification operations will fail. The
 * {@link #set(MemoryRegion) set(...)} operation works.
 *
 * @author Donal Fellows
 */
public final class MemoryRegionCollection implements Collection<MemoryRegion> {
	private final MemoryRegion[] regions;

	/**
	 * Allocate a new memory region collection. The regions default to being
	 * empty.
	 *
	 * @param numRegions
	 *            The number of memory regions possible in the collection.
	 */
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

	/**
	 * Get the region with the given ID.
	 *
	 * @param regionID
	 *            The region ID to retrieve.
	 * @return The memory region, or {@code null} if that region is empty.
	 */
	public MemoryRegion get(int regionID) {
		return regions[regionID];
	}

	/**
	 * Set the region with the given ID.
	 *
	 * @param region
	 *            The region to store. Regions know their ID/index.
	 * @throws RegionInUseException
	 *             if the region has already been set to a non-empty region.
	 */
	void set(MemoryRegion region)
			throws RegionInUseException {
		if (!isEmpty(region.getIndex())) {
			throw new RegionInUseException(region.getIndex());
		}
		regions[region.getIndex()] =
            requireNonNull(region, "must not set an empty region");
	}

	/**
	 * Test whether a given region is empty.
	 *
	 * @param regionID
	 *            The ID of the region to test.
	 * @return True exactly when the region is empty.
	 */
	public boolean isEmpty(int regionID) {
		return isEmpty() || regions[regionID] == null;
	}

	/**
	 * Test whether a given region is unfilled.
	 *
	 * @param regionID
	 *            The ID of the region to test.
	 * @return True exactly when the region is unfilled. Empty regions are
	 *         always unfilled.
	 */
	public boolean isUnfilled(int regionID) {
		return isEmpty(regionID) || regions[regionID].isUnfilled();
	}

	/**
	 * Get the size of a particular region.
	 *
	 * @param regionID
	 *            The ID of the region to get the size of.
	 * @return The size of the region. Empty regions have zero size.
	 */
	public int getSize(int regionID) {
		return regions[regionID] == null ? 0
            : regions[regionID].getAllocatedSize();
	}

	/**
	 * Get the number of non-empty regions.
	 *
	 * @return How many regions have been set.
	 */
	public int countUsedRegions() {
		return (int) stream().filter(r -> r != null).count();
	}

	/**
	 * Get whether a particular region needs to be written.
	 *
	 * @param regionID
	 *            The ID of the region to check.
	 * @return True if the region must be written. A region must be written if
	 *         it is filled or if it has a region after it that must be written.
	 * @throws IllegalArgumentException
	 *             If a bad region ID is given
	 */
	public boolean needsToWriteRegion(int regionID) {
		if (regionID < 0 || regionID >= regions.length) {
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
		return stream().allMatch(c::contains);
	}

	/**
	 * <i>This method is unsupported, as this collection does not support
	 * size-varying operations.</i>
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated
	public boolean add(MemoryRegion e) {
		throw new UnsupportedOperationException();
	}

	/**
	 * <i>This method is unsupported, as this collection does not support
	 * size-varying operations.</i>
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	/**
	 * <i>This method is unsupported, as this collection does not support
	 * size-varying operations.</i>
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated
	public boolean addAll(Collection<? extends MemoryRegion> c) {
		throw new UnsupportedOperationException();
	}

	/**
	 * <i>This method is unsupported, as this collection does not support
	 * size-varying operations.</i>
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	/**
	 * <i>This method is unsupported, as this collection does not support
	 * size-varying operations.</i>
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	/**
	 * <i>This method is unsupported, as this collection does not support
	 * size-varying operations.</i>
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object o) {
		Object[] a;
		if (o instanceof MemoryRegionCollection) {
			a = ((MemoryRegionCollection) o).regions;
		} else if (o instanceof Collection) {
			a = ((Collection<?>) o).toArray();
		} else if (o instanceof MemoryRegion[]) {
			a = (MemoryRegion[]) o;
		} else {
			return false;
		}
		return Arrays.equals(regions, a);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(regions);
	}
}
