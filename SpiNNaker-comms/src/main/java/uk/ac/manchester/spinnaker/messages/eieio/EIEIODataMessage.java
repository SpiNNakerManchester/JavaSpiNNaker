package uk.ac.manchester.spinnaker.messages.eieio;

import static java.lang.String.format;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.transceiver.Utils.newMessageBuffer;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class EIEIODataMessage
		implements EIEIOMessage, Iterable<AbstractDataElement> {
	public final EIEIODataHeader header;
	private ByteBuffer elements;
	private ByteBuffer data;
	private int offset;

	public EIEIODataMessage(EIEIOType eieioType) {
		this(eieioType, (byte) 0, null, 0, null, null, null,
				EIEIOPrefix.LOWER_HALF_WORD);
	}

	public EIEIODataMessage(EIEIODataHeader header, ByteBuffer data,
			int offset) {
		this.header = header;
		this.elements = null;
		this.data = data;
		this.offset = offset;
	}

	public EIEIODataMessage(EIEIOType eieioType, byte count, ByteBuffer data,
			int offset, Short key_prefix, Integer payload_prefix,
			Integer timestamp, EIEIOPrefix prefix_type) {
		Integer payload_base = payload_prefix;
		if (timestamp != null) {
			payload_base = timestamp;
		}
		header = new EIEIODataHeader(eieioType, (byte) 0, key_prefix,
				prefix_type, payload_base, timestamp != null, count);
		elements = newMessageBuffer();
		this.data = data;
		this.offset = offset;
	}

	/** Get the minimum length of a message instance in bytes */
	public int minPacketLength() {
		return header.getSize() + header.eieioType.payloadBytes;
	}

	/** The maximum number of elements that can fit in the packet */
	public int getMaxNumElements() {
		return Math.floorDiv(UDP_MESSAGE_MAX_SIZE - header.getSize(),
				header.eieioType.keyBytes + header.eieioType.payloadBytes);
	}

	/**
	 * The number of elements in the packet
	 */
	public int getNumElements() {
		return header.getCount();
	}

	/** The size of the packet with the current contents */
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
		if (key > header.eieioType.maxValue) {
			throw new IllegalArgumentException(
					format("key %d larger than the maximum allowed of %d", key,
							header.eieioType.maxValue));
		}
		if (payload > header.eieioType.maxValue) {
			throw new IllegalArgumentException(
					format("payload %d larger than the maximum allowed of %d",
							payload, header.eieioType.maxValue));
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
		if (key > header.eieioType.maxValue) {
			throw new IllegalArgumentException(
					format("key %d larger than the maximum allowed of %d", key,
							header.eieioType.maxValue));
		}
		addElement(new KeyDataElement(key));
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
		final int initialOffset = offset;
		return new Iterator<AbstractDataElement>() {
			private int elements_read = 0;
			private int offset = initialOffset;

			@Override
			public boolean hasNext() {
				return data != null && elements_read < header.getCount();
			}

			@Override
			public AbstractDataElement next() {
				if (!hasNext()) {
					throw new NoSuchElementException("read all elements");
				}
				elements_read++;
				int key = 0;
				Integer payload = null;
				switch (header.eieioType) {
				case KEY_16_BIT:
					key = (int) data.getShort(offset);
					offset += 2;
					break;
				case KEY_PAYLOAD_16_BIT:
					key = (int) data.getShort(offset);
					offset += 2;
					payload = (int) data.getShort(offset);
					offset += 2;
					break;
				case KEY_32_BIT:
					key = data.getInt(offset);
					offset += 4;
					break;
				case KEY_PAYLOAD_32_BIT:
					key = data.getInt(offset);
					offset += 4;
					payload = data.getInt(offset);
					offset += 4;
					break;
				}
				if (header.prefix != null) {
					if (header.prefixType == EIEIOPrefix.UPPER_HALF_WORD) {
						key |= header.prefix << 16;
					} else {
						key |= header.prefix;
					}
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
				} else {
					return new KeyPayloadDataElement(key, payload,
							header.isTime);
				}
			}
		};
	}
}
