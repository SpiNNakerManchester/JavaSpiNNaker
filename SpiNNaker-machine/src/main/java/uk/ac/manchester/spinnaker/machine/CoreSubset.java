/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents a subset of the cores on a SpiNNaker chip.
 *
 * <p>
 * @see <a
 * href="https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/core_subsets.py">
 * Python Version</a>
 *
 * @author Christian-B
 */
public class CoreSubset {

    private final TreeMap<Integer, CoreLocation> cores;

    /** The x and y coordinates for all cores in this subset. */
    public final ChipLocation chip;

    private boolean immutable;

    /**
     * Base constructor.
     *
     * @param chip The x and y coordinates.
     */
    CoreSubset(ChipLocation chip) {
        this.chip = chip;
        cores = new TreeMap();
        immutable = false;
    }

    /**
     * Constructs a subset and fills it with a core for each processor.
     * <p>
     * It there are no processors the subset will be empty.
     *
     * @param chip The x and y coordinates.
     * @param processors The p coordinates.
     */
    public CoreSubset(ChipLocation chip, Iterable<Integer> processors) {
        this(chip);
        for (Integer p: processors) {
            addCore(p);
        }
    }

    /**
     * Constructs a subsets with a single core.
     * @param chip The x and y coordinates.
     * @param p The p coordinate.
     */
    public CoreSubset(ChipLocation chip, int p) {
        this(chip);
        addCore(p);
    }

    /**
     * Adds a Core/Processor to the subsets.
     * <p>
     * This method uses set semantics so attempts to add a Core/Processor that
     *      is already in the subset are silently ignored.
     * <p>
     * Note: There is both and int and an Integer version of this method as it
     *      actually requires p in both formats and
     *      having two methods avoid having to switch twice.
     *
     * @param p id of the processor
     */
    public void addCore(Integer p) {
        if (immutable) {
            throw new IllegalStateException("The subsets is immutable. "
                    + "Possibly because a hashcode has been generated.");
        }
        cores.put(p, new CoreLocation(chip.getX(), chip.getY(), p));
    }

   /**
     * Adds a Core/Processor to the subsets.
     * <p>
     * This method uses set semantics so attempts to add a Core/Processor that
     *      is already in the subset are silently ignored.
     * <p>
     * Note: There is both and int and an Integer version of this method as it
     *      actually requires p in both formats and
     *      having two methods avoid having to switch twice.
     *
     * @param p id of the processor
     */
      public void addCore(int p) {
        if (immutable) {
            throw new IllegalStateException("The subsets is immutable. "
                    + "Possibly because a hashcode has been generated.");
        }
        cores.put(p, new CoreLocation(chip.getX(), chip.getY(), p));
    }

   /**
     * Adds a Core/Processors to the subsets.
     * <p>
     * This method uses set semantics so attempts to add a Core/Processor that
     *      is already in the subset are silently ignored.
     *
     * @param processors Iterable of p coordinates.
     */
    public void addCores(Iterable<Integer> processors) {
        if (immutable) {
            throw new IllegalStateException("The subsets is immutable. "
                    + "Possibly because a hashcode has been generated.");
        }
        for (int p:processors) {
            cores.put(p, new CoreLocation(chip.getX(), chip.getY(), p));
        }
    }
  /**
     * Adds a Core/Processor to the subsets.
     * <p>
     * This method uses set semantics so attempts to add a Core/Processor that
     *      is already in the subset are silently ignored.
     *
     * @param core x,y, and p coordinates of the processor
     * @throws IllegalArgumentException If the x and y coordinates of the core
     *     do not match the subsets chips x and y
     */
    public void addCore(CoreLocation core) {
        if (immutable) {
            throw new IllegalStateException("The subsets is immutable. "
                    + "Possibly because a hashcode has been generated.");
        }
        if (!this.chip.onSameChipAs(core)) {
            throw new IllegalArgumentException(
                    "Can not add core " + core + " to CoreSubset with Chip ("
                    + chip.getX() + "," + chip.getY() + ")");
        }
        cores.put(core.getP(), core);
    }

    /**
     * The subset of processor IDs on the chip.
     * @return An unmodifiable set of the p coordinates in these cores.
     */
    public Set<Integer> processors() {
        return Collections.unmodifiableSet(cores.keySet());
    }


     /**
     * The subset of cores on the chip.
     * <p>
     * While the result is technically not a set
     *      it is guarantee not to contain duplicates.
     *
     * @return An unmodifiable view over the cores.
     */
    public Collection<CoreLocation> coreLocations() {
        return Collections.unmodifiableCollection(cores.values());
    }

    /**
     * Provides a single pass iterator over the cores.
     * <p>
     * The results is guaranteed not to contain duplicates.
     * <p>
     * The behaviour if cores are added during iteration is not determined.
     * <p>
     * This method mainly serves as a support method for CoreSubsets Iterator.
     * <p>
     * Remember that iterators can not be used in for loops they require
     *      Iterables so use coreIterable instead.
     * @return A Iterator over the Cores in this subset.
     */
    public Iterator<CoreLocation> coreIterator() {
        return cores.values().iterator();
    }

    /**
     * Provides a multiple pass iterable over the cores.
      * <p>
     * The results is guaranteed not to contain duplicates.
     * <p>
     * The behaviour if cores are added during iteration is not determined.
     *
     * @return An Iterable over the Cores in this subset.
     */
    public Iterable<CoreLocation> coreIterable() {
        return new Iterable<CoreLocation>() {
            @Override
            public Iterator<CoreLocation> iterator() {
                return cores.values().iterator();
            }
        };
    }

    /**
     * Checks if the subsets contains a core with this processor id.
     *
     * @param p p coordinate of the core.
     *
     * @return True if and only if there is a core with this p coordinate.
     */
    public boolean contains(Integer p) {
        return cores.containsKey(p);
    }

    /**
     * Checks if the subsets contains a core equals to this one.
     * <p>
     * Checks against all three (x, y and p) coordinates of the core.
     * @param core x, y and p coordinates to check
     * @return True if and only if there is a core eual to thus one,
     */
    public boolean contains(CoreLocation core) {
        return cores.containsValue(core);
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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CoreSubset other = (CoreSubset) obj;
        if (!Objects.equals(this.chip, other.chip)) {
            return false;
        }
        if (!Objects.equals(this.cores.keySet(), other.cores.keySet())) {
            return false;
        }
        return true;
    }

    /**
     * Generate a hashcode for this subset.
     * <p>
     * Two subsets that have the same chip and the same processors
     *      (and are therefor considered equals)
     *      will generate the same hashcode.
     * <p>
     * To guarantee consistency over time once a hashcode is requested
     *      the subset will be made immutable
     *      and any farther add calls will raise an exception.
     * @return interger to use as the hashcode.
     */
    public int hashCode() {
        immutable = true;
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.chip);
        for (Integer key:cores.keySet()) {
            hash = 53 * hash + key;
        }
        return hash;
    }

    /**
     * The number of processors in this core subset.
     *
     * @return The count of the cores,.
     */
    public int size() {
        return cores.size();
    }

    /**
     * Returns a new CoreSubset which is an intersect of this and the other.
     *
     * @param other A second CoreSubset with possibly overlapping cores.
     * @return A new Coresubset Object with only the cores present in both.
     *      Therefor the result may be empty.
     */
    public CoreSubset intersection(CoreSubset other) {
        if (!this.chip.onSameChipAs(other.chip)) {
            throw new IllegalArgumentException(
                    "Can not combine CoreSubset with Chip ("
                            + chip.getX() + "," + chip.getY()
                            + ") with CoreSubset with Chip ("
                            + other.chip.getX() + "," + other.chip.getY()
                            + ")");
        }
        CoreSubset result = new CoreSubset(chip);
        for (Integer p: processors()) {
            if (other.contains(p)) {
                result.addCore(p);
            }
        }
        return result;
    }

    @Override
    public String toString() {
          return (chip + "p:" + cores.keySet());
    }
}
