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
package uk.ac.manchester.spinnaker.machine;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import uk.ac.manchester.spinnaker.utils.DoubleMapIterator;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * Represents a set of of {@link CoreLocation}s organized by Chip.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/core_subsets.py">
 *      Python Version</a>
 * @author Christian-B
 */
public class CoreSubsets implements MappableIterable<CoreLocation> {
	private final Map<ChipLocation, Map<Integer, CoreLocation>> locations;

	private boolean immutable;

	/**
	 * Bases constructor which creates an empty set of CoreSubset(s).
	 */
	public CoreSubsets() {
		locations = new TreeMap<>();
		immutable = false;
	}

	/**
	 * Constructor which adds a single location.
	 *
	 * @param location
	 *            The location of the processor to add.
	 */
	public CoreSubsets(HasCoreLocation location) {
		this();
		addCore(location.asCoreLocation());
	}

	/**
	 * Constructor which adds the locations.
	 *
	 * @param locations
	 *            The location of all processors to add.
	 */
	public CoreSubsets(Iterable<CoreLocation> locations) {
		this();
		addCores(locations);
	}

	/**
	 * Adds the Core Location.
	 * <p>
	 * This method uses set semantics so attempts to add a Core/Processor that
	 * is already in the subset are silently ignored.
	 *
	 * @param core
	 *            Location (x, y, p) to add.
	 * @throws IllegalStateException
	 *             If the subsets have been set immutable. For example because a
	 *             hashcode has been generated,
	 */
	public void addCore(CoreLocation core) {
		if (immutable) {
			throw new IllegalStateException("The subsets is immutable. "
					+ "Possibly because a hashcode has been generated.");
		}
		getOrCreate(core.asChipLocation()).put(core.getP(), core);
	}

	/**
	 * Adds the Core Location, creating a new subset if required.
	 * <p>
	 * This method uses set semantics so attempts to add a Core/Processor that
	 * is already in the subset are silently ignored.
	 *
	 * @param x
	 *            x coordinate of chip
	 * @param y
	 *            y coordinate of chip
	 * @param p
	 *            p coordinate/ processor id
	 * @throws IllegalStateException
	 *             If the subsets have been set immutable. For example because a
	 *             hashcode has been generated,
	 */
	public void addCore(int x, int y, int p) {
		addCore(new ChipLocation(x, y), p);
	}

	/**
	 * Adds the processor for this chip, creating a new subset if required.
	 * <p>
	 * This method uses set semantics so attempts to add a Core/Processor that
	 * is already in the subset are silently ignored.
	 *
	 * @param chip
	 *            Chip key of CoreSubset to add to.
	 * @param p
	 *            p coordinate/ processor id.
	 * @throws IllegalStateException
	 *             If the subsets have been set immutable. For example because a
	 *             hashcode has been generated,
	 */
	public void addCore(ChipLocation chip, int p) {
		if (immutable) {
			throw new IllegalStateException("The subsets is immutable. "
					+ "Possibly because a hashcode has been generated.");
		}
		getOrCreate(chip).put(p, new CoreLocation(chip, p));
	}

	/**
	 * Adds the processors for this chip, creating a new subset if required.
	 * <p>
	 * This method uses set semantics so attempts to add a Core/Processor that
	 * is already in the subset are silently ignored.
	 *
	 * @param chip
	 *            Chip key of CoreSubset to add to.
	 * @param processors
	 *            p coordinates/ processor IDs.
	 * @throws IllegalStateException
	 *             If the subsets have been set immutable. For example because a
	 *             hashcode has been generated,
	 */
	public void addCores(ChipLocation chip, Iterable<Integer> processors) {
		if (immutable) {
			throw new IllegalStateException("The subsets is immutable. "
					+ "Possibly because a hashcode has been generated.");
		}
		Map<Integer, CoreLocation> map = getOrCreate(chip);
		for (Integer p : processors) {
			map.put(p, new CoreLocation(chip, p));
		}
	}

	/**
	 * Adds the processors for this chip, creating a new subset if required.
	 * <p>
	 * This method uses set semantics so attempts to add a Core/Processor that
	 * is already in the subset are silently ignored.
	 *
	 * @param x
	 *            x coordinate of chip
	 * @param y
	 *            y coordinate of chip
	 * @param processors
	 *            p coordinates/ processor IDs.
	 * @throws IllegalStateException
	 *             If the subsets have been set immutable. For example because a
	 *             hashcode has been generated,
	 */
	public void addCores(int x, int y, Iterable<Integer> processors) {
		addCores(new ChipLocation(x, y), processors);
	}

	/**
	 * Adds the locations into this one.
	 * <p>
	 * This method uses set semantics so attempts to add a Core/Processor that
	 * is already in the subset are silently ignored.
	 *
	 * @param locations
	 *            the locations to add.
	 */
	public void addCores(Iterable<CoreLocation> locations) {
		for (CoreLocation location : locations) {
			addCore(location);
		}
	}

	/**
	 * Obtain the CoreSubset for this Chip.
	 *
	 * @param chip
	 *            Coordinates of a chip
	 * @return The core subset of a chip or {@code null} if there is no subset.
	 */
	private Map<Integer, CoreLocation> getOrCreate(ChipLocation chip) {
		return locations.computeIfAbsent(chip, k -> new TreeMap<>());
	}

	/**
	 * The total number of processors that are in these core subsets.
	 *
	 * @return The sum of the individual CoreSubset sizes.
	 */
	public int size() {
		return locations.values().stream().mapToInt(Map::size).sum();
	}

	/**
	 * Whether there are any processors in these core subsets.
	 *
	 * @return {@code true} when the core subsets are empty.
	 */
	public boolean isEmpty() {
		return locations.values().stream().allMatch(Map::isEmpty);
	}

	/**
	 * Determine if the chip with coordinates (x, y) is in the subset.
	 * <p>
	 * Note: An empty subset mapped to the Chip is ignored.
	 *
	 * @param chip
	 *            Coordinates to check
	 * @return True if and only if there is a none empty Subset for this Chip.
	 */
	public boolean isChip(ChipLocation chip) {
		return !locations.getOrDefault(chip, emptyMap()).isEmpty();
	}

	/**
	 * Determine if there is a chip with coordinates (x, y) in the subset, which
	 * has a core with the given id in the subset.
	 *
	 * @param core
	 *            x, y and p coordinates
	 * @return True if and only if there is a core with these coordinates
	 */
	public boolean isCore(CoreLocation core) {
		return locations.getOrDefault(core.asChipLocation(), emptyMap())
				.containsValue(core);
	}

	/**
	 * Generate a hashcode for these subsets.
	 * <p>
	 * Two CoreSubsets that have the same subsets (and are therefore considered
	 * equals) will generate the same hashcode.
	 * <p>
	 * To guarantee consistency over time, once a hashcode is requested the
	 * CoreSubsets and all its subsets will be made immutable and any further
	 * add calls will raise an exception.
	 *
	 * @return integer to use as the hashcode.
	 */
	@Override
	public final int hashCode() {
		immutable = true;
		int hash = 7;
		for (Map<Integer, CoreLocation> subset : locations.values()) {
			for (CoreLocation location : subset.values()) {
				hash = 89 * hash + location.hashCode();
			}
		}
		return hash;
	}

	/**
	 * Indicates whether some other object is "equal to" this one. It is
	 * reflexive, symmetric and transitive. It is consistent provided no core or
	 * subset has been added.
	 * <p>
	 * Unlike {@link #hashCode()}, a call to equals does <em>not</em> effect
	 * mutability.
	 *
	 * @param obj
	 *            Other Object to compare to.
	 * @return True if and only if {@code obj} is another CoreSubsets with
	 *         exactly the same subsets.
	 */
	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (isNull(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final CoreSubsets other = (CoreSubsets) obj;
		return Objects.equals(this.locations, other.locations);
	}

	@Override
	public String toString() {
		return locations.toString();
	}

	/**
	 * Returns a new CoreSubsets which is an intersect of this and the other.
	 *
	 * @param other
	 *            A second CoreSubsets with possibly overlapping cores.
	 * @return A new CoreSubsets object with only the cores present in both.
	 *         Therefore the result may be empty.
	 */
	public CoreSubsets intersection(CoreSubsets other) {
		CoreSubsets results = new CoreSubsets();
		locations.forEach((chip, locs) -> {
			Map<?, CoreLocation> otherSubset = other.locations.get(chip);
			if (nonNull(otherSubset)) {
				locs.forEach((ignored, location) -> {
					if (otherSubset.containsValue(location)) {
						results.addCore(location);
					}
				});
			}
		});
		return results;
	}

	/**
	 * Returns the ChipLocations for which there is at least one CoreLocations
	 * in the Subsets.
	 * <p>
	 * The order of the locations is guaranteed to be the natural order.
	 *
	 * @return An ordered set of chips.
	 */
	public Set<ChipLocation> getChips() {
		return unmodifiableSet(locations.keySet());
	}

	@Override
	public Iterator<CoreLocation> iterator() {
		return new DoubleMapIterator<>(locations);
	}

	/**
	 * Provides the CoreLocations for just a single Chip.
	 * <p>
	 * This will be an empty list when {@link #isChip(ChipLocation)} returns
	 * {@code false}.
	 *
	 * @param chip
	 *            coordinates of the chip
	 * @return Unmodifiable (possibly empty) collection of CoreLocation
	 */
	public Collection<CoreLocation> coreByChip(ChipLocation chip) {
		if (locations.containsKey(chip)) {
			return unmodifiableCollection(locations.get(chip).values());
		} else {
			return emptyList();
		}
	}

	/**
	 * Provides the CoreLocations for just a single Chip.
	 * <p>
	 * This will be an empty list when {@link #isChip(ChipLocation)} returns
	 * {@code false}.
	 *
	 * @param chip
	 *            coordinates of the chip
	 * @return Unmodifiable (possibly empty) collection of CoreLocation
	 */
	public Set<Integer> pByChip(ChipLocation chip) {
		if (locations.containsKey(chip)) {
			return unmodifiableSet(locations.get(chip).keySet());
		} else {
			return emptySet();
		}
	}
}
