package uk.ac.manchester.spinnaker.connections.model;

import uk.ac.manchester.spinnaker.messages.boot.BootMessage;

// NB: This "concrete interface" is needed for the Transceiver.
/** A receiver of SpiNNaker boot messages. */
public interface BootReceiver
		extends SocketHolder, MessageReceiver<BootMessage> {
}
