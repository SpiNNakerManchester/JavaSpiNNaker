/*
 * Copyright (c) 2018 The University of Manchester
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
