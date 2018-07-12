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
 *
 * @author Christian-B
 */
public class CoreSubset {

    private final TreeMap<Integer, CoreLocation> cores;
    public final ChipLocation chip;
    private boolean immutable;

    public CoreSubset(ChipLocation chip) {
        this.chip = chip;
        cores = new TreeMap();
        immutable = false;
    }

    public CoreSubset(ChipLocation chip, Iterable<Integer>processors) {
        this(chip);
        for (Integer p: processors) {
            addCore(p);
        }
    }

    public CoreSubset(ChipLocation chip, int p) {
        this(chip);
        addCore(p);
    }

    public void addCore(Integer p) {
        if (immutable) {
            throw new IllegalStateException("The subsets is immutable. "
                    + "Possibly because a hashcode has been generated.");
        }
        cores.put(p, new CoreLocation(chip.getX(), chip.getY(), p));
    }

    public void addCore(int p) {
        if (immutable) {
            throw new IllegalStateException("The subsets is immutable. "
                    + "Possibly because a hashcode has been generated.");
        }
        cores.put(p, new CoreLocation(chip.getX(), chip.getY(), p));
    }

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

    public void addCores(Iterable<Integer> processors) {
        if (immutable) {
            throw new IllegalStateException("The subsets is immutable. "
                    + "Possibly because a hashcode has been generated.");
        }
        for (int p:processors) {
            cores.put(p, new CoreLocation(chip.getX(), chip.getY(), p));
        }
    }

    public Set<Integer> processors() {
        return Collections.unmodifiableSet(cores.keySet());
    }

    public Collection<CoreLocation> coreLocations() {
        return Collections.unmodifiableCollection(cores.values());
    }

    public Iterator<CoreLocation> coreIterator() {
        return cores.values().iterator();
    }

    public Iterable<CoreLocation> coreIterable() {
        return new Iterable<CoreLocation>() {
            @Override
            public Iterator<CoreLocation> iterator() {
                return cores.values().iterator();
            }
        };
    }

    public boolean contains(Integer p) {
        return cores.containsKey(p);
    }

    public boolean contains(CoreLocation core) {
        return cores.containsValue(core);
    }

    @Override
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
        if (!Objects.equals(this.cores, other.cores)) {
            return false;
        }
        if (!Objects.equals(this.chip, other.chip)) {
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

    public int size() {
        return cores.size();
    }

    public CoreSubset intersection(CoreSubset other) {
        if (!this.chip.onSameChipAs(other.chip)) {
            throw new IllegalArgumentException(
                    "Can not combine CoreSubset with Chip ("
                            + chip.getX() + "," + chip.getY()
                            + ") with CoreSubset with Chip (" +
                            other.chip.getX() + "," + other.chip.getY() + ")");
        }
        CoreSubset result = new CoreSubset(chip);
        for (Integer p: processors()) {
            if (other.contains(p)) {
                result.addCore(p);
            }
        }
        return result;
    }

    public String toString() {
          return (chip + "p:" + cores.keySet());
    }
}
