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

import static uk.ac.manchester.spinnaker.py2json.PythonUtils.item;

import org.python.core.PyObject;

import com.google.errorprone.annotations.Immutable;

/** Frame/BMP coordinates. May be a hash table key. */
@Immutable
public final class CF {
	/** Cabinet number. */
	public final int c;

	/** Frame number. */
	public final int f;

	/**
	 * @param c Cabinet number.
	 * @param f Frame number.
	 */
	public CF(int c, int f) {
		this.c = c;
		this.f = f;
	}

	CF(PyObject tuple) {
		int index = 0;
		c = item(tuple, index++).asInt();
		f = item(tuple, index++).asInt();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CF) {
			var other = (CF) obj;
			return c == other.c && f == other.f;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (c * 5) + f;
	}

	@Override
	public String toString() {
		return "[c:" + c + ",f:" + f + "]";
	}
}
