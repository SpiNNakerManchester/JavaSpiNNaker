/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Represents a group of CoreSubsets, with a maximum of one per SpiNNaker chip.
 * <p>
 * @see <a
 * href="https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/core_subsets.py">
 * Python Version</a>
 *
 * @author Christian-B
 */
public class CoreSubsets {

    private final TreeMap<ChipLocation, CoreSubset> subsets;
    private boolean immutable;

    /**
     * Bases constructor which creates an empty set of CoreSubset(s).
     */
    public CoreSubsets() {
        subsets = new TreeMap<>();
        immutable = false;
    }

    /**
     * Constructor which adds the subsets.
     *
     * @param subsets The cores for each desired chip.
     */
    public CoreSubsets(Iterable<CoreSubset> subsets) {
        this();
        for (CoreSubset subset:subsets) {
            addSubset(subset);
        }
    }

    /**
     * Adds the Core Location, creating a new subset if required.
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
        ChipLocation chip = new ChipLocation(core.getX(), core.getY());
        CoreSubset subset = getOrCreate(chip);
        subset.addCore(core);
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
        CoreSubset subset = getOrCreate(chip);
        subset.addCore(p);
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
        if (processors.iterator().hasNext()) {
            CoreSubset subset = getOrCreate(chip);
            subset.addCores(processors);
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
     * Merges a core subset into this one.
     * <p>
     * This method uses set semantics so attempts to add a Core/Processor that
     *      is already in the subset are silently ignored.
     *
     * @param subset the core subsets to add.
     */
    public void addSubset(CoreSubset subset) {
        if (immutable) {
            throw new IllegalStateException("The subsets is immutable. "
                    + "Possibly because a hashcode has been generated.");
        }
        if (subset.size() > 0) {
            if (subsets.containsKey(subset.chip)) {
                subsets.get(subset.chip).addCores(subset.processors());
            } else {
                subsets.put(subset.chip, subset);
            }
        }
    }

    /**
     * Obtain the CoreSubset for this Chip.
     *
     * @param chip Coordinates of a chip
     * @return The core subset of a chip or null if there is no subset.
     */
    private CoreSubset getOrCreate(ChipLocation chip) {
        if (!subsets.containsKey(chip)) {
            subsets.put(chip, new CoreSubset(chip));
        }
        return subsets.get(chip);
    }

    /**
     * Obtain the CoreSubset for this Chip.
     * <p>
     * @param chip Coordinates of a chip
     * @return The core subset of a chip or null if there is no subset.
     */
    public CoreSubset getCoreSubset(ChipLocation chip) {
       return subsets.get(chip);
    }

    /**
     * Obtain the locations for this Chip
     * <p>
     * The Collection will be unmodifiable and possibly empty.
     *
     * @param chip Coordinates of a chip
     * @return The core subset of a chip, which will be empty if not added.
     */
    public Collection<CoreLocation> coreLocations(ChipLocation chip) {
        CoreSubset subset = subsets.get(chip);
        if (subset == null) {
            return new ArrayList<CoreLocation>();
        } else {
            return subset.coreLocations();
        }
    }

    /**
     * The one-per-chip subsets.
     * <p>
     * The Collection will be unmodifiable and possibly empty.
     *
     * @return The added Subsets.
     */
    public Collection<CoreSubset> coreSubsets() {
        return Collections.unmodifiableCollection(subsets.values());
    }

    /**
     * The total number of processors that are in these core subsets.
     *
     * @return The sum of the individual CoreSubset sizes.
     */
    public int size() {
        int result = 0;
        for (CoreSubset subset:subsets.values()) {
            result += subset.size();
        }
        return result;
    }

    /**
     * All the core locations for all the chips.
     * <p>
     * The behaviour if subsets or cores are added during iteration is not
     * determined.
     *
     * @return An Iterable of the locations.
     */
    public Iterable<CoreLocation> coreIterable() {

        return new Iterable<CoreLocation>() {
            @Override
            public Iterator<CoreLocation> iterator() {
                return new CoreIterator(subsets.values().iterator());
            }
        };
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
        CoreSubset subset = subsets.get(chip);
        if (subset == null) {
            return false;
        } else {
            return subset.size() > 0;
        }
    }

    /**
     * Determine if there is a chip with coordinates (x, y) in the subset,
     *      which has a core with the given id in the subset.
     * @param core x, y and p coordinates
     * @return True if and only if there is a core with these coordinates
     */
    public boolean isCore(CoreLocation core) {
        CoreSubset subset = subsets.get(core.asChipLocation());
        if (subset == null) {
            return false;
        } else {
            return subset.contains(core);
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
        for (CoreSubset subset:subsets.values()) {
            // Calling hashcode on the subset makes it imutable.
            hash = 89 * hash + subset.hashCode();
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
        if (!Objects.equals(this.subsets, other.subsets)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return subsets.toString();
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
        for (Entry<ChipLocation, CoreSubset> entry:subsets.entrySet()) {
            if (other.subsets.containsKey(entry.getKey())) {
                CoreSubset combined = entry.getValue().intersection(
                        other.subsets.get(entry.getKey()));
                if (combined.size() > 0) {
                    results.subsets.put(entry.getKey(), combined);
                }
            }
        }
        return results;
    }

    private class CoreIterator implements Iterator<CoreLocation> {

        Iterator<CoreLocation> inner;
        Iterator<CoreSubset> subsets;

        CoreIterator(Iterator<CoreSubset> subsets) {
            this.subsets = subsets;
            if (subsets.hasNext()) {
                inner = subsets.next().coreIterator();
                checkNext();
            } else {
                inner = null;
            }
        }

        @Override
        public boolean hasNext() {
            return (inner != null);
        }

        @Override
        public CoreLocation next() {
            if (inner == null) {
                throw new NoSuchElementException("No CoreLocation remaining.");
            }
            CoreLocation result = inner.next();
            checkNext();
            return result;
        }

        private void checkNext() {
            while (!inner.hasNext()) {
                if (subsets.hasNext()) {
                    inner = subsets.next().coreIterator();
                } else {
                    inner = null;
                    return;
                }
            }
        }

    }
}
