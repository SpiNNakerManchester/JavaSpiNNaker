/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.machine;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import javax.validation.Valid;

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
public final class CoreSubsets implements MappableIterable<CoreLocation> {
	private final Map<@Valid ChipLocation,
			Map<@ValidP Integer, @Valid CoreLocation>> locations;

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
		var map = getOrCreate(chip);
		for (var p : processors) {
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
		for (var location : locations) {
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
		return locations.computeIfAbsent(chip, __ -> new TreeMap<>());
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
		return !locations.getOrDefault(chip, Map.of()).isEmpty();
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
		return locations.getOrDefault(core.asChipLocation(), Map.of())
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
	public int hashCode() {
		immutable = true;
		int hash = 7;
		for (var subset : locations.values()) {
			for (var location : subset.values()) {
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
	public boolean equals(Object obj) {
		if (obj instanceof CoreSubsets) {
			var other = (CoreSubsets) obj;
			return Objects.equals(locations, other.locations);
		}
		return false;
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
		var results = new CoreSubsets();
		locations.forEach((chip, locs) -> {
			var otherSubset = other.locations.get(chip);
			if (otherSubset != null) {
				locs.forEach((__, location) -> {
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
		var l = locations.get(chip);
		if (l != null) {
			return unmodifiableCollection(l.values());
		} else {
			return List.of();
		}
	}

	/**
	 * Provides the processor identifiers for just a single Chip.
	 * <p>
	 * This will be an empty list when {@link #isChip(ChipLocation)} returns
	 * {@code false}.
	 *
	 * @param chip
	 *            coordinates of the chip
	 * @return Unmodifiable (possibly empty) collection of processor identifiers
	 */
	public Set<Integer> pByChip(ChipLocation chip) {
		var l = locations.get(chip);
		if (l != null) {
			return unmodifiableSet(l.keySet());
		} else {
			return Set.of();
		}
	}
}
