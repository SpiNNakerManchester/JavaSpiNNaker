package uk.ac.manchester.spinnaker.connections;

import static java.lang.Thread.sleep;
import static java.nio.ByteBuffer.allocate;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_BOOT_CONNECTION_DEFAULT_PORT;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.model.BootReceiver;
import uk.ac.manchester.spinnaker.connections.model.BootSender;
import uk.ac.manchester.spinnaker.messages.boot.BootMessage;

/** A connection to the SpiNNaker board that uses UDP to for booting. */
public class BootConnection extends UDPConnection<BootMessage>
		implements BootSender, BootReceiver {
	// Determined by Ethernet MTU, not by SDP buffer size
	private static final int BOOT_MESSAGE_SIZE = 1500;
	private static final int ANTI_FLOOD_DELAY = 100;

	/**
	 * @param localHost
	 *            The local host to bind to. If <tt>null</tt>
	 *            defaults to bind to all interfaces, unless remoteHost is
	 *            specified, in which case binding is done to the IP address
	 *            that will be used to send packets.
	 * @param localPort
	 *            The local port to bind to, between 1025 and 65535. If
	 *            <tt>null</tt>, defaults to a random unused local port
	 * @param remoteHost
	 *            The remote host to send packets to. If
	 *            <tt>null</tt>, the socket will be available for listening
	 *            only, and will throw and exception if used for sending
	 * @param remotePort
	 *            The remote port to send packets to. If <tt>null</tt>, a
	 *            default value is used.
	 * @throws IOException
	 *             If there is an error setting up the communication channel
	 */
	public BootConnection(InetAddress localHost, Integer localPort,
			InetAddress remoteHost, Integer remotePort) throws IOException {
		super(localHost, localPort, remoteHost,
				remotePort == null ? UDP_BOOT_CONNECTION_DEFAULT_PORT
						: remotePort);
	}

	@Override
	public BootMessage receiveBootMessage(Integer timeout) throws IOException {
		return new BootMessage(receive(timeout));
	}

	@Override
	public void sendBootMessage(BootMessage bootMessage) throws IOException {
		ByteBuffer b = allocate(BOOT_MESSAGE_SIZE);
		bootMessage.addToBuffer(b);
		b.flip();
		send(b);
		// Sleep between messages to avoid flooding the machine
		try {
			sleep(ANTI_FLOOD_DELAY);
		} catch (InterruptedException e) {
			throw new IOException("interrupted during anti-flood delay", e);
		}
	}

	@Override
	public MessageReceiver<BootMessage> getReceiver() {
		return this::receiveBootMessage;
	}
}
