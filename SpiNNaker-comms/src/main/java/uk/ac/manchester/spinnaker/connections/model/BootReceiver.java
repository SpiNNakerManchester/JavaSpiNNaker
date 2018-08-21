package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;

import uk.ac.manchester.spinnaker.messages.boot.BootMessage;

/** A receiver of SpiNNaker boot messages. */
public interface BootReceiver extends SocketHolder {
	/**
	 * Receives a boot message from this connection. Blocks until a message has
	 * been received.
	 *
	 * @return the received packet
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws IllegalArgumentException
	 *             If one of the fields of the SpiNNaker boot message is invalid
	 */
	default BootMessage receiveBootMessage() throws IOException {
		return receiveBootMessage(null);
	}

	/**
	 * Receives a boot message from this connection. Blocks until a message has
	 * been received, or a timeout occurs.
	 *
	 * @param timeout
	 *            The time in seconds to wait for the message to arrive; if
	 *            <tt>null</tt>, will wait forever, or until the connection is
	 *            closed.
	 * @return the received packet
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws IllegalArgumentException
	 *             If one of the fields of the SpiNNaker boot message is invalid
	 */
	BootMessage receiveBootMessage(Integer timeout) throws IOException;
}
