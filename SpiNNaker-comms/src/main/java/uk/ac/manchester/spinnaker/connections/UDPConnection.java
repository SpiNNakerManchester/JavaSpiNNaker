/*
 * Copyright (c) 2018-2020 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.connections;

import static java.net.InetAddress.getByAddress;
import static java.net.StandardProtocolFamily.INET;
import static java.net.StandardSocketOptions.SO_RCVBUF;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.IPV4_SIZE;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_NOT_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.RUNNING_COMMAND_SDP_PORT;
import static uk.ac.manchester.spinnaker.utils.MathUtils.hexbyte;
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
import java.util.List;
import java.util.function.IntFunction;

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
	 * Main constructor, any argument of which could {@code null}.
	 * <p>
	 * No default constructors are provided as it would not be possible to
	 * disambiguate between ones with only a local host/port like
	 * {@link IPAddressConnection} and ones with only remote host/port like
	 * {@link BMPConnection}.
	 *
	 * @param localHost
	 *            The local host to bind to. If {@code null}, it defaults to
	 *            binding to all interfaces, unless {@code remoteHost} is
	 *            specified, in which case binding is done to the IP address
	 *            that will be used to send packets.
	 * @param localPort
	 *            The local port to bind to, 0 (or {@code null}) or between 1025
	 *            and 65535.
	 * @param remoteHost
	 *            The remote host name or IP address to send packets to. If
	 *            {@code null}, the socket will be available for listening only,
	 *            and will throw and exception if used for sending.
	 * @param remotePort
	 *            The remote port to send packets to. If remoteHost is
	 *            {@code null}, this is ignored. If remoteHost is specified,
	 *            this must also be specified as non-zero for the connection to
	 *            allow sending.
	 * @throws IOException
	 *             If there is an error setting up the communication channel
	 */
	public UDPConnection(InetAddress localHost, Integer localPort,
			InetAddress remoteHost, Integer remotePort) throws IOException {
		canSend = (remoteHost != null && remotePort != null && remotePort > 0);
		channel =
				initialiseSocket(localHost, localPort, remoteHost, remotePort);
		selectionKeyFactory = ThreadLocal.withInitial(() -> {
			try {
				return channel.register(SELECTOR_FACTORY.get(), OP_READ);
			} catch (IOException e) {
				log.error("failed to create selection key for thread", e);
				return null;
			}
		});
		if (channel != null && log.isDebugEnabled()) {
			InetSocketAddress us = null;
			try {
				us = getLocalAddress();
			} catch (Exception ignore) {
			}
			if (us == null) {
				us = new InetSocketAddress((InetAddress) null, 0);
			}
			InetSocketAddress them = null;
			try {
				them = getRemoteAddress();
			} catch (Exception ignore) {
			}
			if (them == null) {
				them = new InetSocketAddress((InetAddress) null, 0);
			}
			log.debug("{} socket created ({} <--> {})", getClass().getName(),
					us, them);
		}
	}

	/**
	 * Set up a UDP/IPv4 socket.
	 *
	 * @param localHost
	 *            Local side address.
	 * @param localPort
	 *            Local side port.
	 * @param remoteHost
	 *            Remote side address.
	 * @param remotePort
	 *            Remote side port.
	 * @return The configured, connected socket.
	 * @throws IOException
	 *             If anything fails.
	 */
	DatagramChannel initialiseSocket(InetAddress localHost, Integer localPort,
			InetAddress remoteHost, Integer remotePort) throws IOException {
		// SpiNNaker only speaks IPv4
		DatagramChannel chan = DatagramChannel.open(INET);
		chan.bind(createLocalAddress(localHost, localPort));
		chan.configureBlocking(false);
		chan.setOption(SO_RCVBUF, RECEIVE_BUFFER_SIZE);
		if (canSend) {
			remoteIPAddress = (Inet4Address) remoteHost;
			remoteAddress = new InetSocketAddress(remoteIPAddress, remotePort);
			chan.connect(remoteAddress);
		}
		return chan;
	}

	private static SocketAddress createLocalAddress(InetAddress localHost,
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
		return new InetSocketAddress(localAddr, localPort);
	}

	InetSocketAddress getLocalAddress() throws IOException {
		return (InetSocketAddress) channel.getLocalAddress();
	}

	InetSocketAddress getRemoteAddress() throws IOException {
		return (InetSocketAddress) channel.getRemoteAddress();
	}

	/** @return The local IP address to which the connection is bound. */
	@Override
	public final InetAddress getLocalIPAddress() {
		try {
			return getLocalAddress().getAddress();
		} catch (NullPointerException | IOException e) {
			return null;
		}
	}

	/** @return The local port to which the connection is bound. */
	@Override
	public final int getLocalPort() {
		try {
			return getLocalAddress().getPort();
		} catch (NullPointerException | IOException e) {
			return -1;
		}
	}

	/**
	 * @return The remote IP address to which the connection is connected, or
	 *         {@code null} if it is not connected.
	 */
	@Override
	public final InetAddress getRemoteIPAddress() {
		try {
			return getRemoteAddress().getAddress();
		} catch (NullPointerException | IOException e) {
			return null;
		}
	}

	/**
	 * @return The remote port to which the connection is connected, or zero if
	 *         it is not connected.
	 */
	@Override
	public final int getRemotePort() {
		try {
			return getRemoteAddress().getPort();
		} catch (NullPointerException | IOException e) {
			return -1;
		}
	}

	/**
	 * Receive data from the connection.
	 *
	 * @param timeout
	 *            The timeout in milliseconds, or {@code null} to wait forever
	 * @return The data received, in a little-endian buffer
	 * @throws SocketTimeoutException
	 *             If a timeout occurs before any data is received
	 * @throws EOFException
	 *             If the connection is closed
	 * @throws IOException
	 *             If an error occurs receiving the data
	 */
	public final ByteBuffer receive(Integer timeout)
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
		return doReceive(timeout);
	}

	/**
	 * Receive data from the connection.
	 *
	 * @param timeout
	 *            The timeout in milliseconds
	 * @return The data received, in a little-endian buffer
	 * @throws SocketTimeoutException
	 *             If a timeout occurs before any data is received
	 * @throws IOException
	 *             If an error occurs receiving the data
	 */
	ByteBuffer doReceive(Integer timeout)
			throws SocketTimeoutException, IOException {
		if (!receivable && !isReadyToReceive(timeout)) {
			log.debug("not ready to recieve");
			throw new SocketTimeoutException();
		}
		ByteBuffer buffer = allocate(PACKET_MAX_SIZE);
		SocketAddress addr = channel.receive(buffer);
		receivable = false;
		if (addr == null) {
			throw new SocketTimeoutException("no packet available");
		}
		buffer.flip();
		logRecv(buffer, addr);
		return buffer.order(LITTLE_ENDIAN);
	}

	/**
	 * Receive data from the connection along with the address where the data
	 * was received from.
	 *
	 * @param timeout
	 *            The timeout in milliseconds, or {@code null} to wait forever
	 * @return The datagram packet received
	 * @throws SocketTimeoutException
	 *             If a timeout occurs before any data is received
	 * @throws EOFException
	 *             If the connection is closed
	 * @throws IOException
	 *             If an error occurs receiving the data
	 */
	public final DatagramPacket receiveWithAddress(Integer timeout)
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
		return doReceiveWithAddress(timeout);
	}

	/**
	 * Receive data from the connection along with the address where the data
	 * was received from.
	 *
	 * @param timeout
	 *            The timeout in milliseconds
	 * @return The datagram packet received
	 * @throws SocketTimeoutException
	 *             If a timeout occurs before any data is received
	 * @throws IOException
	 *             If an error occurs receiving the data
	 */
	DatagramPacket doReceiveWithAddress(int timeout)
			throws SocketTimeoutException, IOException {
		if (!receivable && !isReadyToReceive(timeout)) {
			throw new SocketTimeoutException();
		}
		ByteBuffer buffer = ByteBuffer.allocate(PACKET_MAX_SIZE);
		SocketAddress addr = channel.receive(buffer);
		receivable = false;
		if (addr == null) {
			throw new SocketTimeoutException();
		}
		logRecv(buffer, addr);
		return new DatagramPacket(buffer.array(), 0, buffer.position(), addr);
	}

	/**
	 * Send data down this connection.
	 *
	 * @param data
	 *            The data to be sent
	 * @throws EOFException
	 *             If the connection is closed
	 * @throws IOException
	 *             If there is an error sending the data
	 * @throws IllegalStateException
	 *             If the data packet doesn't hold a real message; zero-length
	 *             messages are not supported!
	 */
	public final void send(DatagramPacket data) throws IOException {
		send(wrap(data.getData(), data.getOffset(), data.getLength()));
	}

	/**
	 * Send data down this connection.
	 *
	 * @param data
	 *            The data to be sent; the position in this buffer will
	 *            <em>not</em> be updated by this method
	 * @throws IOException
	 *             If there is an error sending the data
	 */
	void doSend(ByteBuffer data) throws IOException {
		if (log.isDebugEnabled()) {
			logSend(data, getRemoteAddress());
		}
		while (true) {
			int sent = channel.send(data.slice(), remoteAddress);
			if (sent > 0) {
				log.debug("sent {} bytes", sent);
				break;
			}
		}
	}

	/**
	 * Send data down this connection.
	 *
	 * @param data
	 *            The data to be sent
	 * @throws EOFException
	 *             If the connection is closed
	 * @throws IOException
	 *             If there is an error sending the data
	 * @throws IllegalStateException
	 *             If the data buffer doesn't hold a message; zero-length
	 *             messages are not supported!
	 */
	public final void send(ByteBuffer data) throws IOException {
		if (!canSend) {
			throw new IOException("Remote host and/or port not set; "
					+ "data cannot be sent with this connection");
		}
		if (isClosed()) {
			throw new EOFException();
		}
		if (!data.hasRemaining()) {
			throw new IllegalStateException(
					"data buffer must have bytes to send");
		}
		doSend(data);
	}

	/**
	 * Send data down this connection.
	 *
	 * @param data
	 *            The data to be sent
	 * @param address
	 *            Where to send (must be non-{@code null})
	 * @param port
	 *            What port to send to (must be non-zero)
	 * @throws EOFException
	 *             If the connection is closed
	 * @throws IOException
	 *             If there is an error sending the data
	 * @throws IllegalStateException
	 *             If the data packet doesn't hold a real message; zero-length
	 *             messages are not supported!
	 */
	public final void sendTo(DatagramPacket data, InetAddress address, int port)
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
	 *            Where to send (must be non-{@code null})
	 * @param port
	 *            What port to send to (must be non-zero)
	 * @throws EOFException
	 *             If the connection is closed
	 * @throws IOException
	 *             If there is an error sending the data
	 * @throws IllegalStateException
	 *             If the data array doesn't hold a message; zero-length
	 *             messages are not supported!
	 */
	public final void sendTo(byte[] data, InetAddress address, int port)
			throws IOException {
		sendTo(wrap(data, 0, data.length), address, port);
	}

	/**
	 * Send data down this connection.
	 *
	 * @param data
	 *            The data to be sent
	 * @param address
	 *            Where to send (must be non-{@code null})
	 * @param port
	 *            What port to send to (must be non-zero)
	 * @throws EOFException
	 *             If the connection is closed
	 * @throws IOException
	 *             If there is an error sending the data
	 * @throws IllegalStateException
	 *             If the data buffer doesn't hold a message; zero-length
	 *             messages are not supported!
	 */
	public final void sendTo(ByteBuffer data, InetAddress address, int port)
			throws IOException {
		if (!canSend) {
			throw new IOException("Remote host address or port not set; "
					+ "data cannot be sent with this connection");
		}
		if (isClosed()) {
			throw new EOFException();
		}
		if (!data.hasRemaining()) {
			throw new IllegalStateException(
					"data buffer must have bytes to send");
		}
		doSendTo(data, address, port);
	}

	/**
	 * Send data down this connection.
	 *
	 * @param data
	 *            The data to be sent
	 * @param address
	 *            Where to send (must be non-{@code null})
	 * @param port
	 *            What port to send to (must be non-zero)
	 * @throws IOException
	 *             If there is an error sending the data
	 */
	void doSendTo(ByteBuffer data, InetAddress address, int port)
			throws IOException {
		InetSocketAddress addr = new InetSocketAddress(address, port);
		if (log.isDebugEnabled()) {
			logSend(data, addr);
		}
		while (true) {
			int sent = channel.send(data.slice(), addr);
			if (sent > 0) {
				log.debug("sent {} bytes", sent);
				break;
			}
		}
	}

	private void logSend(ByteBuffer data, SocketAddress addr) {
		log.debug("sending data of length {} to {}", data.remaining(), addr);
		log.debug("message data: {}", describe(data));
	}

	private void logRecv(ByteBuffer data, SocketAddress addr) {
		log.debug("received data of length {} from {}", data.remaining(), addr);
		log.debug("message data: {}", describe(data));
	}

	private List<String> describe(ByteBuffer data) {
		int pos = data.position();
		IntFunction<String> foo = i -> hexbyte(data.get(pos + i));
		return range(0, data.remaining()).mapToObj(foo).collect(toList());
	}

	@Override
	public boolean isConnected() {
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
	public void close() throws IOException {
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

	private SelectionKey remake(SelectionKey stale) {
		log.debug("remaking selection key");
		selectionKeyFactory.remove();
		stale.cancel();
		SelectionKey key = selectionKeyFactory.get();
		if (!key.isValid()) {
			throw new IllegalStateException(
					"newly manufactured selection key is invalid");
		}
		return key;
	}

	@Override
	public final boolean isReadyToReceive(Integer timeout) throws IOException {
		if (isClosed()) {
			log.debug("connection closed, so not ready to receive");
			return false;
		}
		int t = (timeout == null ? 0 : timeout);
		boolean r = readyToReceive(t);
		receivable = r;
		return r;
	}

	/**
	 * Determines if there is a message available to be received without
	 * blocking. <em>This method</em> may block until the timeout given, and a
	 * zero timeout means do not wait.
	 *
	 * @param timeout
	 *            How long to wait, in milliseconds.
	 * @return true when there is a message waiting to be received
	 * @throws IOException
	 *             If anything goes wrong.
	 */
	boolean readyToReceive(int timeout) throws IOException {
		SelectionKey key = selectionKeyFactory.get();
		if (!key.isValid()) {
			// Key is stale; try to remake it
			key = remake(key);
		}
		if (log.isDebugEnabled()) {
			log.debug("timout on UDP({} <--> {}) will happen at {} ({})",
					getLocalAddress(), getRemoteAddress(), timeout,
					key.interestOps());
		}
		int result;
		if (timeout == 0) {
			result = key.selector().selectNow();
		} else {
			result = key.selector().select(timeout);
		}

		if (log.isDebugEnabled()) {
			log.debug("wait result: select={}, valid={}, readable={}", result,
					key.isValid(), key.isValid() && key.isReadable());
		}
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
	public final void sendPortTriggerMessage(InetAddress host)
			throws IOException {
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
			la = getLocalAddress();
		} catch (IOException ignore) {
		}
		try {
			ra = getRemoteAddress();
		} catch (IOException ignore) {
		}
		return String.format("%s(%s <-%s-> %s)",
				getClass().getSimpleName().replaceAll("^.*\\.", ""), la,
				isClosed() ? "|" : "", ra);
	}
}
