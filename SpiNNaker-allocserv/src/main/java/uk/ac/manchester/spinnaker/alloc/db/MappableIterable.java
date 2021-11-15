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

import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Extends iterable with the ability to be mapped to different values.
 *
 * @param <T>
 *            The type of elements returned by the iterator. Note that it is
 *            <em>strongly</em> recommended to not ever return {@code null};
 *            consider using {@link Optional} instead of that.
 * @author Donal Fellows
 */
public interface MappableIterable<T> extends Iterable<T> {
	/**
	 * Apply a function to an iterable to get another iterable.
	 *
	 * @param <U>
	 *            The type of the elements of the created iterable.
	 * @param mapper
	 *            The function to apply. <em>Should not return
	 *            {@code null}.</em>
	 * @return The new iterable, which may also be mapped.
	 */
	default <U> MappableIterable<U> map(Function<T, U> mapper) {
		return IteratorWrapper.wrap(this, src -> new Iterator<U>() {
			@Override
			public boolean hasNext() {
				return src.hasNext();
			}

			@Override
			public U next() {
				return mapper.apply(src.next());
			}
		});
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
		return IteratorWrapper.wrap(this, src -> new IteratorSupport<T>() {
			@Override
			T generateNext() {
				while (src.hasNext()) {
					T val = src.next();
					if (filterer.test(val)) {
						return val;
					}
				}
				return null;
			}
		});
	}

	/**
	 * Get the first item in the iterable, if there is one.
	 *
	 * @return an optional with the first item.
	 */
	default Optional<T> first() {
		for (T value : this) {
			return Optional.ofNullable(value);
		}
		return Optional.empty();
	}

	/**
	 * Get another iterable with the first {@code n} item in the iterable (or up
	 * to that if the source iterable has fewer items).
	 *
	 * @param n
	 *            the maximum number of items of the iterable that are wanted
	 * @return the first {@code n} items.
	 */
	default MappableIterable<T> first(int n) {
		return IteratorWrapper.wrap(this, src -> new IteratorSupport<T>() {
			private int count;

			@Override
			T generateNext() {
				if (count >= n || !src.hasNext()) {
					return null;
				}
				count++;
				return src.next();
			}
		});
	}

	/**
	 * Index into the iterable.
	 *
	 * @param n
	 *            The index into the iterable. <em>Zero-based.</em>
	 * @return The item at that index, should it exist.
	 */
	default Optional<T> nth(int n) {
		int i = 0;
		for (T value : this) {
			if (i++ >= n) {
				return Optional.ofNullable(value);
			}
		}
		return Optional.empty();
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

/**
 * An interlocked generator that handles feeding values out of itself.
 *
 * @param <T>
 *            The type of values in the iterator.
 * @author Donal Fellows
 */
abstract class IteratorSupport<T> implements Iterator<T> {
	/**
	 * The current value.
	 */
	private T value;

	@Override
	public boolean hasNext() {
		if (nonNull(value)) {
			return true;
		}
		value = generateNext();
		return nonNull(value);
	}

	/**
	 * Generate the next value in the sequence, if there is one.
	 *
	 * @return The generated value, or {@code null} if there is no such value.
	 */
	abstract T generateNext();

	@Override
	public T next() {
		T val = value;
		value = null;
		if (nonNull(val)) {
			return val;
		}
		throw new NoSuchElementException("no such element");
	}
}

abstract class IteratorWrapper {
	private IteratorWrapper() {
	}

	static <T, U> MappableIterable<U> wrap(MappableIterable<T> mapiter,
			Function<Iterator<T>, Iterator<U>> mapper) {
		return () -> {
			Iterator<T> srcit = mapiter.iterator();
			return mapper.apply(srcit);
		};
	}
}
