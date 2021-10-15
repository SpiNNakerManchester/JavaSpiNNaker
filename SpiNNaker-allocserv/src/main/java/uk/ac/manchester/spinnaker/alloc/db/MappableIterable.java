/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.db;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Extends iterable with the ability to be mapped to different values.
 *
 * @param <T>
 *            The type of elements returned by the iterator
 * @author Donal Fellows
 */
public interface MappableIterable<T> extends Iterable<T> {
	/**
	 * Apply a function to an iterable to get another iterable.
	 *
	 * @param <TT>
	 *            The type of the elements of the created iterable.
	 * @param mapper
	 *            The function to apply.
	 * @return The new iterable, which may also be mapped.
	 */
	default <TT> MappableIterable<TT> map(Function<T, TT> mapper) {
		MappableIterable<T> src = this;
		return new MappableIterable<TT>() {
			@Override
			public Iterator<TT> iterator() {
				Iterator<T> srcit = src.iterator();
				return new Iterator<TT>() {
					@Override
					public boolean hasNext() {
						return srcit.hasNext();
					}

					@Override
					public TT next() {
						return mapper.apply(srcit.next());
					}
				};
			}
		};
	}

	/**
	 * Apply a filter to an iterable to get another iterable. Note that this
	 * <em>assumes</em> that {@code null} is never produced as a value that
	 * should be produced in the resulting iterable.
	 *
	 * @param filterer
	 *            The filter function to apply.
	 * @return The new iterable, which only contains elements for which
	 *         {@code filterer} returns {@code true} <em>and which are not
	 *         {@code null}</em>.
	 */
	default MappableIterable<T> filter(Predicate<T> filterer) {
		MappableIterable<T> src = this;
		return new MappableIterable<T>() {
			@Override
			public Iterator<T> iterator() {
				Iterator<T> srcit = src.iterator();
				return new Iterator<T>() {
					T value;

					@Override
					public boolean hasNext() {
						if (value != null) {
							return true;
						}
						while (srcit.hasNext()) {
							T val = srcit.next();
							if (filterer.test(val)) {
								value = val;
								return true;
							}
						}
						return false;
					}

					@Override
					public T next() {
						T val = value;
						value = null;
						return val;
					}
				};
			}
		};
	}

	/**
	 * Convert this iterable to a list.
	 *
	 * @return A list of the elements in the iterable. Note that this must be
	 *         finite!
	 */
	default List<T> toList() {
		List<T> list = new ArrayList<>();
		for (T val : this) {
			list.add(val);
		}
		return list;
	}

	/**
	 * Convert this iterable to a set.
	 *
	 * @return A set of the elements in the iterable. Note that this must be
	 *         finite! Also note that the set preserves the iteration order of
	 *         the elements if they are all unique.
	 */
	default Set<T> toSet() {
		Set<T> list = new LinkedHashSet<>();
		for (T val : this) {
			list.add(val);
		}
		return list;
	}
}
