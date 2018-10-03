package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;

/**
 * An abstract connection to the SpiNNaker board over some medium.
 */
public interface Connection extends SocketHolder {
	/**
	 * Determines if the medium is connected at this point in time. Connected
	 * media are not {@linkplain #isClosed() closed}. Disconnected media might
	 * not be open.
	 *
	 * @return true if the medium is connected, false otherwise
	 * @throws IOException
	 *             If there is an error when determining the connectivity of the
	 *             medium.
	 */
	boolean isConnected() throws IOException;

	/**
	 * Determines if the medium is closed at this point in time. Closed media
	 * are not {@linkplain #isConnected() connected}. Open media might not be
	 * connected.
	 *
	 * @return true if the medium is closed, false otherwise
	 */
	boolean isClosed();
}
