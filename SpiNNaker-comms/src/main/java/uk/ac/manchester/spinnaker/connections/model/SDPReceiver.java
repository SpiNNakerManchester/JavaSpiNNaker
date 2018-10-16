package uk.ac.manchester.spinnaker.connections.model;

import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

/** A receiver of SDP messages. */
public interface SDPReceiver extends Connection, MessageReceiver<SDPMessage> {
}
