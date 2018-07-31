package uk.ac.manchester.spinnaker.messages.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Enum for data types. */
public enum DataType {
	BYTE(1), SHORT(2), INT(4), LONG(8), BYTE_ARRAY(16);

	/** The SCAMP data type descriptor code. */
	public final int value;

	DataType(int value) {
		this.value = value;
	}

	/**
	 * Writes an object described by this data type into the given buffer at the
	 * <i>position</i> as a contiguous range of bytes. This assumes that the
	 * buffer has been configured to be {@linkplain ByteOrder#LITTLE_ENDIAN
	 * little-endian} and that its <i>position</i> is at the point where this
	 * method should begin writing. Once it has finished, the <i>position</i>
	 * should be immediately after the last byte written by this method.
	 *
	 * @param value
	 *            The value to write.
	 * @param buffer
	 *            The buffer to write into.
	 */
	public void addToBuffer(Object value, ByteBuffer buffer) {
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
		}
	}
}