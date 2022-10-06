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

import uk.ac.manchester.spinnaker.machine.board.ValidBoardNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidCabinetNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidFrameNumber;

/** Physical board coordinates. May be a hash table key. */
@Immutable
public final class CFB {
	/** Cabinet number. */
	@ValidCabinetNumber
	public final int c;

	/** Frame number. */
	@ValidFrameNumber
	public final int f;

	/** Board number. */
	@ValidBoardNumber
	public final int b;

	/**
	 * @param c Cabinet number.
	 * @param f Frame number.
	 * @param b Board number.
	 */
	public CFB(int c, int f, int b) {
		this.c = c;
		this.f = f;
		this.b = b;
	}

	CFB(PyObject tuple) {
		int index = 0;
		c = item(tuple, index++).asInt();
		f = item(tuple, index++).asInt();
		b = item(tuple, index++).asInt();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CFB) {
			var other = (CFB) obj;
			return c == other.c && f == other.f && b == other.b;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (c * 5 + f) * 5 + b;
	}

	@Override
	public String toString() {
		return "[c:" + c + ",f:" + f + ",b:" + b + "]";
	}

	CF asCF() {
		return new CF(c, f);
	}
}
