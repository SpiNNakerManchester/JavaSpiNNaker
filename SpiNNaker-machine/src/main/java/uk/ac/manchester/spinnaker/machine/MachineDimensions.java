/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine;

import static java.util.Objects.isNull;

/** Represents the size of a machine in chips. */
public final class MachineDimensions {
	/** The width of the machine in chips. */
	public final int width;

	/** The height of the machine in chips. */
	public final int height;

	/**
	 * Create a new instance.
	 *
	 * @param width
	 *            The width of the machine, in chips.
	 * @param height
	 *            The height of the machine, in chips.
	 */
	public MachineDimensions(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof MachineDimensions)
				&& equals((MachineDimensions) o);
	}

	/**
	 * Tests whether this object is equal to another dimension.
	 *
	 * @param dimension
	 *            The other dimension object to compare to.
	 * @return True exactly when they are equal.
	 */
	public boolean equals(MachineDimensions dimension) {
		if (isNull(dimension)) {
			return false;
		}
		return width == dimension.width && height == dimension.height;
	}

	@Override
	public int hashCode() {
		return width << 16 | height;
	}

	@Override
	public String toString() {
		return "Width:" + width + " Height:" + height;
	}
}
