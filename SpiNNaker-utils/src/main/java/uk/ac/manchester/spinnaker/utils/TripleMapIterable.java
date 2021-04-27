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
