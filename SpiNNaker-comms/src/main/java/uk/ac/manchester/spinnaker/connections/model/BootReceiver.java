package uk.ac.manchester.spinnaker.connections.model;

import uk.ac.manchester.spinnaker.messages.boot.BootMessage;

/** A receiver of SpiNNaker boot messages. */
/*
 * NB: This "concrete interface" is needed for the Transceiver.
 */
public interface BootReceiver
		extends SocketHolder, MessageReceiver<BootMessage> {
}
