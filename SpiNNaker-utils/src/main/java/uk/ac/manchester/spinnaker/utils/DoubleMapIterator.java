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
 * An Iterator for a Map or Collection of Maps.
 * <p>
 * The type of the Keys of the maps is irrelevant. It is not even required that
 * keys in each map are of the same type.
 *
 * @param <V>
 *            Class of the Object to be supplied by the final Iterator.
 * @author Christian-B
 */
public final class DoubleMapIterator<V> implements Iterator<V> {
	private final Iterator<? extends Map<?, V>> outer;

	private Iterator<V> inner;

	/**
	 * Creates an Iterator given a Map of Maps.
	 *
	 * @param outermap
	 *            A double map with any type(s) as the keys.
	 */
	public DoubleMapIterator(Map<?, ? extends Map<?, V>> outermap) {
		this(outermap.values().iterator());
	}

	/**
	 * Creates an Iterator given a Collection/Iterable of Maps.
	 *
	 * @param outerIterable
	 *            A double map with any type(s) as the keys.
	 */
	public DoubleMapIterator(Iterable<? extends Map<?, V>> outerIterable) {
		this(outerIterable.iterator());
	}

	/**
	 * Create an Iterator give a Iterator of Maps.
	 *
	 * @param outer
	 *            An iterator of Maps.
	 */
	public DoubleMapIterator(Iterator<? extends Map<?, V>> outer) {
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
			throw new NoSuchElementException("no more elements available");
		}
		var result = inner.next();
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
