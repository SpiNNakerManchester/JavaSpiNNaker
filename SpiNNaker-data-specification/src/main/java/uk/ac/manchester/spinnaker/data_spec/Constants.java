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
package uk.ac.manchester.spinnaker.data_spec;

/**
 * Constants used by the Data Structure Generator (DSG) and the Specification
 * Executor.
 */
public final class Constants {
	private Constants() {
	}

	/** Bytes per int/word. */
	public static final int INT_SIZE = 4;

	/** Bytes per long/double-word. */
	public static final int LONG_SIZE = 8;

	/**
	 * Data spec magic number. This marks the start of a block of memory in
	 * SpiNNaker's SDRAM that has been allocated by the Data Specification.
	 */
	public static final int DSG_MAGIC_NUM = 0x5B7CA17E;

	/** Application data magic number. */
	public static final int APPDATA_MAGIC_NUM = 0xAD130AD6;

	/** Version of the file produced by the DSE. */
	public static final int DSE_VERSION = 0x00010000;

	// DSG Arrays and tables sizes

	/** The number of registers in the DSE model. */
	public static final int MAX_REGISTERS = 16;

	/** The number of memory regions in the DSE model. */
	public static final int MAX_MEM_REGIONS = 32;

	/** The number of structures in the DSE model. */
	public static final int MAX_STRUCT_SLOTS = 16;

	/** The max number of elements in structures in the DSE model. */
	public static final int MAX_STRUCT_ELEMENTS = 255;

	/** The max number of pack specification items in the DSE model. */
	public static final int MAX_PACKSPEC_SLOTS = 16;

	/** The number of functions in the DSE model. */
	public static final int MAX_CONSTRUCTORS = 16;

	/** The number of parameter lists in the DSE model. */
	public static final int MAX_PARAM_LISTS = 16;

	/** The number of basic random number generators in the DSE model. */
	public static final int MAX_RNGS = 16;

	/** The number of random number distributions in the DSE model. */
	public static final int MAX_RANDOM_DISTS = 16;

	/**
	 * The size of the Data Specification table header, in bytes.
	 * Note that the header consists of 2 uint32_t variables
	 * (magic number, version).
	 */
	private static final int APP_PTR_TABLE_HEADER_SIZE = INT_SIZE * 2;

	/**
	 * The size of a Data Specification region description, in bytes.
	 * Note that each description consists of a pointer and 2 uint32_t variables
	 * (pointer, checksum, n_words).
	 */
	private static final int APP_PTR_TABLE_REGION_SIZE = INT_SIZE * 3;

	/**
	 * The size of the Data Specification table, in bytes.
	 */
	public static final int APP_PTR_TABLE_BYTE_SIZE =
			APP_PTR_TABLE_HEADER_SIZE
			+ (MAX_MEM_REGIONS * APP_PTR_TABLE_REGION_SIZE);
}
