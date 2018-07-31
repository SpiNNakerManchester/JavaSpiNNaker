package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

/** An EIEIO command message. */
public class EIEIOCommandMessage implements EIEIOMessage<EIEIOCommandHeader> {
	/** The header of the message. */
	private final EIEIOCommandHeader header;

	public EIEIOCommandMessage(EIEIOCommandHeader header) {
		this.header = header;
	}

	public EIEIOCommandMessage(EIEIOCommand command) {
		this.header = new EIEIOCommandHeader(command);
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		header.addToBuffer(buffer);
	}

	@Override
	public int minPacketLength() {
		return 2;
	}

	@Override
	public EIEIOCommandHeader getHeader() {
		return header;
	}
}
