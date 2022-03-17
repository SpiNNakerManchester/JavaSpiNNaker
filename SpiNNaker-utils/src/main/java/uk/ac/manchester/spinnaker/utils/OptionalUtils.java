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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Extra utilities for working with {@link Optional} in Java 8.
 *
 * @author Donal Fellows
 */
public abstract class OptionalUtils {
	private OptionalUtils() {
	}

	/**
	 * How to apply a sequence of modification transforms to a value in an
	 * optional if there is a value there at all.
	 *
	 * @param <T>
	 *            The type of value in the optional.
	 * @param source
	 *            Where to get the value from.
	 * @param actions
	 *            The transformations to apply to the value in the optional if
	 *            it exists; these are expected to <em>modify</em> that object.
	 * @return The original optional.
	 */
	@SafeVarargs
	public static <T> Optional<T> apply(Optional<T> source,
			Consumer<T>... actions) {
		source.ifPresent(t -> {
			for (Consumer<T> action : actions) {
				action.accept(t);
			}
		});
		return source;
	}

	/**
	 * How to do one thing if an {@linkplain Optional optional} is present, and
	 * another if it is absent.
	 *
	 * @param <T>
	 *            The type of value in the optional.
	 * @param <U>
	 *            The type of the result.
	 * @param source
	 *            Where to get the value from.
	 * @param ifPresent
	 *            What to do if the value is available.
	 * @param ifAbsent
	 *            What to do if the value is not available.
	 * @return The result, depending on which arm was taken.
	 */
	public static <T, U> U ifElse(Optional<T> source, Function<T, U> ifPresent,
			Supplier<U> ifAbsent) {
		return source.map(ifPresent).orElseGet(ifAbsent);
	}

	/**
	 * How to do one thing if an {@linkplain Optional optional} is present, and
	 * another if it is absent.
	 *
	 * @param <T>
	 *            The type of value in the optional.
	 * @param <U>
	 *            The type of the result.
	 * @param source
	 *            Where to get the value from.
	 * @param ifPresent
	 *            What to do if the value is available.
	 * @param ifAbsent
	 *            What to do if the value is not available.
	 * @return The result, depending on which arm was taken.
	 */
	public static <T, U> Optional<U> ifElseFlat(Optional<T> source,
			Function<T, U> ifPresent, Supplier<U> ifAbsent) {
		return source.map(t -> Optional.ofNullable(ifPresent.apply(t)))
				.orElseGet(() -> Optional.ofNullable(ifAbsent.get()));
	}
}
