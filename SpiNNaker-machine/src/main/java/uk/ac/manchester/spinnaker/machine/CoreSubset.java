/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author Christian-B
 */
public class CoreSubset {

    private final TreeMap<Integer, CoreLocation> cores;
    public final HasChipLocation chip;


    public CoreSubset(HasChipLocation chip) {
        this.chip = chip;
        cores = new TreeMap();
    }

    public CoreSubset(HasChipLocation chip, Iterable<Integer>processors) {
        this(chip);
        for (Integer p: processors) {
            addCore(p);
        }
    }

    public CoreSubset(HasChipLocation chip, int p) {
        this(chip);
        addCore(p);
    }

    public void addCore(Integer p) {
        cores.put(p, new CoreLocation(chip.getX(), chip.getY(), p));
    }

    public void addCore(int p) {
        cores.put(p, new CoreLocation(chip.getX(), chip.getY(), p));
    }

    public void addCore(CoreLocation core) {
        if (!this.chip.onSameChipAs(core)) {
            throw new IllegalArgumentException(
                    "Can not add core " + core + " to CoreSubset with Chip ("
                    + chip.getX() + "," + chip.getY() + ")");
        }
        cores.put(core.getP(), core);
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
}
