/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * An Iterator for a Map or Collection of Maps.
 * <p>
 * The type of the Keys of the maps is irrelevant.
 * It is not even required that keys in each map are of the same type.
 *
 * @param <V> Class of the Object to be supplied by the final Iterator.
 * @author Christian-B
 */
public final class DoubleMapIterator<V> implements Iterator<V> {

    private final Iterator<Map<?, V>> outer;
    private Iterator<V> inner;

    /**
     * Creates an Iterator given a Map of Maps.
     *
     * @param outermap A double map with any type(s) as the keys.
     */
    DoubleMapIterator(Map<?, Map<?, V>> outermap) {
        this(outermap.values().iterator());
    }

    /**
     * Creates an Iterator given a Collection/Iterable of Maps.
     *
     * @param outerIterable A double map with any type(s) as the keys.
     */
    DoubleMapIterator(Iterable<Map<?, V>> outerIterable) {
        this(outerIterable.iterator());
    }

    /**
     * Create an Iterator give a Iterator of Maps.
     *
     * @param outer An iterator of Maps.
     */
    DoubleMapIterator(Iterator<Map<?, V>> outer) {
        this.outer = outer;
        if (outer.hasNext()) {
            inner = outer.next().values().iterator();
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
                inner = outer.next().values().iterator();
            } else {
                inner = null;
            }
        }
    }


}
