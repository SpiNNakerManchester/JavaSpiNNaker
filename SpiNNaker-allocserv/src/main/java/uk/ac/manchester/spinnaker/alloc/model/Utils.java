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
