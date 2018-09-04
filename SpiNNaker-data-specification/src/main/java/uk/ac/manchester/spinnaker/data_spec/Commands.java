package uk.ac.manchester.spinnaker.data_spec;

import java.util.HashMap;
import java.util.Map;

/** Set of opcodes for the spec executor. */
public enum Commands {
	/** Halts spec execution with an error. */
	BREAK(0x00),
	/** No operation. Can be used as a filler. */
	NOP(0x01),
	/** Reserves a block of memory ready for filling. */
	RESERVE(0x02),
	/** Releases previously reserved memory. */
	FREE(0x03),
	/** Declares a new random number generator. */
	DECLARE_RNG(0x05),
	/** Declares a new random distribution. */
	DECLARE_RANDOM_DIST(0x06),
	/** Returns a random number drawn from the given distribution. */
	GET_RANDOM_NUMBER(0x07),
	/** Begins declaration of new structure. */
	START_STRUCT(0x10),
	/** Declare single element in a structure. */
	STRUCT_ELEM(0x11),
	/** Ends declaration of new structure. */
	END_STRUCT(0x12),
	/** Begins definition of a packing specification. */
    START_PACKSPEC(0x1A),
	/**
	 * Writes one bit field inside a single parameter from a bit field of a
	 * source parameter.
	 */
    PACK_PARAM(0x1B),
	/** Ends definition of a packing specification. */
    END_PACKSPEC(0x1C),
	/** Begins definition of a function to write data structures to memory. */
	START_CONSTRUCTOR(0x20),
	/** Ends definition of the write function. */
	END_CONSTRUCTOR(0x25),
	/** Invokes a constructor to build a data structure. */
	CONSTRUCT(0x40),
	/** Performs a simple read operation. */
	READ(0x41),
	/** Performs a simple write or block write operation. */
	WRITE(0x42),
	/** Performs a write from an array. */
	WRITE_ARRAY(0x43),
	/** Performs a write from a predefined structure. */
	WRITE_STRUCT(0x44),
	/** Copies a block of data from one area to another. */
	BLOCK_COPY(0x45),
	/**
	 * Swap between different reserved memory regions to work on several at the
	 * same time.
	 */
	SWITCH_FOCUS(0x50),
	/** Set-up a loop. */
	LOOP(0x51),
	/** Early exit from a loop. */
	BREAK_LOOP(0x52),
	/** End of a loop. */
	END_LOOP(0x53),
	/**
	 * Perform a condition and execute the following instructions only if the
	 * condition is true.
	 */
	IF(0x55),
	/** Else clause for associated IF statement. */
	ELSE(0x56),
	/** Close block of instructions begun with the IF instruction. */
	END_IF(0x57),
	/** Place a value in a register, from an immediate or another register. */
	MV(0x60),
	/** Copy current write address to a register. */
	GET_WR_PTR(0x63),
	/**
	 * Move the write pointer to a new location, either relative to the start of
	 * this reserved memory area or relative to the current write pointer.
	 */
	SET_WR_PTR(0x64),
	/**
	 * Moves the write pointer so that it points to the next block with a given
	 * address granularity.
	 */
	ALIGN_WR_PTR(0x65),
	/** Perform arithmetic operation with operand 2 coming from a register. */
	ARITH_OP(0x67),
	/** Perform logical operation with operand 2 coming from a register. */
	LOGIC_OP(0x68),
	/** Reformats a value in an internal register. */
	REFORMAT(0x6A),
	/** Create an identical copy of a structure. */
	COPY_STRUCT(0x70),
	/** Copy a parameter from one structure to another. */
	COPY_PARAM(0x71),
	/**
	 * Modify a single parameter in a structure using an immediate value or
	 * register held-value.
	 */
	WRITE_PARAM(0x72),
	/** Load the value of a structure parameter in a register. */
	READ_PARAM(0x73),
	/** Modify a single parameter in a structure. */
	WRITE_PARAM_COMPONENT(0x74),
	/** Output the value of a register to the screen. */
	PRINT_VAL(0x80),
	/** Print a text string to the screen. */
	PRINT_TXT(0X81),
	/** Print the current state of one structure to the screen. */
	PRINT_STRUCT(0X82),
	/** Cleanly ends the parsing of the data specs. */
	END_SPEC(0XFF);
	/**
	 * The value of the command, from the Data Specification specification
	 * itself.
	 */
	public final int value;
	private static final Map<Integer, Commands> MAP;

	Commands(int value) {
		this.value = value;
	}

	static {
		MAP = new HashMap<>();
		for (Commands cmd : values()) {
			MAP.put(cmd.value, cmd);
		}
	}

	public static Commands get(int value) {
		return MAP.get(value);
	}
}
