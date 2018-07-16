/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * An Iterator for a Map or Collection of Maps of Maps.
 * <p>
 * The type of the Keys of the maps is irrelevant.
 * It is not even required that keys in each map are of the same type.
 *
 * @param <V> Class of the Object to be supplied by the final Iterator.
 * @author Christian-B
 */
public class TripleMapIterator<V> implements Iterator<V> {

    private final Iterator<Map<Object, Map<Object, V>>> outer;
    private Iterator<V> inner;

    /**
     * Creates an Iterator given a Map of Maps of Maps.
     *
     * @param outermap A triple map with any type(s) as the keys.
     */
    TripleMapIterator(Map<Object, Map<Object, Map<Object, V>>> outermap) {
        this(outermap.values().iterator());
    }

    /**
     * Creates an Iterator given an Iterable/Collection of Maps of Maps.
     *
     * @param outerIterable A triple map with any type(s) as the keys.
     */
    TripleMapIterator(Iterable<Map<Object, Map<Object, V>>> outerIterable) {
        this(outerIterable.iterator());
    }

    /**
     * Creates an Iterator given an Iterator of Maps of Maps.
     *
     * @param outer A Iterator of double maps with any type(s) as the keys.
     */
    TripleMapIterator(Iterator<Map<Object, Map<Object, V>>> outer) {
        this.outer = outer;
        if (outer.hasNext()) {
            inner = new DoubleMapIterator(outer.next());
            checkInner();
        } else {
            inner = null;
        }
    }

    @Override
    public boolean hasNext() {
        return inner != null;
    }

    @Override
    public V next() {
        if (inner == null) {
                throw new NoSuchElementException(
                        "No more Elements available.");
        }
        V result = inner.next();
        checkInner();
        return result;
    }

    private void checkInner() {
        while (inner != null) {
            if (inner.hasNext()) {
                return;
            }
            if (outer.hasNext()) {
                inner = new DoubleMapIterator(outer.next());
            } else {
                inner = null;
            }
        }
    }


}
