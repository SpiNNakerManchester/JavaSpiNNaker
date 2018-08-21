package uk.ac.manchester.spinnaker.messages.model;

import java.nio.ByteBuffer;

/** Enum for data types of system variables. */
public enum DataType {
	/** The value is one byte long. */
	BYTE(1),
	/** The value is two bytes long. */
	SHORT(2),
	/** The value is four bytes long. */
	INT(4),
	/** The value is eight bytes long. */
	LONG(8),
	/** The value is an array of bytes. */
	BYTE_ARRAY(16);

	/** The SCAMP data type descriptor code. */
	public final int value;

	DataType(int value) {
		this.value = value;
	}

	/**
	 * Writes an object described by this data type into the given buffer at the
	 * <i>position</i> as a contiguous range of bytes. This assumes that the
	 * buffer has been configured to be
	 * {@linkplain java.nio.ByteOrder#LITTLE_ENDIAN little-endian} and that its
	 * <i>position</i> is at the point where this method should begin writing.
	 * Once it has finished, the <i>position</i> should be immediately after the
	 * last byte written by this method.
	 *
	 * @param value
	 *            The value to write.
	 * @param buffer
	 *            The buffer to write into.
	 */
	void addToBuffer(Object value, ByteBuffer buffer) {
		switch (this) {
		case BYTE:
			buffer.put(((Number) value).byteValue());
			return;
		case SHORT:
			buffer.putShort(((Number) value).shortValue());
			return;
		case INT:
			buffer.putInt(((Number) value).intValue());
			return;
		case LONG:
			buffer.putLong(((Number) value).longValue());
			return;
		case BYTE_ARRAY:
			buffer.put((byte[]) value);
			return;
		default:
			throw new Error("unreachable?");
		}
	}
}
