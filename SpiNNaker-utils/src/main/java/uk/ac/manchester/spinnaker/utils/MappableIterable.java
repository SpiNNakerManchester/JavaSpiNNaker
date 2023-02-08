/*
 * Copyright (c) 2021 The University of Manchester
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

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.utils.IteratorWrapper.wrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
		return wrap(this, src -> new Iterator<U>() {
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
		return wrap(this, src -> new IteratorSupport<T>() {
			@Override
			T generateNext() {
				while (src.hasNext()) {
					var val = src.next();
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
		for (var value : this) {
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
		return wrap(this, src -> new IteratorSupport<T>() {
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
		for (var value : this) {
			if (i++ >= n) {
				return Optional.ofNullable(value);
			}
		}
		return Optional.empty();
	}

	/**
	 * Convert this iterable to a list. The order of elements in the list will
	 * be the same as the order of items in this iterable.
	 *
	 * @return A list of the elements in the iterable. Note that this must be
	 *         finite! This list is not modifiable.
	 */
	default List<T> toList() {
		var list = new ArrayList<T>();
		for (var val : this) {
			list.add(val);
		}
		return unmodifiableList(list);
	}

	/**
	 * Convert this iterable to a list. Items will be added to the list in the
	 * order of this iterable.
	 *
	 * @param supplier
	 *            How to make the list itself.
	 * @return A list of the elements in the iterable. Note that this must be
	 *         finite! This list is not modifiable.
	 */
	default List<T> toList(Supplier<List<T>> supplier) {
		var list = supplier.get();
		for (var val : this) {
			list.add(val);
		}
		return unmodifiableList(list);
	}

	/**
	 * Convert this iterable to a set. The natural order of elements in the set
	 * will be the same as the order of items in this iterable.
	 *
	 * @return A set of the elements in the iterable. Note that this must be
	 *         finite! This set is not modifiable.
	 */
	default Set<T> toSet() {
		var set = new LinkedHashSet<T>();
		for (var val : this) {
			set.add(val);
		}
		return unmodifiableSet(set);
	}

	/**
	 * Convert this iterable to a set. Items will be added to the set in the
	 * order of this iterable.
	 *
	 * @param supplier
	 *            How to make the set itself.
	 * @return A set of the elements in the iterable. Note that this must be
	 *         finite! This set is not modifiable.
	 */
	default Set<T> toSet(Supplier<Set<T>> supplier) {
		var set = supplier.get();
		for (var val : this) {
			set.add(val);
		}
		return unmodifiableSet(set);
	}

	/**
	 * Convert this iterable to a map. The natural order of entries will be the
	 * same as the natural order of elements in this iterable.
	 *
	 * @param <K>
	 *            The type of keys.
	 * @param <V>
	 *            The type of values.
	 * @param keyMapper
	 *            How to get a key from an element of the iterable.
	 * @param valueMapper
	 *            How to get a value from an element of the iterable.
	 * @return A map derived from elements in the iterable. Note that this must
	 *         be finite! This map is not modifiable.
	 */
	default <K, V> Map<K, V> toMap(Function<T, K> keyMapper,
			Function<T, V> valueMapper) {
		var map = new LinkedHashMap<K, V>();
		for (var val : this) {
			map.put(keyMapper.apply(val), valueMapper.apply(val));
		}
		return unmodifiableMap(map);
	}

	/**
	 * Convert this iterable to a map. Items will be added to the map in the
	 * order of this iterable.
	 *
	 * @param <K>
	 *            The type of keys.
	 * @param <V>
	 *            The type of values.
	 * @param supplier
	 *            How to make the map itself.
	 * @param keyMapper
	 *            How to get a key from an element of the iterable.
	 * @param valueMapper
	 *            How to get a value from an element of the iterable.
	 * @return A map derived from the elements in the iterable. Note that this
	 *         must be finite! This map is not modifiable.
	 */
	default <K, V> Map<K, V> toMap(Supplier<Map<K, V>> supplier,
			Function<T, K> keyMapper, Function<T, V> valueMapper) {
		var map = supplier.get();
		for (var val : this) {
			map.put(keyMapper.apply(val), valueMapper.apply(val));
		}
		return unmodifiableMap(map);
	}

	/**
	 * Convert this iterable to a map of lists. Items will be added to
	 * the map and to the lists in the order of this iterable.
	 *
	 * @param <K>
	 *            The type of keys.
	 * @param <V>
	 *            The type of leaf values.
	 * @param keyMapper
	 *            How to get a key from an element of the iterable.
	 * @param valueMapper
	 *            How to get a leaf value from an element of the iterable.
	 * @return A map derived from the elements in the iterable. Note that this
	 *         must be finite! This map is not modifiable.
	 */
	default <K, V> Map<K, List<V>> toCollectingMap(
			Function<T, K> keyMapper, Function<T, V> valueMapper) {
		var map = new LinkedHashMap<K, List<V>>();
		for (var val : this) {
			map.computeIfAbsent(keyMapper.apply(val), __ -> new ArrayList<>())
					.add(valueMapper.apply(val));
		}
		return unmodifiableMap(map);
	}

	/**
	 * Convert this iterable to a map of sets of enum values. Items will be
	 * added to the map in the order of this iterable.
	 *
	 * @param <K>
	 *            The type of keys.
	 * @param <E>
	 *            The type of leaf values.
	 * @param cls
	 *            The class of leaf values.
	 * @param keyMapper
	 *            How to get a key from an element of the iterable.
	 * @param valueMapper
	 *            How to get a leaf value from an element of the iterable.
	 * @return A map derived from the elements in the iterable. Note that this
	 *         must be finite! This map is not modifiable.
	 */
	default <K, E extends Enum<E>> Map<K, EnumSet<E>> toCollectingMap(
			Class<E> cls, Function<T, K> keyMapper,
			Function<T, E> valueMapper) {
		var map = new LinkedHashMap<K, EnumSet<E>>();
		for (var val : this) {
			map.computeIfAbsent(keyMapper.apply(val), __ -> EnumSet.noneOf(cls))
					.add(valueMapper.apply(val));
		}
		return unmodifiableMap(map);
	}

	/**
	 * Convert this iterable to a map of collections. Items will be added to
	 * the map and to the collections in the order of this iterable.
	 *
	 * @param <K>
	 *            The type of keys.
	 * @param <V>
	 *            The type of leaf values.
	 * @param <S>
	 *            The type of per-key collectors.
	 * @param supplier
	 *            How to make the value collectors.
	 * @param keyMapper
	 *            How to get a key from an element of the iterable.
	 * @param valueMapper
	 *            How to get a leaf value from an element of the iterable.
	 * @return A map derived from the elements in the iterable. Note that this
	 *         must be finite! This map is not modifiable.
	 */
	default <K, V, S extends Collection<V>> Map<K, S> toCollectingMap(
			Supplier<S> supplier, Function<T, K> keyMapper,
			Function<T, V> valueMapper) {
		var map = new LinkedHashMap<K, S>();
		for (var val : this) {
			map.computeIfAbsent(keyMapper.apply(val), __ -> supplier.get())
					.add(valueMapper.apply(val));
		}
		return unmodifiableMap(map);
	}

	/**
	 * Convert this iterable to a map of collections. Items will be added to
	 * the map and to the collections in the order of this iterable.
	 *
	 * @param <K>
	 *            The type of keys.
	 * @param <V>
	 *            The type of leaf values.
	 * @param <S>
	 *            The type of per-key collectors.
	 * @param mapSupplier
	 *            How to make the map itself.
	 * @param collectorSupplier
	 *            How to make the value collectors.
	 * @param keyMapper
	 *            How to get a key from an element of the iterable.
	 * @param valueMapper
	 *            How to get a value from an element of the iterable.
	 * @return A map derived from the elements in the iterable. Note that this
	 *         must be finite! This map is not modifiable.
	 */
	default <K, V, S extends Collection<V>> Map<K, S> toCollectingMap(
			Supplier<Map<K, S>> mapSupplier, Supplier<S> collectorSupplier,
			Function<T, K> keyMapper, Function<T, V> valueMapper) {
		var map = mapSupplier.get();
		for (var val : this) {
			map.computeIfAbsent(keyMapper.apply(val),
					__ -> collectorSupplier.get()).add(valueMapper.apply(val));
		}
		return unmodifiableMap(map);
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
		var val = value;
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
			var srcit = mapiter.iterator();
			return mapper.apply(srcit);
		};
	}
}
