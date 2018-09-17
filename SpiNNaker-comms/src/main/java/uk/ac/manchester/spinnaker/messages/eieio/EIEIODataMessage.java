package uk.ac.manchester.spinnaker.messages.eieio;

import static java.lang.Integer.toUnsignedLong;
import static java.lang.Math.floorDiv;
import static java.lang.String.format;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOPrefix.LOWER_HALF_WORD;
import static uk.ac.manchester.spinnaker.transceiver.Utils.newMessageBuffer;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An EIEIO message containing data.
 *
 * @author Sergio Davies
 * @author Donal Fellows
 */
public class EIEIODataMessage implements EIEIOMessage<EIEIODataMessage.Header>,
		Iterable<AbstractDataElement> {
	private final Header header;
	private ByteBuffer elements;
	private ByteBuffer data;

	/**
	 * Create a data message.
	 *
	 * @param eieioType
	 *            The type of data components in the message.
	 */
	public EIEIODataMessage(EIEIOType eieioType) {
		this(eieioType, (byte) 0, null, null, null, null, LOWER_HALF_WORD);
	}

	/**
	 * Deserialise a message.
	 *
	 * @param data
	 *            The data to deserialise.
	 */
	EIEIODataMessage(ByteBuffer data) {
		this.header = new Header(data);
		this.elements = null;
		this.data = data.asReadOnlyBuffer().order(LITTLE_ENDIAN);
	}

	/**
	 * Create a data message.
	 *
	 * @param eieioType
	 *            The type of data components in the message.
	 * @param count
	 *            The number of data components in the message.
	 * @param data
	 *            The serialized data components.
	 * @param keyPrefix
	 *            Any key prefix to apply to the data components.
	 * @param payloadPrefix
	 *            The prefix to apply. Overridden by a non-null timestamp.
	 * @param timestamp
	 *            The timestamp base to apply.
	 * @param prefixType
	 *            The type of prefix to apply.
	 */
	public EIEIODataMessage(EIEIOType eieioType, byte count, ByteBuffer data,
			Short keyPrefix, Integer payloadPrefix, Integer timestamp,
			EIEIOPrefix prefixType) {
		Integer payloadBase = payloadPrefix;
		if (timestamp != null) {
			payloadBase = timestamp;
		}
		header = new Header(eieioType, (byte) 0, keyPrefix, prefixType,
				payloadBase, timestamp != null, count);
		elements = newMessageBuffer();
		this.data = data;
	}

	@Override
	public int minPacketLength() {
		return header.getSize() + header.eieioType.payloadBytes;
	}

	/** @return The maximum number of elements that can fit in the packet. */
	public int getMaxNumElements() {
		return floorDiv(UDP_MESSAGE_MAX_SIZE - header.getSize(),
				header.eieioType.keyBytes + header.eieioType.payloadBytes);
	}

	/** @return The number of elements in the packet. */
	public int getNumElements() {
		return header.getCount();
	}

	/** @return The size of the packet with the current contents. */
	public int getSize() {
		return header.getSize()
				+ (header.eieioType.keyBytes + header.eieioType.payloadBytes)
						* header.getCount();
	}

	/**
	 * Adds a key and payload to the packet.
	 *
	 * @param key
	 *            The key to add
	 * @param payload
	 *            The payload to add
	 * @throws IllegalArgumentException
	 *             If the key or payload is too big for the format, or the
	 *             format doesn't expect a payload
	 */
	public void addKeyAndPayload(int key, int payload) {
		if (toUnsignedLong(key) > header.eieioType.maxValue) {
			throw new IllegalArgumentException(
					format("key %d larger than the maximum allowed of %d",
							toUnsignedLong(key), header.eieioType.maxValue));
		}
		if (toUnsignedLong(payload) > header.eieioType.maxValue) {
			throw new IllegalArgumentException(format(
					"payload %d larger than the maximum allowed of %d",
					toUnsignedLong(payload), header.eieioType.maxValue));
		}
		addElement(new KeyPayloadDataElement(key, payload, header.isTime));
	}

	/**
	 * Adds a key to the packet.
	 *
	 * @param key
	 *            The key to add
	 * @throws IllegalArgumentException
	 *             If the key is too big for the format, or the format expects a
	 *             payload
	 */
	public void addKey(int key) {
		if (toUnsignedLong(key) > header.eieioType.maxValue) {
			throw new IllegalArgumentException(
					format("key %d larger than the maximum allowed of %d",
							toUnsignedLong(key), header.eieioType.maxValue));
		}
		addElement(new KeyDataElement(key));
	}

	/** @return The raw data of this message. */
	public ByteBuffer getData() {
		return data.asReadOnlyBuffer().order(LITTLE_ENDIAN);
	}

	/**
	 * Add an element to the message. The correct type of element must be added,
	 * depending on the header values.
	 *
	 * @param element
	 *            The element to be added
	 * @throws IllegalArgumentException
	 *             If the element is not compatible with the header
	 * @throws IllegalStateException
	 *             If the message was created to read data
	 */
	public void addElement(AbstractDataElement element) {
		if (data != null) {
			throw new IllegalStateException("This packet is read-only");
		}
		element.addToBuffer(elements, header.eieioType);
		header.incrementCount();
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		header.addToBuffer(buffer);
		buffer.put(elements.array(), 0, elements.position());
	}

	@Override
	public Iterator<AbstractDataElement> iterator() {
		final ByteBuffer d =
				data == null ? null : data.duplicate().order(LITTLE_ENDIAN);
		return new Iterator<AbstractDataElement>() {
			private int elementsRead = 0;

			@Override
			public boolean hasNext() {
				return d != null && elementsRead < header.getCount();
			}

			@Override
			public AbstractDataElement next() {
				if (d == null || !hasNext()) {
					throw new NoSuchElementException("read all elements");
				}
				elementsRead++;
				int key;
				Integer payload;
				switch (header.eieioType) {
				case KEY_16_BIT:
					key = d.getShort();
					payload = null;
					break;
				case KEY_PAYLOAD_16_BIT:
					key = d.getShort();
					payload = (int) d.getShort();
					break;
				case KEY_32_BIT:
					key = d.getInt();
					payload = null;
					break;
				case KEY_PAYLOAD_32_BIT:
					key = d.getInt();
					payload = d.getInt();
					break;
				default:
					throw new IllegalStateException();
				}
				if (header.prefix != null) {
					key |= header.prefix << header.prefixType.shift;
				}
				if (header.payloadBase != null) {
					if (payload != null) {
						payload |= header.payloadBase;
					} else {
						payload = header.payloadBase;
					}
				}
				if (payload == null) {
					return new KeyDataElement(key);
				}
				return new KeyPayloadDataElement(key, payload, header.isTime);
			}
		};
	}

	@Override
	public Header getHeader() {
		return header;
	}

	/** EIEIO header for data packets. */
	public static class Header implements EIEIOHeader {
		/** The type of packet (size of various fields). */
		public final EIEIOType eieioType;
		/** The tag on the message. */
		public final byte tag;
		/** The prefix on the message, or <tt>null</tt> for no prefix. */
		public final Short prefix;
		/** How to apply the prefix. */
		public final EIEIOPrefix prefixType;
		/** An offset for the payload. */
		public final Integer payloadBase;
		/** Whether payloads are times. */
		public final boolean isTime;
		/** The number of items in the packet. */
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
		public Header(EIEIOType eieioType, byte tag, Short prefix,
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

		private static int bits(byte b, int bitbase) {
			return (b >>> bitbase) & TWO_BITS_MASK;
		}

		/**
		 * @param buffer
		 *            The buffer to deserialise from.
		 */
		private Header(ByteBuffer buffer) {
			count = buffer.get();
			byte flags = buffer.get();
			boolean havePrefix = bit(flags, PREFIX_BIT) != 0;
			if (havePrefix) {
				prefixType =
						EIEIOPrefix.getByValue(bit(flags, PREFIX_TYPE_BIT));
			} else {
				prefixType = null;
			}
			boolean havePayload = bit(flags, PAYLOAD_BIT) != 0;
			isTime = bit(flags, TIME_BIT) != 0;
			eieioType = EIEIOType.getByValue(bits(flags, TYPE_BITS));
			tag = (byte) bits(flags, TAG_BITS);
			if (havePrefix) {
				prefix = buffer.getShort();
			} else {
				prefix = null;
			}
			if (havePayload) {
				switch (eieioType) {
				case KEY_PAYLOAD_16_BIT:
				case KEY_16_BIT:
					payloadBase = Short.toUnsignedInt(buffer.getShort());
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

		/** @return The unit size per data component. */
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
}
