/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	public static final int APP_PTR_TABLE_HEADER_SIZE = INT_SIZE * 2;

	/**
	 * The size of a Data Specification region description, in bytes.
	 * Note that each description consists of a pointer and 2 uint32_t variables
	 * (pointer, checksum, n_words).
	 */
	public static final int APP_PTR_TABLE_REGION_SIZE = INT_SIZE * 3;

	/**
	 * The size of the Data Specification table, in bytes.
	 */
	public static final int APP_PTR_TABLE_BYTE_SIZE =
			APP_PTR_TABLE_HEADER_SIZE
			+ (MAX_MEM_REGIONS * APP_PTR_TABLE_REGION_SIZE);

	// Constants used by DSG command encoding: payload length field

	/** DSG command is one word long. */
	static final int LEN1 = 0;

	/** DSG command is two words long. */
	static final int LEN2 = 1;

	/** DSG command is three words long. */
	static final int LEN3 = 2;

	/** DSG command is four words long. */
	static final int LEN4 = 3;

	// Constants used by DSG command encoding: register usage field

	/** DSG command uses no registers. */
	static final int NO_REGS = 0;

	/** DSG command uses just the src2 register. */
	static final int SRC2_ONLY = 1;

	/** DSG command uses just the src1 register. */
	static final int SRC1_ONLY = 2;

	/** DSG command uses just the src1 and src2 registers. */
	static final int SRC1_AND_SRC2 = 3;

	/** DSG command uses just the dest register. */
	static final int DEST_ONLY = 4;

	/** DSG command uses just the src2 and dest registers. */
	static final int DEST_AND_SRC2 = 5;

	/** DSG command uses just the src1 and dest registers. */
	static final int DEST_AND_SRC1 = 6;

	/** DSG command uses the src1, src2 and dest registers. */
	static final int ALL_REGS = 7;

	/** return values from functions of the data spec executor. */
	static final int END_SPEC_EXECUTOR = -1;
}
