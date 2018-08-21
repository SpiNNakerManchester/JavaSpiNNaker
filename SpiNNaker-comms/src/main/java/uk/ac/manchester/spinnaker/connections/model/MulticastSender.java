package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.MulticastMessage;

/** A sender of Multicast messages. */
public interface MulticastSender extends Connection {
	/**
	 * @return a list of chips which identify the chips to which this sender can
	 *         send multicast packets directly.
	 */
	Iterable<? extends HasChipLocation> getInputChips();

	/**
	 * Sends a SpiNNaker multicast message using this connection.
	 *
	 * @param multicastMessage
	 *            The message to be sent
	 * @throws IOException
	 *             If there is an error sending the message
	 */
	void sendMessage(MulticastMessage multicastMessage) throws IOException;
}
