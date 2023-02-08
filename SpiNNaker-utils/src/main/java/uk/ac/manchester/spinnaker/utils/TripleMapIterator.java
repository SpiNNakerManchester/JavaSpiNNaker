/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	 * @param outerMap
	 *            A triple map with any type(s) as the keys.
	 */
	public TripleMapIterator(
			Map<?, ? extends Map<?, ? extends Map<?, V>>> outerMap) {
		this(outerMap.values().iterator());
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
				inner = new DoubleMapIterator<>(outer.next());
			} else {
				inner = null;
			}
		}
	}

}
