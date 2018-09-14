package uk.ac.manchester.spinnaker.data_spec;

import static java.lang.String.format;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.data_spec.Commands.END_SPEC;
import static uk.ac.manchester.spinnaker.data_spec.Commands.MV;
import static uk.ac.manchester.spinnaker.data_spec.Commands.RESERVE;
import static uk.ac.manchester.spinnaker.data_spec.Commands.SET_WR_PTR;
import static uk.ac.manchester.spinnaker.data_spec.Commands.SWITCH_FOCUS;
import static uk.ac.manchester.spinnaker.data_spec.Commands.WRITE;
import static uk.ac.manchester.spinnaker.data_spec.Commands.WRITE_ARRAY;
import static uk.ac.manchester.spinnaker.data_spec.Constants.END_SPEC_EXECUTOR;
import static uk.ac.manchester.spinnaker.data_spec.Constants.INT_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.LEN2;
import static uk.ac.manchester.spinnaker.data_spec.Constants.LEN3;
import static uk.ac.manchester.spinnaker.data_spec.Constants.LONG_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_REGISTERS;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.BitField;

import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;
import uk.ac.manchester.spinnaker.data_spec.exceptions.NoMoreException;
import uk.ac.manchester.spinnaker.data_spec.exceptions.NoRegionSelectedException;
import uk.ac.manchester.spinnaker.data_spec.exceptions.RegionInUseException;
import uk.ac.manchester.spinnaker.data_spec.exceptions.RegionNotAllocatedException;
import uk.ac.manchester.spinnaker.data_spec.exceptions.RegionUnfilledException;
import uk.ac.manchester.spinnaker.data_spec.exceptions.UnknownTypeLengthException;

/**
 * Functions that implement {@linkplain Operation operations} for the
 * {@link Executor}.
 *
 * @author Donal Fellows
 * @see OperationMapper
 */
@SuppressWarnings("unused")
class Functions implements FunctionAPI {
	private static final int SIZE_FIELD = 28;
	private static final int OPCODE_FIELD = 20;
	private static final int DEST_FLAG = 18;
	private static final int SRC1_FLAG = 17;
	private static final int SRC2_FLAG = 16;
	private static final int DEST_FIELD = 12;
	private static final int SRC1_FIELD = 8;
	private static final int SRC2_FIELD = 4;
	private static final int BIT_MASK = 0b00000001;
	private static final int SIZE_MASK = 0b00000011;
	private static final int REG_MASK = 0b00001111;
	private static final int OPCODE_MASK = 0b11111111;

	/** How to extract the size field from the bit-encoded form. */
	private static final BitField SIZE = new BitField(SIZE_MASK << SIZE_FIELD);
	/** How to extract the opcode field from the bit-encoded form. */
	static final BitField OPCODE = new BitField(OPCODE_MASK << OPCODE_FIELD);
	/** How to extract the dest-is-register flag from the bit-encoded form. */
	private static final BitField DEST_BIT = new BitField(1 << DEST_FLAG);
	/** How to extract the src1-is-register flag from the bit-encoded form. */
	private static final BitField SRC1_BIT = new BitField(1 << SRC1_FLAG);
	/** How to extract the src2-is-register flag from the bit-encoded form. */
	private static final BitField SRC2_BIT = new BitField(1 << SRC2_FLAG);
	/** How to extract the dest field from the bit-encoded form. */
	private static final BitField DEST = new BitField(REG_MASK << DEST_FIELD);
	/** How to extract the src1 field from the bit-encoded form. */
	private static final BitField SRC1 = new BitField(REG_MASK << SRC1_FIELD);
	/** How to extract the src2 field from the bit-encoded form. */
	private static final BitField SRC2 = new BitField(REG_MASK << SRC2_FIELD);
	/** How to extract the data length field from the bit-encoded form. */
	private static final BitField DATA_LEN =
			new BitField(SIZE_MASK << DEST_FIELD);
	/** How to extract the region field from the bit-encoded form. */
	private static final BitField REGION = new BitField(0b00011111);
	/** How to extract the unfilled flag from the bit-encoded form. */
	private static final BitField UNFILLED = new BitField(0b10000000);
	/** How to extract the relative flag from the bit-encoded form. */
	private static final BitField RELATIVE = new BitField(0b00000001);
	/** How to extract the repeats field from the bit-encoded form. */
	private static final BitField REPEATS = new BitField(0b11111111);

	private static final int SIZE_LOW_BITS = 0b00000011;

	/** Where we are reading the data spec from. */
	private final ByteBuffer spec;
	/** How much space do we have available? Maximum <i>per region</i>. */
	private int memorySpace;
	/** How much space has been allocated. */
	int spaceAllocated;
	/** What is the current region that we're writing to. */
	private Integer currentRegion;
	/** The model registers, an array of 16 ints. */
	private final int[] registers;
	/** The collection of memory regions that can be written to. */
	private final MemoryRegionCollection memRegions;

	private int packedCommand;
	private int cmdSize;
	private int opcode;
	private Integer dest;
	private Integer src1;
	private Integer src2;
	private int dataLength;

	/**
	 * Create an instance.
	 *
	 * @param input
	 *            Where to read immediate payload arguments from.
	 * @param memorySpace
	 *            What's the maximum size of memory that a memory region may
	 *            occupy.
	 * @param memRegions
	 *            Where are we storing the memory regions that we are
	 *            allocating.
	 */
	Functions(ByteBuffer input, int memorySpace,
			MemoryRegionCollection memRegions) {
		spec = input;
		this.memorySpace = memorySpace;
		spaceAllocated = 0;
		currentRegion = null;
		registers = new int[MAX_REGISTERS];
		this.memRegions = memRegions;
	}

	private MemoryRegion getRegion() {
		if (currentRegion == null) {
			return null;
		}
		return memRegions.get(currentRegion);
	}

	@Override
	public void unpack(int command) {
		packedCommand = command;
		cmdSize = SIZE.getValue(command);
		opcode = OPCODE.getValue(command);
		dest = DEST_BIT.isSet(command) ? DEST.getValue(command) : null;
		src1 = SRC1_BIT.isSet(command) ? SRC1.getValue(command) : null;
		src2 = SRC2_BIT.isSet(command) ? SRC2.getValue(command) : null;
		dataLength = 1 << DATA_LEN.getValue(command);
	}

	/** This command reserves a region and assigns some memory space to it. */
	@Operation(RESERVE)
	public void reserve() throws DataSpecificationException {
		int region = REGION.getValue(packedCommand);
		if (cmdSize != LEN2) {
			throw new DataSpecificationException(format(
					"Command %s requires one word as argument (total 2 words),"
							+ " but the current encoding (%08X) is specified"
							+ " to be %d words long",
					RESERVE, packedCommand, cmdSize));
		}
		if (!memRegions.isEmpty(region)) {
			throw new RegionInUseException(region);
		}
		boolean unfilled = UNFILLED.isSet(packedCommand);
		// Get the rounded-up size
		int size = (spec.getInt() + SIZE_LOW_BITS) & ~SIZE_LOW_BITS;
		if (size < 0 || size >= memorySpace) {
			throw new IllegalArgumentException(
					"region size is out of bounds; is " + size
							+ " but needs to be in 0 to " + (memorySpace - 1)
							+ " (inclusive)");
		}
		memRegions.set(region, new MemoryRegion(0, unfilled, size));
		spaceAllocated += size;
	}

	/**
	 * This command writes the given value in the specified region a number of
	 * times as identified by either a value in the command or a register value.
	 */
	@Operation(WRITE)
	public void write() throws DataSpecificationException {
		int numRepeats = (src2 != null ? registers[src2]
				: REPEATS.getValue(packedCommand));
		long value;
		if (src1 != null) {
			value = registers[src1];
		} else if (cmdSize == LEN2 && dataLength != LONG_SIZE) {
			value = spec.getInt();
		} else if (cmdSize == LEN3 && dataLength == LONG_SIZE) {
			value = spec.getLong();
		} else {
			throw new DataSpecificationException(String.format(
					"Command %s requires a value as an argument, but the "
							+ "current encoding (%08X) is specified to be %d "
							+ "words long and the data length command argument "
							+ "is specified to be %d bytes long",
					WRITE, packedCommand, cmdSize, dataLength));
		}
		writeToMemory(value, dataLength, numRepeats, WRITE);
	}

	/** This command writes an array of values in the specified region. */
	@Operation(WRITE_ARRAY)
	public void writeArray() throws DataSpecificationException {
		byte[] bytes = new byte[spec.getInt() * INT_SIZE];
		spec.get(bytes);
		writeToMemory(bytes, WRITE_ARRAY);
	}

	/**
	 * This command switches the focus to the desired, already allocated, memory
	 * region.
	 */
	@Operation(SWITCH_FOCUS)
	public void switchFocus() throws DataSpecificationException {
		int region =
				(src1 != null ? registers[src1] : SRC1.getValue(packedCommand));
		if (memRegions.isEmpty(region)) {
			throw new RegionUnfilledException(region, SWITCH_FOCUS);
		}
		currentRegion = region;
	}

	/**
	 * This command moves an immediate value to a register or copies the value
	 * of a register to another register.
	 */
	@Operation(MV)
	public void move() throws DataSpecificationException {
		if (dest == null) {
			throw new DataSpecificationException(
					"destination register not correctly specified");
		}
		if (src1 != null) {
			registers[dest] = registers[src1];
		} else {
			registers[dest] = spec.getInt();
		}
	}

	/**
	 * This command sets the position where writes will be performed.
	 */
	@Operation(SET_WR_PTR)
	public void setWritePointer() throws DataSpecificationException {
		int address;
		if (src1 != null) {
			// the data is a register
			address = registers[src1];
		} else {
			// the data is a raw address
			address = spec.getInt();
		}

		// check that the address is relative or absolute
		if (RELATIVE.isSet(packedCommand)) {
			// relative to its current write pointer
			if (getRegion() == null) {
				throw new NoRegionSelectedException(
						"the write pointer for this region is currently"
								+ " undefined");
			}

			// relative to the base address of the region (obsolete)
			address += getRegion().getWritePointer();
		}

		// update write pointer
		getRegion().setWritePointer(address);
	}

	/**
	 * This command indicates that the data specification has completed
	 * successfully.
	 *
	 * @return Special end-of-execution token.
	 */
	@Operation(END_SPEC)
	public int endSpecification() throws DataSpecificationException {
		int p = spec.position();
		int value = spec.getInt();
		if (value != END_SPEC_EXECUTOR) {
			throw new DataSpecificationException(format(
					"Command END_SPEC requires an argument equal to -1. The "
							+ "current argument value is %d (from %d)",
					value, p));
		}
		return END_SPEC_EXECUTOR;
	}

	private void writeToMemory(long value, int dataLen, int numRepeats,
			Commands command) throws DataSpecificationException {
		ByteBuffer b = allocate(numRepeats * dataLen).order(LITTLE_ENDIAN);
		for (int i = 0; i < numRepeats; i++) {
			switch (dataLen) {
			case 1:
				b.put((byte) value);
				break;
			case 2:
				b.putShort((short) value);
				break;
			case INT_SIZE:
				b.putInt((int) value);
				break;
			case LONG_SIZE:
				b.putLong((long) value);
				break;
			default:
				throw new UnknownTypeLengthException(dataLen, command);
			}
		}
		writeToMemory(b.array(), command);
	}

	private void writeToMemory(byte[] array, Commands command)
			throws DataSpecificationException {
		// Sanity checks
		if (currentRegion == null) {
			throw new NoRegionSelectedException(command);
		}
		MemoryRegion r = getRegion();
		if (r == null) {
			throw new RegionNotAllocatedException(currentRegion, command);
		}

		// It must have enough space
		if (r.getRemainingSpace() < array.length) {
			throw new NoMoreException(r.getRemainingSpace(), array.length,
					currentRegion);
		}

		// We can safely write
		r.writeIntoRegionData(array);
	}
}
