package uk.ac.manchester.spinnaker.connections;

import static uk.ac.manchester.spinnaker.messages.Constants.UDP_BOOT_CONNECTION_DEFAULT_PORT;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * A connection that detects any UDP packet that is transmitted by SpiNNaker
 * boards prior to boot.
 */
public class IPAddressConnection extends UDPConnection<InetAddress> {
	/** Matches SPINN_PORT in spinnaker_bootROM */
	private static final int BOOTROM_SPINN_PORT = 54321;

	public IPAddressConnection() throws IOException {
		this(null, UDP_BOOT_CONNECTION_DEFAULT_PORT);
	}

	public IPAddressConnection(String localHost) throws IOException {
		this(localHost, UDP_BOOT_CONNECTION_DEFAULT_PORT);
	}

	public IPAddressConnection(String localHost, int localPort)
			throws IOException {
		super(localHost, localPort, null, null);
	}

	public final InetAddress receiveIPAddress() {
		return receiveIPAddress(null);
	}

	public InetAddress receiveIPAddress(Integer timeout) {
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

	@Override
	public MessageReceiver<InetAddress> getReceiver() {
		return this::receiveIPAddress;
	}
}
