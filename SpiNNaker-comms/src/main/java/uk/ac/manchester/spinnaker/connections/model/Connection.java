package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;

/**
 * An abstract connection to the SpiNNaker board over some medium.
 */
public interface Connection extends SocketHolder {
	/**
	 * Determines if the medium is connected at this point in time
	 *
	 * @return true if the medium is connected, false otherwise
	 * @throws IOException
	 *             If there is an error when determining the connectivity of
	 *             the medium.
	 */
	boolean isConnected() throws IOException;
}
