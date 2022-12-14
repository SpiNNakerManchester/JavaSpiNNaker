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
package uk.ac.manchester.spinnaker.py2json;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import static org.python.core.Py.newString;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.python.core.PyObject;

/**
 * Wrappers around {@link PyObject} to make it less awful.
 *
 * @author Donal Fellows
 */
final class PythonUtils {
	private PythonUtils() {
	}

	/**
	 * Get an attribute of a value. Might be a field or a method.
	 *
	 * @param object
	 *            The object to get the attribute of.
	 * @param name
	 *            The name of the attribute
	 * @return The value of the attribute.
	 */
	public static PyObject getattr(PyObject object, String name) {
		return object.__getattr__(newString(name));
	}

	/**
	 * Get the length of a value, assuming it has that concept.
	 *
	 * @param object
	 *            The value to get the length of.
	 * @return The length of the value.
	 */
	public static int len(PyObject object) {
		return object.__len__();
	}

	/**
	 * Index into a value by number, assuming it supports indexing.
	 *
	 * @param object
	 *            The value to index into.
	 * @param index
	 *            The index to use.
	 * @return The value at that index, assuming it exists.
	 */
	public static PyObject item(PyObject object, int index) {
		return object.__getitem__(index);
	}

	/**
	 * Index into a value by general value, assuming it supports indexing.
	 *
	 * @param object
	 *            The value to index into.
	 * @param key
	 *            The key to use.
	 * @return The value for that key, assuming it exists.
	 */
	public static PyObject item(PyObject object, PyObject key) {
		return object.__getitem__(key);
	}

	/**
	 * Convert an iterable object into a stream.
	 *
	 * @param iterable
	 *            The iterable object.
	 * @return The stream view.
	 */
	private static Stream<PyObject> stream(PyObject iterable) {
		return StreamSupport.stream(iterable.asIterable().spliterator(), false);
	}

	/**
	 * Convert a collection of values into a map of collections.
	 *
	 * @param <S>
	 *            The type of collection to produce a map of.
	 * @param <T>
	 *            The type of keys in the outer map
	 * @param <U>
	 *            The type of values in the inner collection.
	 * @param mapObject
	 *            The originating object.
	 * @param makeKey
	 *            How to make a key from an element of the originating object.
	 * @param makeCollector
	 *            How to make a collecting collection for a key.
	 * @param makeValue
	 *            How to make a leaf value from an element of the originating
	 *            object.
	 * @return The map of collections.
	 */
	public static <S extends Collection<U>, T, U> Map<T, S> toCollectingMap(
			PyObject mapObject, Function<PyObject, T> makeKey,
			Supplier<S> makeCollector, Function<PyObject, U> makeValue) {
		return stream(mapObject).collect(groupingBy(makeKey,
				mapping(makeValue, toCollection(makeCollector))));
	}

	/**
	 * Convert a dictionary into a map.
	 *
	 * @param <T>
	 *            The type of keys in the map
	 * @param <U>
	 *            The type of values in the map.
	 * @param dictObject
	 *            The originating dict.
	 * @param makeKey
	 *            How to make a map key from a dict key.
	 * @param makeValue
	 *            How to make a map value from a dict value.
	 * @return The map.
	 */
	public static <T, U> Map<T, U> toMap(PyObject dictObject,
			Function<PyObject, T> makeKey, Function<PyObject, U> makeValue) {
		return stream(dictObject).collect(Collectors.toMap(makeKey,
				key -> makeValue.apply(item(dictObject, key))));
	}

	/**
	 * Convert a Python list into a Java list.
	 *
	 * @param <T>
	 *            The type of values in the Java list
	 * @param listObject
	 *            The originating list.
	 * @param makeValue
	 *            How to make a Java list element from a Python list element.
	 * @return The list.
	 */
	public static <T> List<T> toList(PyObject listObject,
			Function<PyObject, T> makeValue) {
		return stream(listObject).map(makeValue).collect(Collectors.toList());
	}

	/**
	 * Convert a Python set into a Java set.
	 *
	 * @param <T>
	 *            The type of values in the Java set
	 * @param listObject
	 *            The originating set.
	 * @param makeValue
	 *            How to make a Java set element from a Python set element.
	 * @return The set.
	 */
	public static <T> Set<T> toSet(PyObject listObject,
			Function<PyObject, T> makeValue) {
		return stream(listObject).map(makeValue).collect(Collectors.toSet());
	}
}
