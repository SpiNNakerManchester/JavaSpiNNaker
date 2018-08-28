package uk.ac.manchester.spinnaker.connections;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.model.Listenable;

/**
 * A simple connection that makes incoming messages available as buffers.
 *
 * @author Donal Fellows
 */
public class UDPListenableConnection extends UDPConnection<ByteBuffer>
		implements Listenable.MessageReceiver<ByteBuffer> {
	public UDPListenableConnection(InetAddress localHost, Integer localPort,
			InetAddress remoteHost, Integer remotePort) throws IOException {
		super(localHost, localPort, remoteHost, remotePort);
	}

	@Override
	public MessageReceiver<ByteBuffer> getReceiver() {
		return this;
	}
}
