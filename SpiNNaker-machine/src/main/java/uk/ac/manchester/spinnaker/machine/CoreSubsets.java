/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;

/**
 *
 * @author Christian-B
 */
public class CoreSubsets {

    private final TreeMap<ChipLocation, CoreSubset> cores;

    public CoreSubsets() {
        cores = new TreeMap();
    }

    public void addCore(CoreLocation core) {
        ChipLocation chip = new ChipLocation(core.getX(), core.getY());
        CoreSubset subset = getCoreSubset(chip);
        subset.addCore(core);
    }

    private CoreSubset getCoreSubset(ChipLocation chip) {
        if (!cores.containsKey(chip)) {
            cores.put(chip, new CoreSubset(chip));
        }
        return cores.get(chip);
    }

    public Collection<CoreLocation> coreLocations(ChipLocation chip) {
        CoreSubset subset = getCoreSubset(chip);
        return subset.coreLocations();
    }

    public Iterable<CoreLocation> coreIterable() {
        Collection<CoreSubset> foo = cores.values();

        return new Iterable<CoreLocation>() {
            @Override
            public Iterator<CoreLocation> iterator() {
                return new CoreIterator(cores.values().iterator());
            }
        };
    }

    public CoreSubsets intersection(CoreSubsets other) {
        CoreSubsets results = new CoreSubsets();
        for (Entry<ChipLocation, CoreSubset> entry:cores.entrySet()) {
            if (other.cores.containsKey(entry.getKey())) {
                CoreSubset combined = entry.getValue().intersection(
                        other.getCoreSubset(entry.getKey()));
                results.cores.put(entry.getKey(), combined);
            }
        }
        return results;
    }

    class CoreIterator implements Iterator<CoreLocation> {

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
