/*
 * Copyright (c) 2022 The University of Manchester
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

import static java.lang.Math.min;
import static java.util.Collections.unmodifiableCollection;
import static java.util.EnumSet.noneOf;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collector;
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
		return unmodifiableCollection(range(0,
				(input.size() + batchSize - 1) / batchSize)
				.map(i -> i * batchSize)
				.mapToObj(idx -> unmodifiableCollection(
						input.subList(idx, min(input.size(), idx + batchSize))))
				.collect(toList()));
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
	 * @return The output list.
	 */
	public static <T, U> List<U> lmap(Collection<T> list, Function<T, U> fun) {
		return list.stream().map(fun).collect(toList());
	}

	static {
		Stream.class.getClass();
	}
}
