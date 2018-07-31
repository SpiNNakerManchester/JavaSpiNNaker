package uk.ac.manchester.spinnaker.messages.eieio;

import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/** An EIEIO message's basic operations. */
public interface EIEIOMessage extends SerializableMessage {
	/** @return the minimum length of a message instance in bytes. */
	int minPacketLength();
}
