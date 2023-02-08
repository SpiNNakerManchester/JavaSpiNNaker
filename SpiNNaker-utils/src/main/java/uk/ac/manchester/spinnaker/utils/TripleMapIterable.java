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
public final class TripleMapIterable<V> implements MappableIterable<V> {
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
