package uk.ac.manchester.spinnaker.connections;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessageFactory.read_eieio_command_message;
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessageFactory.read_eieio_data_message;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessage;

/** A UDP connection for sending and receiving raw EIEIO messages. */
public class EIEIOConnection extends UDPConnection
		implements EIEIOReceiver, EIEIOSender, Listenable<EIEIOMessage> {
	public EIEIOConnection(String localHost, Integer localPort,
			String remoteHost, Integer remotePort) throws IOException {
		super(localHost, localPort, remoteHost, remotePort);
	}

	@Override
	public void sendEIEIOMessage(EIEIOMessage eieioMessage) throws IOException {
		ByteBuffer b = ByteBuffer.allocate(300);
		b.order(LITTLE_ENDIAN);
		b.position(0);
		eieioMessage.addToBuffer(b);
		send(b);
	}

	public void sendEIEIOMessageTo(EIEIOMessage eieioMessage,
			InetAddress ipAddress, int port) throws IOException {
		ByteBuffer b = ByteBuffer.allocate(300);
		b.order(LITTLE_ENDIAN);
		b.position(0);
		eieioMessage.addToBuffer(b);
		sendTo(b, ipAddress, port);
	}

	@Override
	public EIEIOMessage receiveEIEIOMessage(Integer timeout)
			throws IOException {
		ByteBuffer b = receive();
		short header = b.getShort();
		if ((header & 0xC000) == 0x4000) {
			return read_eieio_command_message(b, 0);
		} else {
			return read_eieio_data_message(b, 0);
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
