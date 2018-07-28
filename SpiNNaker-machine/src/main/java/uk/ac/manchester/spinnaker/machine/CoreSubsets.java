/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import uk.ac.manchester.spinnaker.utils.DoubleMapIterator;

/**
 * Represents a set of of CoreLocations organized by Chip.
 * <p>
 * @see <a
 * href="https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/core_subsets.py">
 * Python Version</a>
 *
 * @author Christian-B
 */
public class CoreSubsets implements Iterable<CoreLocation> {

    private final TreeMap<ChipLocation,
            TreeMap<Integer, CoreLocation>> locations;
    private boolean immutable;

    /**
     * Bases constructor which creates an empty set of CoreSubset(s).
     */
    public CoreSubsets() {
        locations = new TreeMap<>();
        immutable = false;
    }

    /**
     * Constructor which adds the locations.
     *
     * @param locations The location of all processors to add.
     */
    public CoreSubsets(Iterable<CoreLocation> locations) {
        this();
        addCores(locations);
    }

    /**
     * Adds the Core Location.
     * <p>
     * This method uses set semantics so attempts to add a Core/Processor that
     *      is already in the subset are silently ignored.
     * <p>
     * @param core Location (x, y, p) to add.
     * @throws IllegalStateException If the subsets have been set immutable.
     *      For example because a hashcode has been generated,
     */
    public void addCore(CoreLocation core) {
        if (immutable) {
            throw new IllegalStateException("The subsets is immutable. "
                    + "Possibly because a hashcode has been generated.");
        }
        ChipLocation chip = core.asChipLocation();
        TreeMap<Integer, CoreLocation> subset = getOrCreate(chip);
        subset.put(core.getP(), core);
    }

    /**
     * Adds the Core Location, creating a new subset if required.
     * <p>
     * This method uses set semantics so attempts to add a Core/Processor that
     *      is already in the subset are silently ignored.
     *
     * @param x x coordinate of chip
     * @param y y coordinate of chip
     * @param p p coordinate/ processor id
     * @throws IllegalStateException If the subsets have been set immutable.
     *      For example because a hashcode has been generated,
     */
    public void addCore(int x, int y, int p) {
        ChipLocation chip = new ChipLocation(x, y);
        addCore(chip, p);
    }

    /**
     * Adds the processor for this chip, creating a new subset if required.
     * <p>
     * This method uses set semantics so attempts to add a Core/Processor that
     *      is already in the subset are silently ignored.
     *
     * @param chip Chip key of CoreSubset to add to.
     * @param p p coordinate/ processor id.
     * @throws IllegalStateException If the subsets have been set immutable.
     *      For example because a hashcode has been generated,
     */
    public void addCore(ChipLocation chip, int p) {
        if (immutable) {
            throw new IllegalStateException("The subsets is immutable. "
                    + "Possibly because a hashcode has been generated.");
        }
        CoreLocation core = new CoreLocation(chip, p);
        TreeMap<Integer, CoreLocation> subset = getOrCreate(chip);
        subset.put(p, core);
    }

    /**
     * Adds the processors for this chip, creating a new subset if required.
     * <p>
     * This method uses set semantics so attempts to add a Core/Processor that
     *      is already in the subset are silently ignored.
     *
     * @param chip Chip key of CoreSubset to add to.
     * @param processors p coordinates/ processor ids.
     * @throws IllegalStateException If the subsets have been set immutable.
     *      For example because a hashcode has been generated,
     */
    public void addCores(ChipLocation chip, Iterable<Integer> processors) {
        if (immutable) {
            throw new IllegalStateException("The subsets is immutable. "
                    + "Possibly because a hashcode has been generated.");
        }
        for (Integer p: processors) {
            CoreLocation core = new CoreLocation(chip, p);
            TreeMap<Integer, CoreLocation> subset = getOrCreate(chip);
            subset.put(p, core);
        }
    }

    /**
     * Adds the processors for this chip, creating a new subset if required.
     * <p>
     * This method uses set semantics so attempts to add a Core/Processor that
     *      is already in the subset are silently ignored.
     *
     * @param x x coordinate of chip
     * @param y y coordinate of chip
     * @param processors p coordinates/ processor ids.
     * @throws IllegalStateException If the subsets have been set immutable.
     *      For example because a hashcode has been generated,
     */
    public void addCores(int x, int y, Iterable<Integer> processors) {
        ChipLocation chip = new ChipLocation(x, y);
        addCores(chip, processors);
    }

    /**
     * Adds the locations into this one.
     * <p>
     * This method uses set semantics so attempts to add a Core/Processor that
     *      is already in the subset are silently ignored.
     *
     * @param locations the locations to add.
     */
    public void addCores(Iterable<CoreLocation> locations) {
        for (CoreLocation location:locations) {
            addCore(location);
        }
    }

    /**
     * Obtain the CoreSubset for this Chip.
     *
     * @param chip Coordinates of a chip
     * @return The core subset of a chip or null if there is no subset.
     */
    private TreeMap<Integer, CoreLocation> getOrCreate(ChipLocation chip) {
        if (!locations.containsKey(chip)) {
            locations.put(chip, new TreeMap<>());
        }
        return locations.get(chip);
    }

    /**
     * The total number of processors that are in these core subsets.
     *
     * @return The sum of the individual CoreSubset sizes.
     */
    public int size() {
        int count = 0;
        for (TreeMap<Integer, CoreLocation> subset:locations.values()) {
            count += subset.size();
        }
        return count;
    }

     /**
     * Determine if the chip with coordinates (x, y) is in the subset.
     * <p>
     * Note: An empty subset mapped to the Chip is ignored.
     *
     * @param chip x and y Coordinates to check
     * @return True if and only if there is a none empty Subset for this Chip.
     */
    public boolean isChip(ChipLocation chip) {
        if (locations.containsKey(chip)) {
            return !locations.get(chip).isEmpty();
        } else {
            return false;
        }
    }

    /**
     * Determine if there is a chip with coordinates (x, y) in the subset,
     *      which has a core with the given id in the subset.
     * @param core x, y and p coordinates
     * @return True if and only if there is a core with these coordinates
     */
    public boolean isCore(CoreLocation core) {
        TreeMap<Integer, CoreLocation> subset = locations.get(
                core.asChipLocation());
        if (subset == null) {
            return false;
        } else {
            return subset.containsValue(core);
        }
    }

    @Override
    /**
     * Generate a hashcode for these subsets.
     * <p>
     * Two CoreSubsets that have the same subsets
     *      (and are therefor considered equals)
     *      will generate the same hashcode.
     * <p>
     * To guarantee consistency over time once a hashcode is requested
     *      the CoreSubsets and all its subsets will be made immutable
     *      and any farther add calls will raise an exception.
     * @return interger to use as the hashcode.
     */
    public final int hashCode() {
        immutable = true;
        int hash = 7;
        for (TreeMap<Integer, CoreLocation> subset:locations.values()) {
            for (CoreLocation location:subset.values()) {
                hash = 89 * hash + location.hashCode();
            }
        }
        return hash;
    }

    @Override
    /**
      * Indicates whether some other object is "equal to" this one.
      * It is reflexive, symmetric and transitive.
      * It is consistent provided no core or subset has been added.
      * <p>
      * Unlike hashcode() a call to equals does NOT effect mutability.
      * @param obj Other Object to compare to.
      * @return True if and only if obj is another CoreSubsets
      *      with exactly the same subsets.
     */
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CoreSubsets other = (CoreSubsets) obj;
        if (!Objects.equals(this.locations, other.locations)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return locations.toString();
    }

    /**
     * Returns a new CoreSubsets which is an intersect of this and the other.
     *
     * @param other A second CoreSubsets with possibly overlapping cores.
     * @return A new Coresubsets Objects with only the cores present in both.
     *      Therefor the result may be empty.
     */
    public CoreSubsets intersection(CoreSubsets other) {
        CoreSubsets results = new CoreSubsets();
        for (Entry<ChipLocation, TreeMap<Integer, CoreLocation>> entry
                :locations.entrySet()) {
            if (other.locations.containsKey(entry.getKey())) {
                TreeMap<Integer, CoreLocation> othersubset =
                        other.locations.get(entry.getKey());
                if (othersubset != null) {
                    for (CoreLocation location:entry.getValue().values()) {
                        if (othersubset.containsValue(location)) {
                            results.addCore(location);
                        }
                    }
                }
            }
        }
        return results;
    }

    /**
     * Returns the ChipLocations for which there is at least one CoreLocations
     *      in the Subsets.
     * <p>
     * The order of the locations is guaranteed to be the natural order.

     * @return An ordered set of
     */
    public Set<ChipLocation> getChips() {
        return Collections.unmodifiableSet(locations.keySet());
    }

    @Override
    public Iterator<CoreLocation> iterator() {
        return new DoubleMapIterator<>(locations);
    }

    /**
     * Provides the CoreLocations for just a single Chip.
     * <p>
     * This will be an empty list when isChip(chip) returns false.
     *
     * @param chip x y coordinates
     * @return Unmodifiable (possibly empty) collection of CoreLocation
     */
    public Collection<CoreLocation> coreByChip(ChipLocation chip) {
        if (locations.containsKey(chip)) {
            return Collections.unmodifiableCollection(
                    locations.get(chip).values());
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Provides the CoreLocations for just a single Chip.
     * <p>
     * This will be an empty list when isChip(chip) returns false.
     *
     * @param chip x y coordinates
     * @return Unmodifiable (possibly empty) collection of CoreLocation
     */
    public Set<Integer> pByChip(ChipLocation chip) {
        if (locations.containsKey(chip)) {
            return Collections.unmodifiableSet(
                    locations.get(chip).keySet());
        } else {
            return new HashSet<>();
        }
    }

}
