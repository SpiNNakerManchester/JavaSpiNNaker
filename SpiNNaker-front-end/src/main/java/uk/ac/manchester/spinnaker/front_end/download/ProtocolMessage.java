package uk.ac.manchester.spinnaker.front_end.download;

import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_NOT_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

/**
 * A message participating in the fast-data-download protocol. This protocol
 * looks like SDP in general.
 *
 * @author Donal Fellows
 */
public abstract class ProtocolMessage extends SDPMessage {
	/**
	 * Create a protocol message.
	 *
	 * @param destination
	 *            Where to send the message
	 * @param destPort
	 *            Which port to send the message to
	 * @param payload
	 *            What the contents of the message should be.
	 */
	protected ProtocolMessage(HasCoreLocation destination, int destPort,
			ByteBuffer payload) {
		super(new SDPHeader(REPLY_NOT_EXPECTED, destination, destPort),
				payload);
	}
}
