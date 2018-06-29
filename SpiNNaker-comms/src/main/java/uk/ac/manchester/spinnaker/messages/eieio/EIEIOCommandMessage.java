package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/** An EIEIO command message */
public class EIEIOCommandMessage implements EIEIOMessage {
	/** The header of the message */
	public final EIEIOCommandHeader header;
	/** Optional incoming data */
	public final byte[] data;// FIXME
	/** Offset into the data where valid data begins */
	public final int offset;

	public EIEIOCommandMessage(EIEIOCommandHeader header, byte[] data,
			int offset) {
		this.header = header;
		this.data = data;
		this.offset = offset;
	}

	public EIEIOCommandMessage(EIEIOCommandHeader header) {
		this(header, null, 0);
	}

	public void addToBuffer(ByteBuffer buffer) {
		header.addToBuffer(buffer);
	}

	public static int minPacketLength() {
		return 2;
	}
}
