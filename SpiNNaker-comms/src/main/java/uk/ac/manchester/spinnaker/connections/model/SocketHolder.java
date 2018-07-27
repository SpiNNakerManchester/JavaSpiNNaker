package uk.ac.manchester.spinnaker.connections.model;

import java.net.InetAddress;

/**
 * Indicates a resource class that holds a network socket and that can answer
 * basic questions about it.
 *
 * @author Donal Fellows
 */
public interface SocketHolder extends AutoCloseable {
	/**
	 * @return the local (host) IP address of the socket. Expected to be an IPv4
	 *         address when talking to SpiNNaker.
	 */
	InetAddress getLocalIPAddress();

	/**
	 * @return the local (host) port of the socket.
	 */
	int getLocalPort();

	/**
	 * @return the remote (board) IP address of the socket. Expected to be an
	 *         IPv4 address when talking to SpiNNaker.
	 */
	InetAddress getRemoteIPAddress();

	/**
	 * @return the remote (board) port of the socket.
	 */
	int getRemotePort();
}
