/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * An Iterator for a Map or Collection of Maps of Maps.
 * <p>
 * The type of the Keys of the maps is irrelevant. It is not even required that
 * keys in each map are of the same type.
 *
 * @param <V>
 *            class of objects to be supplied by the this iterator.
 * @author Christian-B
 */
public class TripleMapIterator<V> implements Iterator<V> {
	private final Iterator<? extends Map<?, ? extends Map<?, V>>> outer;

	private Iterator<V> inner;

	/**
	 * Creates an Iterator given a Map of Maps of Maps.
	 *
	 * @param outermap
	 *            A triple map with any type(s) as the keys.
	 */
	public TripleMapIterator(
			Map<?, ? extends Map<?, ? extends Map<?, V>>> outermap) {
		this(outermap.values().iterator());
	}

	/**
	 * Creates an Iterator given an Iterable/Collection of Maps of Maps.
	 *
	 * @param outerIterable
	 *            A triple map with any type(s) as the keys.
	 */
	public TripleMapIterator(
			Iterable<? extends Map<?, ? extends Map<?, V>>> outerIterable) {
		this(outerIterable.iterator());
	}

	/**
	 * Creates an Iterator given an Iterator of Maps of Maps.
	 *
	 * @param outer
	 *            A Iterator of double maps with any type(s) as the keys.
	 */
	public TripleMapIterator(
			Iterator<? extends Map<?, ? extends Map<?, V>>> outer) {
		this.outer = outer;
		if (outer.hasNext()) {
			inner = new DoubleMapIterator<>(outer.next());
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
			throw new NoSuchElementException("no more elements available");
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
				inner = new DoubleMapIterator<>(outer.next());
			} else {
				inner = null;
			}
		}
	}

}
