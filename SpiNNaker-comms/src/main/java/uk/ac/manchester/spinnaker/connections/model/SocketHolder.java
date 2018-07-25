package uk.ac.manchester.spinnaker.connections.model;

import java.net.InetAddress;

/**
 * Indicates a class that holds a network socket and that can answer basic
 * questions about it.
 *
 * @author Donal Fellows
 */
public interface SocketHolder extends AutoCloseable {
	InetAddress getLocalIPAddress();
	int getLocalPort();
	InetAddress getRemoteIPAddress();
	int getRemotePort();
}
