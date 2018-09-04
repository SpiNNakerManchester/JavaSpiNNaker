/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.util.Iterator;
import java.util.Map;

/**
 * An Iterator for a Map or Collection of Maps of maps.
 * <p>
 * The type of the Keys of the maps is irrelevant. It is not even required that
 * keys in each map are of the same type.
 *
 * @author Christian-B
 * @param <V>
 *            class of objects to be supplied by the final iterator.
 */
public final class TripleMapIterable<V> implements Iterable<V> {

	private final Iterable<? extends Map<?, ? extends Map<?, V>>> outer;

	/**
	 * Creates an Iterable given a Map of Maps of Maps.
	 *
	 * @param outermap
	 *            A triple map with any type(s) as the keys.
	 */
	public TripleMapIterable(
			Map<?, ? extends Map<?, ? extends Map<?, V>>> outermap) {
		this(outermap.values());
	}

	/**
	 * Creates an Iterable given a Iterable/Collection of Maps of Maps.
	 *
	 * @param outer
	 *            An Iterable of double maps with any type(s) as the keys.
	 */
	public TripleMapIterable(
			Iterable<? extends Map<?, ? extends Map<?, V>>> outer) {
		this.outer = outer;
	}

	@Override
	public Iterator<V> iterator() {
		return new TripleMapIterator<>(outer.iterator());
	}

}
