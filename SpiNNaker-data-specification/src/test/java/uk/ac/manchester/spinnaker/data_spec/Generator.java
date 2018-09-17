package uk.ac.manchester.spinnaker.data_spec;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.data_spec.Commands.BREAK;
import static uk.ac.manchester.spinnaker.data_spec.Commands.END_SPEC;
import static uk.ac.manchester.spinnaker.data_spec.Commands.MV;
import static uk.ac.manchester.spinnaker.data_spec.Commands.NOP;
import static uk.ac.manchester.spinnaker.data_spec.Commands.RESERVE;
import static uk.ac.manchester.spinnaker.data_spec.Commands.SET_WR_PTR;
import static uk.ac.manchester.spinnaker.data_spec.Commands.SWITCH_FOCUS;
import static uk.ac.manchester.spinnaker.data_spec.Commands.WRITE;
import static uk.ac.manchester.spinnaker.data_spec.Commands.WRITE_ARRAY;
import static uk.ac.manchester.spinnaker.data_spec.Constants.DEST_ONLY;
import static uk.ac.manchester.spinnaker.data_spec.Constants.INT_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.LEN1;
import static uk.ac.manchester.spinnaker.data_spec.Constants.LEN2;
import static uk.ac.manchester.spinnaker.data_spec.Constants.LEN3;
import static uk.ac.manchester.spinnaker.data_spec.Constants.NO_REGS;
import static uk.ac.manchester.spinnaker.data_spec.Constants.SRC1_ONLY;
import static uk.ac.manchester.spinnaker.data_spec.Constants.SRC2_ONLY;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/** Severely cut down version of the DSG, for testing only. */
public class Generator {
	private ByteBuffer buffer;

	public static ByteBuffer makeSpec(SpecGen specGen) {
		Generator spec = new Generator();
		specGen.generate(spec);
		ByteBuffer result = spec.buffer.asReadOnlyBuffer();
		result.order(LITTLE_ENDIAN).flip();
		return result;
	}

	public static void makeSpec(File f, SpecGen specGen) throws IOException {
		Generator spec = new Generator();
		specGen.generate(spec);
		try (FileOutputStream os = new FileOutputStream(f)) {
			os.write(spec.buffer.array(), 0, spec.buffer.position());
		}
	}

	public static ByteArrayInputStream makeSpecStream(SpecGen specGen)
			throws IOException {
		Generator spec = new Generator();
		specGen.generate(spec);
		return new ByteArrayInputStream(spec.buffer.array(), 0,
				spec.buffer.position());
	}

	@FunctionalInterface
	interface SpecGen {
		void generate(Generator generator);
	}

	private Generator() {
		buffer = allocate(32 * 1024).order(LITTLE_ENDIAN);
	}

	/**
	 * Various shifts for fields used with
	 * {@link #command(Command,int,int,Object[]) command(...)}.
	 */
	enum Field {
		/** length field. */
		LENGTH(28),
		/** opcode field. */
		COMMAND(20),
		/** signed bit. */
		SIGNED(19),
		/** usage field. */
		USAGE(16),
		/** dest field. */
		DESTINATION(12),
		/** function ID field. */
		FUNCTION(11),
		/** src1 field. */
		SOURCE_1(8),
		/** empty field. */
		EMPTY(7),
		/** shrink field. */
		SHRINK(6),
		/** src2 field. */
		SOURCE_2(4),
		/** immediate value field. */
		IMMEDIATE(0);
		final int offset;

		Field(int offset) {
			this.offset = offset;
		}
	}

	public enum DataType {
		INT8(1, LEN2), INT16(2, LEN2), INT32(4, LEN2), INT64(8, LEN3);
		final int size, writeLength;

		DataType(int size, int writeLength) {
			this.size = size;
			this.writeLength = writeLength;
		}

		void putPacked(ByteBuffer buffer, Number n) {
			switch (this) {
			case INT8:
				buffer.put(n.byteValue());
				break;
			case INT16:
				buffer.putShort(n.shortValue());
				break;
			case INT32:
				buffer.putInt(n.intValue());
				break;
			case INT64:
				buffer.putLong(n.longValue());
				break;
			}
		}

		void putPadded(ByteBuffer buffer, Number n) {
			switch (this) {
			case INT8:
				buffer.put(n.byteValue());
				// Add padding
				buffer.put((byte) 0);
				buffer.put((byte) 0);
				buffer.put((byte) 0);
				break;
			case INT16:
				buffer.putShort(n.shortValue());
				// Add padding
				buffer.putShort((short) 0);
				break;
			case INT32:
				buffer.putInt(n.intValue());
				break;
			case INT64:
				buffer.putLong(n.longValue());
				break;
			}
		}
	}

	/**
	 * Encode the command word and add it to the buffer.
	 *
	 * @param command
	 *            The command to write.
	 * @param length
	 *            The command length. One of {@link Constants#LEN1},
	 *            {@link Constants#LEN2}, {@link Constants#LEN3}, or
	 *            {@link Constants#LEN4}.
	 * @param usage
	 *            The command's register usage. One of
	 *            {@link Constants#NO_REGS}, {@link Constants#SRC1_ONLY},
	 *            {@link Constants#SRC2_ONLY}, {@link Constants#DEST_ONLY},
	 *            {@link Constants#SRC1_AND_SRC2},
	 *            {@link Constants#DEST_AND_SRC1},
	 *            {@link Constants#DEST_AND_SRC2} or {@link Constants#ALL_REGS}.
	 * @param arguments
	 *            Additional fields to be set, as pairs of arguments; the first
	 *            of each pair is a {@link Field} and the second is a
	 *            {@link Number} (encoded as its integer value), a
	 *            {@link Boolean} (encoded as 0 or 1) or a non-Field
	 *            {@link Enum} (encoded as its ordinal value). There must be an
	 *            even number of arguments overall.
	 */
	private void command(Commands command, int length, int usage,
			Object... arguments) {
		int word = command.value << Field.COMMAND.offset;
		word |= length << Field.LENGTH.offset;
		word |= usage << Field.USAGE.offset;
		for (int i = 0; i < arguments.length; i += 2) {
			Field f = (Field) arguments[i];
			Object val = arguments[i + 1];
			if (val instanceof Boolean) {
				word |= (((Boolean) val) ? 1 : 0) << f.offset;
			} else if (val instanceof Enum && !(val instanceof Field)) {
				word |= ((Enum<?>) val).ordinal() << f.offset;
			} else if (val instanceof Number) {
				word |= ((Number) val).intValue() << f.offset;
			} else {
				throw new IllegalArgumentException("arg: " + i + " = " + val);
			}
		}
		buffer.putInt(word);
	}

	public void endSpecification() {
		command(END_SPEC, LEN1, NO_REGS);
		buffer.putInt(-1);
	}

	public void reserveMemoryRegion(int region, int size) {
		reserveMemoryRegion(region, size, false);
	}

	public void reserveMemoryRegion(int region, int size, boolean empty) {
		boolean shrink = false;
		command(RESERVE, LEN2, NO_REGS, Field.EMPTY, empty, Field.SHRINK,
				shrink, Field.IMMEDIATE, region);
		buffer.putInt(size);
	}

	public void switchWriteFocus(int region) {
		command(SWITCH_FOCUS, LEN1, NO_REGS, Field.SOURCE_1, region);
	}

	public void setWritePointer(int address) {
		boolean relative = false;
		command(SET_WR_PTR, LEN2, NO_REGS, Field.IMMEDIATE, relative);
		buffer.putInt(address);
	}

	public void setWritePointerFromRegister(int register) {
		boolean relative = false;
		command(SET_WR_PTR, LEN1, SRC1_ONLY, Field.SOURCE_1, register,
				Field.IMMEDIATE, relative);
	}

	public void setRegisterValue(int register, int value) {
		command(MV, LEN2, DEST_ONLY, Field.DESTINATION, register);
		buffer.putInt(value);
	}

	public void writeArray(Integer... values) {
		writeArray(values, DataType.INT32);
	}

	public void writeArray(Number[] values, DataType type) {
		command(WRITE_ARRAY, LEN2, NO_REGS, Field.IMMEDIATE, type.size);
		int pos = buffer.position();
		buffer.putInt(0); // dummy
		int mark = buffer.position();
		for (Number n : values) {
			type.putPacked(buffer, n);
		}
		buffer.putInt(pos, (buffer.position() - mark) / INT_SIZE);
	}

	public void writeValue(int value) {
		writeValue(value, DataType.INT32);
	}

	public void writeValue(Number value, DataType type) {
		int repeats = 1;
		command(WRITE, type.writeLength, NO_REGS, Field.DESTINATION, type,
				Field.IMMEDIATE, repeats);
		type.putPadded(buffer, value);
	}

	public void writeRepeatedValue(Number value, int register,
			DataType type) {
		command(WRITE, type.writeLength, SRC2_ONLY, Field.DESTINATION, type,
				Field.SOURCE_2, register);
		type.putPadded(buffer, value);
	}

	public void writeValueFromRegister(int register) {
		int repeats = 1;
		DataType type = DataType.INT32;
		command(WRITE, LEN1, SRC1_ONLY, Field.DESTINATION, type, Field.SOURCE_1,
				register, Field.IMMEDIATE, repeats);
	}

	public void nop() {
		command(NOP, LEN1, NO_REGS);
	}

	public void fail() {
		command(BREAK, LEN1, NO_REGS);
	}
}
