package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/** An EIEIO command message. */
public class EIEIOCommandMessage implements EIEIOMessage {
	/** The header of the message. */
	public final EIEIOCommandHeader header;

	public EIEIOCommandMessage(EIEIOCommandHeader header, ByteBuffer data,
			int offset) {
		this.header = header;
	}

	public EIEIOCommandMessage(EIEIOCommandHeader header) {
		this(header, null, 0);
	}

	public EIEIOCommandMessage(EIEIOCommandID command) {
		this(new EIEIOCommandHeader(command), null, 0);
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		header.addToBuffer(buffer);
	}

	public int minPacketLength() {
		return 2;
	}
}
