package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/** A data element that contains just a key. */
public class KeyDataElement implements AbstractDataElement {
	private final int key;

	public KeyDataElement(int key) {
		this.key = key;
	}

	@Override
	public final void addToBuffer(ByteBuffer buffer, EIEIOType eieioType) {
		if (eieioType.payloadBytes != 0) {
			throw new IllegalArgumentException(
					"The type specifies a payload, but this element has no"
							+ " payload");
		}
		switch (eieioType) {
		case KEY_16_BIT:
			buffer.putShort((short) key);
			return;
		case KEY_32_BIT:
			buffer.putInt(key);
			return;
		default:
			throw new IllegalArgumentException("Unknown type");
		}
	}
}
