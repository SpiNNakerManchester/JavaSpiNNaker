package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/** A data element that contains a key and a payload. */
public class KeyPayloadDataElement implements AbstractDataElement {
	private final int key;
	private final int payload;
	private final boolean timestamp;

	/**
	 * Create a data element.
	 * @param key The key in the element.
	 * @param payload The payload in the element.
	 * @param isTimestamp Whether this is a timestamp.
	 */
	public KeyPayloadDataElement(int key, int payload, boolean isTimestamp) {
		this.key = key;
		this.payload = payload;
		this.timestamp = isTimestamp;
	}

	public boolean isTimestamp() {
		return timestamp;
	}

	@Override
	public final void addToBuffer(ByteBuffer buffer, EIEIOType eieioType) {
		if (eieioType.payloadBytes == 0) {
			throw new IllegalArgumentException(
					"The type specifies no payload, but this element has a"
							+ " payload");
		}
		switch (eieioType) {
		case KEY_PAYLOAD_16_BIT:
			buffer.putShort((short) key);
			buffer.putShort((short) payload);
			return;
		case KEY_PAYLOAD_32_BIT:
			buffer.putInt(key);
			buffer.putInt(payload);
			return;
		default:
			throw new IllegalArgumentException("Unknown type");
		}
	}
}
