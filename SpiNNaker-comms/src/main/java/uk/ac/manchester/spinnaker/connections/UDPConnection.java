package uk.ac.manchester.spinnaker.connections;

import static java.net.InetAddress.getByName;
import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.channels.SelectionKey.OP_READ;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_NOT_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.RUNNING_COMMAND_SDP_PORT;
import static uk.ac.manchester.spinnaker.transceiver.Utils.newMessageBuffer;
import static uk.ac.manchester.spinnaker.utils.Ping.ping;

import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.connections.model.Listenable;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

/**
 * A connection to SpiNNaker over UDP/IPv4.
 *
 * @param <T>
 *            The Java type of message received on this connection.
 */
public abstract class UDPConnection<T> implements Connection, Listenable<T> {
	private boolean canSend;
	private Inet4Address remoteIPAddress;
	private int remotePort;
	private final DatagramSocket socket;
	private final DatagramChannel channel;
	private static final int RECEIVE_BUFFER_SIZE = 1048576;
	private static final int ONE_SECOND = 1000;
	private static final int PING_COUNT = 5;
	private static final int PACKET_MAX_SIZE = 300;

	/**
	 * @param localHost
	 *            The local host name or IP address to bind to. If not
	 *            specified, it defaults to binding to all interfaces, unless
	 *            remoteHost is specified, in which case binding is done to the
	 *            IP address that will be used to send packets.
	 * @param localPort
	 *            The local port to bind to, 0 or between 1025 and 65535.
	 * @param remoteHost
	 *            The remote host name or IP address to send packets to. If not
	 *            specified, the socket will be available for listening only,
	 *            and will throw and exception if used for sending.
	 * @param remotePort
	 *            The remote port to send packets to. If remoteHost is
	 *            <tt>null</tt>, this is ignored. If remoteHost is specified,
	 *            this must also be specified as non-zero for the connection to
	 *            allow sending.
	 * @throws IOException
	 *             If there is an error setting up the communication channel
	 */
	public UDPConnection(String localHost, Integer localPort, String remoteHost,
			Integer remotePort) throws IOException {
		channel = DatagramChannel.open();
		channel.bind(createLocalAddress(localHost, localPort));
		channel.configureBlocking(false);
		socket = channel.socket();
		socket.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
		canSend = false;
		if (remoteHost != null && remotePort != null && remotePort != 0) {
			remoteIPAddress = (Inet4Address) getByName(remoteHost);
			this.remotePort = remotePort;
			channel.connect(
					new InetSocketAddress(remoteIPAddress, this.remotePort));
			canSend = true;
		}
		socket.setSoTimeout(ONE_SECOND);
	}

	private static SocketAddress createLocalAddress(String localHost,
			Integer localPort) throws UnknownHostException {
		// Convert null into wildcard
		if (localPort == null) {
			localPort = 0;
		}
		if (localHost == null) {
			return new InetSocketAddress(localPort);
		}
		return new InetSocketAddress(getByName(localHost), localPort);
	}

	/** @return The local IP address to which the connection is bound. */
	@Override
	public InetAddress getLocalIPAddress() {
		return socket.getLocalAddress();
	}

	/** @return The local port to which the connection is bound. */
	@Override
	public int getLocalPort() {
		return socket.getLocalPort();
	}

	/**
	 * @return The remote IP address to which the connection is connected, or
	 *         <tt>null</tt> if it is not connected.
	 */
	@Override
	public InetAddress getRemoteIPAddress() {
		return remoteIPAddress;
	}

	/**
	 * @return The remote port to which the connection is connected, or zero if
	 *         it is not connected.
	 */
	@Override
	public int getRemotePort() {
		return remotePort;
	}

	/**
	 * Receive data from the connection.
	 *
	 * @return The data received, in a little-endian buffer
	 * @throws SocketTimeoutException
	 *             If a timeout occurs before any data is received
	 * @throws IOException
	 *             If an error occurs receiving the data
	 */
	public ByteBuffer receive() throws SocketTimeoutException, IOException {
		return receive(null);
	}

	/**
	 * Receive data from the connection.
	 *
	 * @param timeout
	 *            The timeout in milliseconds, or null to wait forever
	 * @return The data received, in a little-endian buffer
	 * @throws SocketTimeoutException
	 *             If a timeout occurs before any data is received
	 * @throws IOException
	 *             If an error occurs receiving the data
	 */
	public ByteBuffer receive(Integer timeout)
			throws SocketTimeoutException, IOException {
		if (!channel.isOpen()) {
			throw new EOFException();
		}
		if (timeout == null) {
			socket.setSoTimeout(0);
		} else {
			socket.setSoTimeout(timeout);
		}
		DatagramPacket p =
				new DatagramPacket(new byte[PACKET_MAX_SIZE], PACKET_MAX_SIZE);
		socket.receive(p);
		ByteBuffer buffer = wrap(p.getData(), 0, p.getLength());
		buffer.order(LITTLE_ENDIAN);
		return buffer;
	}

	/**
	 * Receive data from the connection along with the address where the data
	 * was received from.
	 *
	 * @return The datagram packet received
	 * @throws SocketTimeoutException
	 *             If a timeout occurs before any data is received
	 * @throws IOException
	 *             If an error occurs receiving the data
	 */
	public DatagramPacket receiveWithAddress()
			throws SocketTimeoutException, IOException {
		return receiveWithAddress(null);
	}

	/**
	 * Receive data from the connection along with the address where the data
	 * was received from.
	 *
	 * @param timeout
	 *            The timeout in milliseconds, or null to wait forever
	 * @return The datagram packet received
	 * @throws SocketTimeoutException
	 *             If a timeout occurs before any data is received
	 * @throws IOException
	 *             If an error occurs receiving the data
	 */
	public DatagramPacket receiveWithAddress(Integer timeout)
			throws SocketTimeoutException, IOException {
		if (!channel.isOpen()) {
			throw new EOFException();
		}
		if (timeout == null) {
			socket.setSoTimeout(0);
		} else {
			socket.setSoTimeout(timeout);
		}
		DatagramPacket p =
				new DatagramPacket(new byte[PACKET_MAX_SIZE], PACKET_MAX_SIZE);
		socket.receive(p);
		return p;
	}

	/**
	 * Send data down this connection.
	 *
	 * @param data
	 *            The data to be sent
	 * @throws IOException
	 *             If there is an error sending the data
	 */
	public void send(DatagramPacket data) throws IOException {
		// Clear the destination; use socket's default
		doSend(new DatagramPacket(data.getData(), data.getOffset(),
				data.getLength()));
	}

	/**
	 * Send data down this connection. Caller must ensure that the datagram
	 * packet does <i>not</i> have a destination set.
	 *
	 * @param data
	 *            The data to be sent
	 * @throws IOException
	 *             If there is an error sending the data
	 */
	private void doSend(DatagramPacket data) throws IOException {
		if (!canSend) {
			throw new IOException("Remote host and/or port not set; "
					+ "data cannot be sent with this connection");
		}
		if (!channel.isOpen()) {
			throw new EOFException();
		}
		socket.send(data);
	}

	/**
	 * Send data down this connection.
	 *
	 * @param data
	 *            The data to be sent
	 * @throws IOException
	 *             If there is an error sending the data
	 */
	public void send(byte[] data) throws IOException {
		doSend(new DatagramPacket(data, 0, data.length));
	}

	/**
	 * Send data down this connection.
	 *
	 * @param data
	 *            The data to be sent
	 * @throws IOException
	 *             If there is an error sending the data
	 */
	public void send(ByteBuffer data) throws IOException {
		doSend(new DatagramPacket(data.array(), data.position(),
				data.remaining()));
	}

	/**
	 * Send data down this connection.
	 *
	 * @param data
	 *            The data to be sent
	 * @param address
	 *            Where to send (must be non-null)
	 * @param port
	 *            What port to send to (must be non-zero)
	 * @throws IOException
	 *             If there is an error sending the data
	 */
	public void sendTo(DatagramPacket data, InetAddress address, int port)
			throws IOException {
		if (!canSend) {
			throw new IOException("Remote host address or port not set; "
					+ "data cannot be sent with this connection");
		}
		if (!channel.isOpen()) {
			throw new EOFException();
		}
		// Clear the destination; use socket's default
		data.setAddress(address);
		data.setPort(port);
		socket.send(data);
	}

	/**
	 * Send data down this connection.
	 *
	 * @param data
	 *            The data to be sent
	 * @param address
	 *            Where to send (must be non-null)
	 * @param port
	 *            What port to send to (must be non-zero)
	 * @throws IOException
	 *             If there is an error sending the data
	 */
	public void sendTo(byte[] data, InetAddress address, int port)
			throws IOException {
		sendTo(new DatagramPacket(data, 0, data.length), address, port);
	}

	/**
	 * Send data down this connection.
	 *
	 * @param data
	 *            The data to be sent
	 * @param address
	 *            Where to send (must be non-null)
	 * @param port
	 *            What port to send to (must be non-zero)
	 * @throws IOException
	 *             If there is an error sending the data
	 */
	public void sendTo(ByteBuffer data, InetAddress address, int port)
			throws IOException {
		sendTo(new DatagramPacket(data.array(), data.position(),
				data.remaining()), address, port);
	}

	@Override
	public boolean isConnected() throws IOException {
		if (!canSend) {
			return false;
		}
		for (int i = 0; i < PING_COUNT; i++) {
			if (ping(remoteIPAddress) == 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void close() throws Exception {
		try {
			socket.disconnect();
		} catch (Exception e) {
			// Ignore any possible exception here
		}
		socket.close();
	}

	@Override
	public boolean isReadyToReceive(Integer timeout) throws IOException {
		if (!channel.isOpen()) {
			return false;
		}
		try (Selector selector = Selector.open()) {
			SelectionKey key = channel.register(selector, OP_READ);
			try {
				if (timeout == null || timeout == 0) {
					selector.selectNow();
				} else {
					selector.select(timeout);
				}
				return key.isReadable();
			} finally {
				key.cancel();
			}
		}
	}

	private static final ChipLocation ONE_WAY_SOURCE = new ChipLocation(0, 0);

	/**
	 * Sends a port trigger message using a connection to (hopefully) open a
	 * port in a NAT and/or firewall to allow incoming packets to be received.
	 *
	 * @param hostname
	 *            The address of the SpiNNaker board to which the message should
	 *            be sent
	 * @throws IOException
	 *             If anything goes wrong
	 */
	public void sendPortTriggerMessage(String hostname) throws IOException {
		/*
		 * Set up the message so that no reply is expected and it is sent to an
		 * invalid port for SCAMP. The current version of SCAMP will reject this
		 * message, but then fail to send a response since the
		 * REPLY_NOT_EXPECTED flag is set (see scamp-3.c line 728 and 625-644)
		 */
		SDPMessage triggerMessage =
				new SDPMessage(new SDPHeader(REPLY_NOT_EXPECTED,
						new CoreLocation(0, 0, 0), RUNNING_COMMAND_SDP_PORT));
		triggerMessage.updateSDPHeaderForUDPSend(ONE_WAY_SOURCE);
		ByteBuffer b = newMessageBuffer();
		triggerMessage.addToBuffer(b);
		InetAddress addr = getByName(hostname);
		sendTo(b, addr, SCP_SCAMP_PORT);
	}
}
