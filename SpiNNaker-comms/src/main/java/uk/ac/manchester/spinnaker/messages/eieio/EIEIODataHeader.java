package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/** EIEIO header for data packets. */
public class EIEIODataHeader implements EIEIOHeader {
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

	private static int bit(byte b, int bit) {
		return (b >>> bit) & 1;
	}

	public EIEIODataHeader(ByteBuffer buffer) {
		count = buffer.get();
		byte data = buffer.get();
		boolean havePrefix = bit(data, PREFIX_BIT) != 0;
		if (havePrefix) {
			prefixType = EIEIOPrefix.getByValue(bit(data, PREFIX_TYPE_BIT));
		} else {
			prefixType = null;
		}
		boolean havePayload = bit(data, PAYLOAD_BIT) != 0;
		isTime = bit(data, TIME_BIT) != 0;
		eieioType = EIEIOType.getByValue((data >>> TYPE_BITS) & TWO_BITS_MASK);
		tag = (byte) ((data >>> TAG_BITS) & TWO_BITS_MASK);
		if (havePrefix) {
			prefix = buffer.getShort();
		} else {
			prefix = null;
		}
		if (havePayload) {
			switch (eieioType) {
			case KEY_PAYLOAD_16_BIT:
			case KEY_16_BIT:
				payloadBase = (int) buffer.getShort();
				break;
			case KEY_PAYLOAD_32_BIT:
			case KEY_32_BIT:
				payloadBase = buffer.getInt();
				break;
			default:
				payloadBase = null;
			}
		} else {
			payloadBase = null;
		}
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

	private static final int SHORT_WIDTH = 2;

	public int getSize() {
		int size = SHORT_WIDTH;
		if (prefix != null) {
			size += SHORT_WIDTH;
		}
		if (payloadBase != null) {
			size += eieioType.keyBytes;
		}
		return size;
	}

	private static final int PREFIX_BIT = 7;
	private static final int PREFIX_TYPE_BIT = 6;
	private static final int PAYLOAD_BIT = 5;
	private static final int TIME_BIT = 4;
	private static final int TYPE_BITS = 2;
	private static final int TAG_BITS = 0;
	private static final int TWO_BITS_MASK = 0b00000011;

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		byte data = 0;
		if (prefix != null) {
			data |= 1 << PREFIX_BIT;
			data |= prefixType.getValue() << PREFIX_TYPE_BIT;
		}
		if (payloadBase != null) {
			data |= 1 << PAYLOAD_BIT;
		}
		if (isTime) {
			data |= 1 << TIME_BIT;
		}
		data |= eieioType.getValue() << TYPE_BITS;
		data |= tag << TAG_BITS;

		buffer.put(count);
		buffer.put(data);
		if (prefix != null) {
			buffer.putShort(prefix);
		}
		if (payloadBase == null) {
			return;
		}
		switch (eieioType) {
		case KEY_PAYLOAD_16_BIT:
		case KEY_16_BIT:
			buffer.putShort((short) (int) payloadBase);
			return;
		case KEY_PAYLOAD_32_BIT:
		case KEY_32_BIT:
			buffer.putInt(payloadBase);
			return;
		default:
			throw new IllegalStateException("unexpected EIEIO type");
		}
	}
}
