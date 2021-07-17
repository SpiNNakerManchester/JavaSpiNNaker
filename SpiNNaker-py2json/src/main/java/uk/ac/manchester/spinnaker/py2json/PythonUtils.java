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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.python.core.PyObject;
import org.python.core.PyString;

/**
 * Wrappers around {@link PyObject} to make it less awful.
 *
 * @author Donal Fellows
 */
public final class PythonUtils {
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
		return object.__getattr__(new PyString(name));
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
	 * An operation used to convert a Python object into something more
	 * Java-like.
	 *
	 * @param <T>
	 *            The type of value to produce.
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	public interface Depythonizer<T> {
		/**
		 * Convert an object into a Java value.
		 *
		 * @param object
		 *            The value to convert.
		 * @return The value to produce.
		 */
		T act(PyObject object);
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
			PyObject mapObject, Depythonizer<T> makeKey,
			Function<T, S> makeCollector, Depythonizer<U> makeValue) {
		Map<T, S> result = new HashMap<>();
		for (PyObject value : mapObject.asIterable()) {
			result.computeIfAbsent(makeKey.act(value), makeCollector)
					.add(makeValue.act(value));
		}
		return result;
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
			Depythonizer<T> makeKey, Depythonizer<U> makeValue) {
		Map<T, U> result = new HashMap<>();
		for (PyObject key : dictObject.asIterable()) {
			result.put(makeKey.act(key), makeValue.act(item(dictObject, key)));
		}
		return result;
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
			Depythonizer<T> makeValue) {
		List<T> result = new ArrayList<>();
		for (PyObject value : listObject.asIterable()) {
			result.add(makeValue.act(value));
		}
		return result;
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
			Depythonizer<T> makeValue) {
		Set<T> result = new HashSet<>();
		for (PyObject value : listObject.asIterable()) {
			result.add(makeValue.act(value));
		}
		return result;
	}
}
