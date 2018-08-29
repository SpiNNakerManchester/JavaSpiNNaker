package uk.ac.manchester.spinnaker.data_spec;

/**
 * Constants used by the Data Structure Generator (DSG) and the Specification
 * Executor.
 */
final class Constants {
	private Constants() {
	}

	private static final int WORD_SIZE = 4;
	static final int INT_SIZE = 4;
	static final int LONG_SIZE = 8;

	/** Data spec magic number. */
	static final int DSG_MAGIC_NUM = 0x5B7CA17E;

	/** Application data magic number. */
	static final int APPDATA_MAGIC_NUM = 0xAD130AD6;

	/** Version of the file produced by the DSE. */
	static final int DSE_VERSION = 0x00010000;

	// DSG Arrays and tables sizes
	static final int MAX_REGISTERS = 16;
	static final int MAX_MEM_REGIONS = 16;
	static final int MAX_STRUCT_SLOTS = 16;
	static final int MAX_STRUCT_ELEMENTS = 255;
	static final int MAX_PACKSPEC_SLOTS = 16;
	static final int MAX_CONSTRUCTORS = 16;
	static final int MAX_PARAM_LISTS = 16;
	static final int MAX_RNGS = 16;
	static final int MAX_RANDOM_DISTS = 16;

	static final int APP_PTR_TABLE_HEADER_BYTE_SIZE = WORD_SIZE * 2;
	static final int APP_PTR_TABLE_BYTE_SIZE =
			APP_PTR_TABLE_HEADER_BYTE_SIZE + MAX_MEM_REGIONS * WORD_SIZE;

	// Constants used by DSG command encoding:
	static final int LEN1 = 0;
	static final int LEN2 = 1;
	static final int LEN3 = 2;
	static final int LEN4 = 3;

	static final int NO_REGS = 0;
	static final int DEST_ONLY = 4;
	static final int SRC1_ONLY = 2;
	static final int SRC1_AND_SRC2 = 3;
	static final int DEST_AND_SRC1 = 6;
	static final int ALL_REGS = 7;

	/** return values from functions of the data spec executor. */
	static final int END_SPEC_EXECUTOR = -1;
}
