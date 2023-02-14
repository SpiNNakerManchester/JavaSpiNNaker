/*
 * Copyright (c) 2022 The University of Manchester
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

import static java.lang.Math.min;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.EnumSet.noneOf;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Utilities for working with collections. Things that it would be nice if they
 * were in Java itself, but which aren't.
 *
 * @author Donal Fellows
 * @see MappableIterable
 */
public abstract class CollectionUtils {
	private CollectionUtils() {
	}

	/**
	 * The binary and operator ({@code &}) as a function.
	 */
	public static final IntBinaryOperator AND = (a, b) -> a & b;

	/**
	 * The binary or operator ({@code |}) as a function.
	 */
	public static final IntBinaryOperator OR = (a, b) -> a | b;

	/**
	 * Create a collector that collects to an {@link EnumSet}.
	 *
	 * @param <E>
	 *            The type of {@code enum} we are collecting.
	 * @param cls
	 *            The class of the {@code enum} we are collecting.
	 * @return The collector.
	 */
	public static <E extends Enum<E>> Collector<E, ?, EnumSet<E>> toEnumSet(
			Class<E> cls) {
		return toCollection(() -> noneOf(cls));
	}

	/**
	 * Generate an iterable that covers a range. Only expected to cover the
	 * range once.
	 *
	 * @param startAt
	 *            What value to start at.
	 * @param upTo
	 *            What value to go up to (in steps of +1) but not include.
	 * @return A one-shot iterable.
	 */
	public static Iterable<Integer> range(int startAt, int upTo) {
		return IntStream.range(startAt, upTo)::iterator;
	}

	/**
	 * Generate an iterable that covers a range starting at zero and counting
	 * up. Only expected to cover the range once.
	 *
	 * @param upTo
	 *            What value to go up to (in steps of +1) but not include.
	 * @return A one-shot iterable.
	 */
	public static Iterable<Integer> range(int upTo) {
		return IntStream.range(0, upTo)::iterator;
	}

	/**
	 * Binary function currier.
	 *
	 * @param <T>
	 *            First argument type.
	 * @param <U>
	 *            Second argument type
	 * @param <V>
	 *            Return type.
	 * @param fn
	 *            Binary function.
	 * @param arg
	 *            First argument.
	 * @return Unary function based on binding the first argument to the binary
	 *         function.
	 */
	public static <T, U, V> Function<U, V> curry(BiFunction<T, U, V> fn,
			T arg) {
		return u -> fn.apply(arg, u);
	}

	/**
	 * Given a list of elements, split it into batches of elements of a given
	 * size. The final batch might be smaller. Note that this <em>requires</em>
	 * a list because it uses the {@link List#subList(int, int)} method.
	 *
	 * @param <T>
	 *            The type of the elements of the input list.
	 * @param batchSize
	 *            The maximum number of elements in a batch. All but the final
	 *            batch will have this number of elements; the final batch may
	 *            have fewer.
	 * @param input
	 *            The list to be split into batches.
	 * @return The batched list. The collections that make up each batch will be
	 *         unmodifiable, as will the overall collection.
	 */
	public static <T> Collection<Collection<T>> batch(int batchSize,
			List<T> input) {
		return IntStream.range(0, ceildiv(input.size(), batchSize))
				.map(i -> i * batchSize)
				.mapToObj(idx -> unmodifiableCollection(
						input.subList(idx, min(input.size(), idx + batchSize))))
				.collect(toUnmodifiableList());
	}

	/**
	 * Like {@link Stream#map(Function)}, but for lists/collections.
	 *
	 * @param <T>
	 *            The type of elements of the input list.
	 * @param <U>
	 *            The type of elements of the output list.
	 * @param list
	 *            The input list.
	 * @param fun
	 *            How to map an element.
	 * @return The output list. Unmodifiable.
	 */
	@UsedInJavadocOnly(Stream.class)
	public static <T, U> List<U> lmap(Collection<T> list, Function<T, U> fun) {
		return list.stream().map(fun).collect(toUnmodifiableList());
	}

	/**
	 * Utility for making the backing maps for fast {@code enum}s. These are
	 * expected to be used mainly to invert the trivial mapping from an
	 * enumeration member to its {@code value} field, though this is not assumed
	 * by this code.
	 *
	 * @param <E>
	 *            The type of the {@code enum}.
	 * @param <K>
	 *            The type of the value to use as the map key.
	 * @param enumMembers
	 *            The values in the {@code enum}, as returned by the
	 *            {@code values()} method.
	 * @param valueExtractor
	 *            How to get the value to use as the map key.
	 * @return Unmodifiable map from the values to the {@code enum} members.
	 */
	public static <E extends Enum<E>, K> Map<K, E> makeEnumBackingMap(
			E[] enumMembers, Function<E, K> valueExtractor) {
		return stream(enumMembers)
				.collect(toUnmodifiableMap(valueExtractor, v -> v));
	}

	/**
	 * Makes a read-only copy of a list.
	 *
	 * @param <T>
	 *            The type of elements in the list.
	 * @param list
	 *            The list to copy. {@code null} becomes an empty list.
	 * @return A read-only copy of the list.
	 */
	public static <T> List<T> copy(List<T> list) {
		// Don't copyOf; avoid nullability failures
		return isNull(list) ? List.of()
				: unmodifiableList(new ArrayList<>(list));
	}

	/**
	 * Makes a read-only copy of a set. This will preserve whatever order was in
	 * the input set.
	 *
	 * @param <T>
	 *            The type of elements in the set.
	 * @param set
	 *            The set to copy. {@code null} becomes an empty set.
	 * @return A read-only copy of the set.
	 */
	public static <T> Set<T> copy(Set<T> set) {
		// Don't copyOf; avoid nullability failures
		return isNull(set) ? Set.of()
				: unmodifiableSet(new LinkedHashSet<>(set));
	}

	/**
	 * Makes a read-only copy of a map. This will preserve whatever order was in
	 * the input map.
	 *
	 * @param <K>
	 *            The type of keys in the map.
	 * @param <V>
	 *            The type of values in the map.
	 * @param map
	 *            The map to copy. {@code null} becomes an empty map.
	 * @return A read-only copy of the map.
	 */
	public static <K, V> Map<K, V> copy(Map<K, V> map) {
		// Don't copyOf; avoid nullability failures
		return isNull(map) ? Map.of()
				: unmodifiableMap(new LinkedHashMap<>(map));
	}

	/**
	 * Create a collector that produces an array from a stream. The order of the
	 * elements in the array will be the inherent order of the elements in the
	 * stream.
	 *
	 * @param <T>
	 *            The type of the elements.
	 * @param generator
	 *            How to make the array. Typically something like
	 *            {@code T[]::new}.
	 * @return A collector.
	 */
	public static <
			T> Collector<T, ?, T[]> collectToArray(IntFunction<T[]> generator) {
		return new Collector<T, List<T>, T[]>() {
			@Override
			public Supplier<List<T>> supplier() {
				return ArrayList::new;
			}

			@Override
			public BiConsumer<List<T>, T> accumulator() {
				return List::add;
			}

			@Override
			public BinaryOperator<List<T>> combiner() {
				return (left, right) -> {
					left.addAll(right);
					return left;
				};
			}

			@Override
			public Function<List<T>, T[]> finisher() {
				// We know the size right now; use it!
				// Also, we assume that the generator is non-crazy
				return l -> l.toArray(generator.apply(l.size()));
			}

			@Override
			public Set<Characteristics> characteristics() {
				return Set.of();
			}
		};
	}
}
