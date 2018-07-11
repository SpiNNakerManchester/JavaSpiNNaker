package uk.ac.manchester.spinnaker.connections;

import static java.lang.Thread.sleep;
import static java.nio.ByteBuffer.allocate;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_BOOT_CONNECTION_DEFAULT_PORT;

import java.io.IOException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.model.SpinnakerBootReceiver;
import uk.ac.manchester.spinnaker.connections.model.SpinnakerBootSender;
import uk.ac.manchester.spinnaker.messages.boot.SpinnakerBootMessage;

/** A connection to the SpiNNaker board that uses UDP to for booting */
public class BootConnection extends UDPConnection
		implements SpinnakerBootSender, SpinnakerBootReceiver {
	private static final int ANTI_FLOOD_DELAY = 100;

	/**
	 *
	 * @param localHost
	 *            The local host name or IP address to bind to. If <tt>null</tt>
	 *            defaults to bind to all interfaces, unless remoteHost is
	 *            specified, in which case binding is done to the IP address
	 *            that will be used to send packets.
	 * @param localPort
	 *            The local port to bind to, between 1025 and 65535. If
	 *            <tt>null</tt>, defaults to a random unused local port
	 * @param remoteHost
	 *            The remote host name or IP address to send packets to. If
	 *            <tt>null</tt>, the socket will be available for listening
	 *            only, and will throw and exception if used for sending
	 * @param remotePort
	 *            The remote port to send packets to. If <tt>null</tt>, a
	 *            default value is used.
	 * @throws IOException
	 *             If there is an error setting up the communication channel
	 */
	public BootConnection(String localHost, Integer localPort,
			String remoteHost, Integer remotePort) throws IOException {
		super(localHost, localPort, remoteHost,
				remotePort == null ? UDP_BOOT_CONNECTION_DEFAULT_PORT
						: remotePort);
	}

	@Override
	public SpinnakerBootMessage receiveBootMessage(Integer timeout)
			throws IOException {
		return new SpinnakerBootMessage(receive(timeout));
	}

	@Override
	public void sendBootMessage(SpinnakerBootMessage bootMessage)
			throws IOException {
		ByteBuffer b = allocate(1500);
		bootMessage.addToBuffer(b);
		send(b);
		// Sleep between messages to avoid flooding the machine
		try {
			sleep(ANTI_FLOOD_DELAY);
		} catch (InterruptedException e) {
			throw new IOException("interrupted during anti-flood delay", e);
		}
	}
}
