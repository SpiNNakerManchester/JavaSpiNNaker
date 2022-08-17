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
package uk.ac.manchester.spinnaker.messages.model;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Enum for data types of system variables.
 *
 * @see SystemVariableDefinition
 */
public enum DataType {
	/** The value is one byte long, a {@code byte}. */
	BYTE(1),
	/** The value is two bytes long, a {@code short}. */
	SHORT(2),
	/** The value is four bytes long, an {@code int}. */
	INT(4),
	/** The value is eight bytes long, a {@code long}. */
	LONG(8),
	/** The value is an array of bytes, a {@code byte[]}. */
	BYTE_ARRAY(16),
	/**
	 * The value is four bytes, representing a {@linkplain MemoryLocation memory
	 * location}.
	 */
	@UsedInJavadocOnly(MemoryLocation.class)
	ADDRESS(4);

	/** The SCAMP data type descriptor code. */
	public final int value;

	DataType(int value) {
		this.value = value;
	}
}
