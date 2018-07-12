package uk.ac.manchester.spinnaker.messages.model;

import java.nio.ByteBuffer;

/** Enum for data types */
public enum DataType {
	BYTE(1), SHORT(2), INT(4), LONG(8), BYTE_ARRAY(16);
	public final int value;

	private DataType(int value) {
		this.value = value;
	}

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