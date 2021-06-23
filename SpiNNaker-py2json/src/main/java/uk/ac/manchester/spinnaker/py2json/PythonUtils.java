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

public class PythonUtils {
	private PythonUtils() {
	}

	public static PyObject getattr(PyObject object, String name) {
		return object.__getattr__(new PyString(name));
	}

	public static int len(PyObject object) {
		return object.__len__();
	}

	public static PyObject item(PyObject object, int index) {
		return object.__getitem__(index);
	}

	public static PyObject item(PyObject object, PyObject key) {
		return object.__getitem__(key);
	}

	@FunctionalInterface
	public interface Depythonizer<T> {
		T act(PyObject object);
	}

	public static <S extends Collection<U>, T, U> Map<T, S> toCollectingMap(
			PyObject mapObject, Depythonizer<T> makeKey,
			Function<? super T, ? extends S> foo, Depythonizer<U> makeValue) {
		Map<T, S> result = new HashMap<>();
		for (PyObject key : mapObject.asIterable()) {
			result.computeIfAbsent(makeKey.act(key), foo)
					.add(makeValue.act(key));
		}
		return result;
	}

	public static <T, U> Map<T, U> toMap(PyObject mapObject,
			Depythonizer<T> makeKey, Depythonizer<U> makeValue) {
		Map<T, U> result = new HashMap<>();
		for (PyObject key : mapObject.asIterable()) {
			result.put(makeKey.act(key), makeValue.act(item(mapObject, key)));
		}
		return result;
	}

	public static <T> List<T> toList(PyObject mapObject,
			Depythonizer<T> makeValue) {
		List<T> result = new ArrayList<>();
		for (PyObject value : mapObject.asIterable()) {
			result.add(makeValue.act(value));
		}
		return result;
	}

	public static <T> Set<T> toSet(PyObject mapObject,
			Depythonizer<T> makeValue) {
		Set<T> result = new HashSet<>();
		for (PyObject value : mapObject.asIterable()) {
			result.add(makeValue.act(value));
		}
		return result;
	}
}
