package uk.ac.manchester.spinnaker.connections;

import java.io.IOException;
import java.nio.ByteBuffer;

public class UDPListenableConnection extends UDPConnection implements
		Listenable<ByteBuffer>, Listenable.MessageReceiver<ByteBuffer> {
	public UDPListenableConnection(String localHost, Integer localPort,
			String remoteHost, Integer remotePort) throws IOException {
		super(localHost, localPort, remoteHost, remotePort);
	}

	@Override
	public MessageReceiver<ByteBuffer> getReceiver() {
		return this;
	}
}
