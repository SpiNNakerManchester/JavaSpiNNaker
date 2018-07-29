package uk.ac.manchester.spinnaker.connections;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessageFactory.readCommandMessage;
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessageFactory.readDataMessage;
import static uk.ac.manchester.spinnaker.transceiver.Utils.newMessageBuffer;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.model.EIEIOReceiver;
import uk.ac.manchester.spinnaker.connections.model.EIEIOSender;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessage;

/** A UDP connection for sending and receiving raw EIEIO messages. */
public class EIEIOConnection extends UDPConnection<EIEIOMessage>
		implements EIEIOReceiver, EIEIOSender {
	public EIEIOConnection(String localHost, Integer localPort,
			String remoteHost, Integer remotePort) throws IOException {
		super(localHost, localPort, remoteHost, remotePort);
	}

	@Override
	public void sendEIEIOMessage(EIEIOMessage eieioMessage) throws IOException {
		ByteBuffer b = newMessageBuffer();
		eieioMessage.addToBuffer(b);
		b.flip();
		send(b);
	}

	public void sendEIEIOMessageTo(EIEIOMessage eieioMessage,
			InetAddress ipAddress, int port) throws IOException {
		ByteBuffer b = newMessageBuffer();
		eieioMessage.addToBuffer(b);
		b.flip();
		sendTo(b, ipAddress, port);
	}

	@Override
	public EIEIOMessage receiveEIEIOMessage(Integer timeout)
			throws IOException {
		ByteBuffer b = receive();
		short header = b.getShort();
		if ((header & 0xC000) == 0x4000) {
			return readCommandMessage(b, 0);
		} else {
			return readDataMessage(b, 0);
		}
	}

	@Override
	public MessageReceiver<EIEIOMessage> getReceiver() {
		return new MessageReceiver<EIEIOMessage>() {
			@Override
			public EIEIOMessage receive() throws IOException {
				return receiveEIEIOMessage(null);
			}
		};
	}
}
