package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/** EIEIO header for data packets. */
public class EIEIODataHeader {
	public final EIEIOType eieioType;
	public final byte tag;
	public final Short prefix;
	public final EIEIOPrefix prefixType;
	public final Integer payloadBase;
	public final boolean isTime;
	private byte count;

	/**
	 * @param eieioType
	 *            the type of message
	 * @param tag
	 *            the tag of the message
	 * @param prefix
	 *            the key prefix of the message
	 * @param prefixType
	 *            the position of the prefix (upper or lower)
	 * @param payloadBase
	 *            The base payload to be applied
	 * @param isTime
	 *            true if the payloads should be taken to be timestamps, or
	 *            false otherwise
	 * @param count
	 *            Count of the number of items in the packet
	 */
	public EIEIODataHeader(EIEIOType eieioType, byte tag, Short prefix,
			EIEIOPrefix prefixType, Integer payloadBase, boolean isTime,
			byte count) {
		this.eieioType = eieioType;
		this.tag = tag;
		this.prefix = prefix;
		this.prefixType = prefixType;
		this.payloadBase = payloadBase;
		this.isTime = isTime;
		this.count = count;
	}

	public EIEIODataHeader(ByteBuffer buffer, int offset) {
		throw new IllegalArgumentException("FIXME");// FIXME
	}

	public byte getCount() {
		return count;
	}

	public void setCount(byte count) {
		this.count = count;
	}

	public void incrementCount() {
		count++;
	}

	public void resetCount() {
		count = 0;
	}

	public int getSize() {
		int size = 2;
		if (prefix != null) {
			size += 2;
		}
		if (payloadBase != null) {
			size += eieioType.keyBytes;
		}
		return size;
	}

	/**
	 * Writes this header into the given buffer. This is so that a message can
	 * be sent.
	 *
	 * @param buffer
	 *            The buffer to write into.
	 */
	public void addToBuffer(ByteBuffer buffer) {
		byte data = 0;
		if (prefix != null) {
			data |= 1 << 7;
			data |= prefixType.getValue() << 6;
		}
		if (payloadBase != null) {
			data |= 1 << 5;
		}
		if (isTime) {
			data |= 1 << 4;
		}
		data |= eieioType.getValue() << 2;
		data |= tag;

		if (payloadBase != null) {
			buffer.put(count);
			buffer.put(data);
			if (prefix != null) {
				buffer.putShort(prefix);
			}
			return;
		}
		switch (eieioType) {
		case KEY_PAYLOAD_16_BIT:
		case KEY_16_BIT:
			buffer.put(count);
			buffer.put(data);
			if (prefix != null) {
				buffer.putShort(prefix);
			}
			buffer.putShort((short) (int) payloadBase);
			return;
		case KEY_PAYLOAD_32_BIT:
		case KEY_32_BIT:
			buffer.put(count);
			buffer.put(data);
			if (prefix != null) {
				buffer.putShort(prefix);
			}
			buffer.putInt(payloadBase);
			return;
		}
		throw new IllegalStateException("unexpected EIEIO type");
	}
}
