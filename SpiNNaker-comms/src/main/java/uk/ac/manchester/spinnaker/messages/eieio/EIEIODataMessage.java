package uk.ac.manchester.spinnaker.messages.eieio;

import static java.lang.Integer.toUnsignedLong;
import static java.lang.Math.floorDiv;
import static java.lang.String.format;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOPrefix.LOWER_HALF_WORD;
import static uk.ac.manchester.spinnaker.transceiver.Utils.newMessageBuffer;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class EIEIODataMessage
		implements EIEIOMessage, Iterable<AbstractDataElement> {
	public final EIEIODataHeader header;
	private ByteBuffer elements;
	private ByteBuffer data;

	public EIEIODataMessage(EIEIOType eieioType) {
		this(eieioType, (byte) 0, null, null, null, null, LOWER_HALF_WORD);
	}

	public EIEIODataMessage(EIEIODataHeader header, ByteBuffer data) {
		this.header = header;
		this.elements = null;
		this.data = data.asReadOnlyBuffer();
	}

	public EIEIODataMessage(EIEIOType eieioType, byte count, ByteBuffer data,
			Short keyPrefix, Integer payloadPrefix, Integer timestamp,
			EIEIOPrefix prefixType) {
		Integer payloadBase = payloadPrefix;
		if (timestamp != null) {
			payloadBase = timestamp;
		}
		header = new EIEIODataHeader(eieioType, (byte) 0, keyPrefix, prefixType,
				payloadBase, timestamp != null, count);
		elements = newMessageBuffer();
		this.data = data;
	}

	@Override
	public int minPacketLength() {
		return header.getSize() + header.eieioType.payloadBytes;
	}

	/** The maximum number of elements that can fit in the packet. */
	public int getMaxNumElements() {
		return floorDiv(UDP_MESSAGE_MAX_SIZE - header.getSize(),
				header.eieioType.keyBytes + header.eieioType.payloadBytes);
	}

	/**
	 * The number of elements in the packet
	 */
	public int getNumElements() {
		return header.getCount();
	}

	/** The size of the packet with the current contents. */
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
		final ByteBuffer d = data == null ? null : data.duplicate();
		return new Iterator<AbstractDataElement>() {
			private int elementsRead = 0;

			@Override
			public final boolean hasNext() {
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
}
