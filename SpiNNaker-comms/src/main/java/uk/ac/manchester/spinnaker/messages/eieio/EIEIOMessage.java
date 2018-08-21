package uk.ac.manchester.spinnaker.messages.eieio;

import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/**
 * An EIEIO message's basic operations.
 *
 * @param <Header>
 *            The type of header on this message.
 */
public interface EIEIOMessage<Header extends EIEIOHeader>
		extends SerializableMessage {
	/** @return the header of this message. */
	Header getHeader();

	/** @return the minimum length of a message instance in bytes. */
	int minPacketLength();
}
