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
