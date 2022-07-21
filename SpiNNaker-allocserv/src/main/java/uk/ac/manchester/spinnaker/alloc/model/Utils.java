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
package uk.ac.manchester.spinnaker.alloc.model;

import java.util.function.Function;

import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

/** Miscellaneous utilities. */
public abstract class Utils {
	private Utils() {
	}

	/**
	 * Extract a chip from a result set row.
	 *
	 * @param row
	 *            The row to extract from.
	 * @param x
	 *            The <em>name</em> of the column with the X coordinate.
	 * @param y
	 *            The <em>name</em> of the column with the Y coordinate.
	 * @return The chip location.
	 */
	public static ChipLocation chip(Row row, String x, String y) {
		return new ChipLocation(row.getInt(x), row.getInt(y));
	}

	/**
	 * Create a function for extracting a chip from a result set row.
	 *
	 * @param x
	 *            The <em>name</em> of the column with the X coordinate.
	 * @param y
	 *            The <em>name</em> of the column with the Y coordinate.
	 * @return The mapping function.
	 */
	public static Function<Row, ChipLocation> chip(String x, String y) {
		return row -> new ChipLocation(row.getInt(x), row.getInt(y));
	}
}
