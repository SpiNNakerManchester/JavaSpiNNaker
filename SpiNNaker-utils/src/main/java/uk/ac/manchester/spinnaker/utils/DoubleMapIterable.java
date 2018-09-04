/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.util.Iterator;
import java.util.Map;

/**
 * An Iterable for a Map or Collection of Maps.
 * <p>
 * The type of the Keys of the maps is irrelevant. It is not even required that
 * keys in each map are of the same type.
 *
 * @author Christian-B
 * @param <V>
 *            Class of the Object to be supplied by the final Iterator.
 */
public final class DoubleMapIterable<V> implements Iterable<V> {

	private final Iterable<? extends Map<?, V>> outer;

	/**
	 * Creates an Iterable given a Map of Maps.
	 *
	 * @param outermap
	 *            A double map with any type(s) as the keys.
	 */
	public DoubleMapIterable(Map<?, ? extends Map<?, V>> outermap) {
		this(outermap.values());
	}

	/**
	 * Create an Iterator give a Collection/Iterable of Maps .
	 *
	 * @param outer
	 *            An iterable of Maps.
	 */
	public DoubleMapIterable(Iterable<? extends Map<?, V>> outer) {
		this.outer = outer;
	}

	@Override
	public Iterator<V> iterator() {
		return new DoubleMapIterator<>(outer);
	}

}
