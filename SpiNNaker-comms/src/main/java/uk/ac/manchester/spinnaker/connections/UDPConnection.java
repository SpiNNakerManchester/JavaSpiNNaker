package uk.ac.manchester.spinnaker.connections;

import static java.net.InetAddress.getByAddress;
import static java.net.StandardSocketOptions.SO_RCVBUF;
import static java.net.StandardSocketOptions.SO_SNDBUF;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.channels.SelectionKey.OP_READ;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.IPV4_SIZE;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_NOT_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.RUNNING_COMMAND_SDP_PORT;
import static uk.ac.manchester.spinnaker.utils.Ping.ping;

import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramPacket;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.connections.model.Listenable;
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
	private static final Logger log = getLogger(UDPConnection.class);
	private static final int RECEIVE_BUFFER_SIZE = 1048576;
	private static final int ETHERNET_MTU = 1500;
	private static final int PING_COUNT = 5;
	private static final int PACKET_MAX_SIZE = 300;
	private static final ThreadLocal<Selector> SELECTOR_FACTORY =
			ThreadLocal.withInitial(() -> {
				try {
					return Selector.open();
				} catch (IOException e) {
					log.error("failed to create selector for thread", e);
					return null;
				}
			});

	private boolean canSend;
	private Inet4Address remoteIPAddress;
	private InetSocketAddress remoteAddress;
	private final DatagramChannel channel;
	private boolean receivable;
	private final ThreadLocal<SelectionKey> selectionKeyFactory;

	/**
	 * Main constructor any of which could null.
	 * <p>
	 * No default constructors are provided as it would not be possible to
	 * disambiguate between ones with only a local host/port like
	 * IPAddressConnection and ones with only remote host/port like
	 * BMPConnection
	 *
	 * @param localHost
	 *            The local host to bind to. If not specified, it defaults to
	 *            binding to all interfaces, unless remoteHost is specified, in
	 *            which case binding is done to the IP address that will be used
	 *            to send packets.
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
	public UDPConnection(InetAddress localHost, Integer localPort,
			InetAddress remoteHost, Integer remotePort) throws IOException {
		channel = DatagramChannel.open();
		channel.bind(createLocalAddress(localHost, localPort));
		channel.configureBlocking(false);
		channel.setOption(SO_RCVBUF, RECEIVE_BUFFER_SIZE);
		channel.setOption(SO_SNDBUF, ETHERNET_MTU);
		selectionKeyFactory = ThreadLocal.withInitial(() -> {
			try {
				return channel.register(SELECTOR_FACTORY.get(), OP_READ);
			} catch (IOException e) {
				log.error("failed to create selection key for thread", e);
				return null;
			}
		});
		canSend = false;
		if (remoteHost != null && remotePort != null && remotePort > 0) {
			remoteIPAddress = (Inet4Address) remoteHost;
			remoteAddress = new InetSocketAddress(remoteIPAddress, remotePort);
			channel.connect(remoteAddress);
			canSend = true;
		}
	}

	private SocketAddress createLocalAddress(InetAddress localHost,
			Integer localPort) throws UnknownHostException {
		// Convert null into wildcard
		if (localPort == null) {
			localPort = 0;
		}
		Inet4Address localAddr;
		try {
			if (localHost == null) {
				localAddr = (Inet4Address) getByAddress(new byte[IPV4_SIZE]);
			} else {
				localAddr = (Inet4Address) localHost;
			}
		} catch (ClassCastException e) {
			throw new UnknownHostException("SpiNNaker only talks IPv4");
		}
		InetSocketAddress addr = new InetSocketAddress(localAddr, localPort);
		return addr;
	}

	private InetSocketAddress getLocalAddress() throws IOException {
		return (InetSocketAddress) channel.getLocalAddress();
	}

	private InetSocketAddress getRemoteAddress() throws IOException {
		return (InetSocketAddress) channel.getRemoteAddress();
	}

	/** @return The local IP address to which the connection is bound. */
	@Override
	public InetAddress getLocalIPAddress() {
		try {
			return getLocalAddress().getAddress();
		} catch (IOException e) {
			return null;
		}
	}

	/** @return The local port to which the connection is bound. */
	@Override
	public int getLocalPort() {
		try {
			return getLocalAddress().getPort();
		} catch (IOException e) {
			return -1;
		}
	}

	/**
	 * @return The remote IP address to which the connection is connected, or
	 *         <tt>null</tt> if it is not connected.
	 */
	@Override
	public InetAddress getRemoteIPAddress() {
		try {
			return getRemoteAddress().getAddress();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * @return The remote port to which the connection is connected, or zero if
	 *         it is not connected.
	 */
	@Override
	public int getRemotePort() {
		try {
			return getRemoteAddress().getPort();
		} catch (IOException e) {
			return -1;
		}
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
		if (isClosed()) {
			throw new EOFException();
		}
		if (timeout == null) {
			/*
			 * "Infinity" is nearly 25 days, which is a very long time to wait
			 * for any message from SpiNNaker.
			 */
			timeout = Integer.MAX_VALUE;
		}
		if (!receivable && !isReadyToReceive(timeout)) {
			throw new SocketTimeoutException();
		}
		ByteBuffer buffer = allocate(PACKET_MAX_SIZE);
		SocketAddress addr = channel.receive(buffer);
		receivable = false;
		if (addr == null) {
			throw new SocketTimeoutException("no packet available");
		}
		buffer.flip();
		return buffer.order(LITTLE_ENDIAN);
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
		if (isClosed()) {
			throw new EOFException();
		}
		if (timeout == null) {
			/*
			 * "Infinity" is nearly 25 days, which is a very long time to wait
			 * for any message from SpiNNaker.
			 */
			timeout = Integer.MAX_VALUE;
		}
		if (!receivable && !isReadyToReceive(timeout)) {
			throw new SocketTimeoutException();
		}
		ByteBuffer buffer = ByteBuffer.allocate(PACKET_MAX_SIZE);
		SocketAddress addr = channel.receive(buffer);
		receivable = false;
		if (addr == null) {
			throw new SocketTimeoutException();
		}
		return new DatagramPacket(buffer.array(), 0, buffer.position(), addr);
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
		doSend(wrap(data.getData(), data.getOffset(), data.getLength()));
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
	private void doSend(ByteBuffer data) throws IOException {
		if (!canSend) {
			throw new IOException("Remote host and/or port not set; "
					+ "data cannot be sent with this connection");
		}
		if (isClosed()) {
			throw new EOFException();
		}
		if (log.isDebugEnabled()) {
			log.debug("sending data of length {} to {}", data.remaining(),
					getRemoteAddress());
			byte[] bytes = new byte[data.remaining()];
			data.duplicate().get(bytes);
			log.debug("message data: {}",
					IntStream.range(0, bytes.length)
							.map(i -> Byte.toUnsignedInt(bytes[i]))
							.mapToObj(Integer::toHexString)
							.collect(Collectors.toList()));
		}
		int sent = channel.send(data, remoteAddress);
		log.debug("sent {} bytes", sent);
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
		doSend(wrap(data));
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
		doSend(data);
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
		sendTo(wrap(data.getData(), data.getOffset(), data.getLength()),
				address, port);
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
		sendTo(wrap(data, 0, data.length), address, port);
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
		if (!canSend) {
			throw new IOException("Remote host address or port not set; "
					+ "data cannot be sent with this connection");
		}
		if (isClosed()) {
			throw new EOFException();
		}
		channel.send(data, new InetSocketAddress(address, port));
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
			channel.disconnect();
		} catch (Exception e) {
			// Ignore any possible exception here
		}
		channel.close();
	}

	@Override
	public boolean isClosed() {
		return !channel.isOpen();
	}

	@Override
	public boolean isReadyToReceive(Integer timeout) throws IOException {
		if (isClosed()) {
			return false;
		}
		SelectionKey key = selectionKeyFactory.get();
		if (!key.isValid()) {
			// Key is stale; try to remake it
			selectionKeyFactory.remove();
			key = selectionKeyFactory.get();
			if (!key.isValid()) {
				throw new IllegalStateException(
						"newly manufactured selection key is invalid");
			}
		}
		log.debug("timout on UDP({} <--> {}) will happen at {} ({})",
				getLocalAddress(), getRemoteAddress(), timeout,
				key.interestOps());
		int result;
		if (timeout == null || timeout == 0) {
			result = key.selector().selectNow();
		} else {
			result = key.selector().select(timeout);
		}
		log.debug("wait:{}:{}:{}", result, key.isValid(),
				key.isValid() && key.isReadable());
		boolean r = key.isValid() && key.isReadable();
		receivable = r;
		return r;
	}

	/**
	 * Sends a port trigger message using a connection to (hopefully) open a
	 * port in a NAT and/or firewall to allow incoming packets to be received.
	 *
	 * @param host
	 *            The address of the SpiNNaker board to which the message should
	 *            be sent
	 * @throws IOException
	 *             If anything goes wrong
	 */
	public void sendPortTriggerMessage(InetAddress host) throws IOException {
		/*
		 * Set up the message so that no reply is expected and it is sent to an
		 * invalid port for SCAMP. The current version of SCAMP will reject this
		 * message, but then fail to send a response since the
		 * REPLY_NOT_EXPECTED flag is set (see scamp-3.c line 728 and 625-644)
		 */
		SDPMessage triggerMessage =
				new SDPMessage(new SDPHeader(REPLY_NOT_EXPECTED,
						new CoreLocation(0, 0, 0), RUNNING_COMMAND_SDP_PORT));
		sendTo(triggerMessage.getMessageData(null), host, SCP_SCAMP_PORT);
	}

	@Override
	public String toString() {
		InetSocketAddress la = null, ra = null;
		try {
			la = this.getLocalAddress();
		} catch (IOException e) {
		}
		try {
			ra = this.getRemoteAddress();
		} catch (IOException e) {
		}
		return String.format("%s(%s <--> %s)",
				getClass().getSimpleName().replaceAll("^.*\\.", ""), la, ra);
	}
}
