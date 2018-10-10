package uk.ac.manchester.spinnaker.connections;

import static uk.ac.manchester.spinnaker.messages.Constants.UDP_BOOT_CONNECTION_DEFAULT_PORT;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import uk.ac.manchester.spinnaker.connections.model.MessageReceiver;

/**
 * A connection that detects any UDP packet that is transmitted by SpiNNaker
 * boards prior to boot.
 */
public class IPAddressConnection extends UDPConnection<InetAddress>
		implements MessageReceiver<InetAddress> {
	/** Matches SPINN_PORT in spinnaker_bootROM. */
	private static final int BOOTROM_SPINN_PORT = 54321;

	public IPAddressConnection() throws IOException {
		this(null, UDP_BOOT_CONNECTION_DEFAULT_PORT);
	}

	public IPAddressConnection(InetAddress localHost) throws IOException {
		this(localHost, UDP_BOOT_CONNECTION_DEFAULT_PORT);
	}

	public IPAddressConnection(InetAddress localHost, int localPort)
			throws IOException {
		super(localHost, localPort, null, null);
	}

	/**
	 * @return The IP address, or {@code null} if none was forthcoming.
	 */
	@Override
	public final InetAddress receiveMessage() {
		return receiveMessage(null);
	}

	/**
	 * @param timeout
	 *            How long to wait for an IP address; {@code null} for forever.
	 * @return The IP address, or {@code null} if none was forthcoming.
	 */
	@Override
	public InetAddress receiveMessage(Integer timeout) {
		try {
			DatagramPacket packet = receiveWithAddress(timeout);
			if (packet.getPort() == BOOTROM_SPINN_PORT) {
				return packet.getAddress();
			}
		} catch (IOException e) {
			// Do nothing
		}
		return null;
	}
}
