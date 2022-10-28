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

import static java.lang.String.format;
import static java.net.InetAddress.getByAddress;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Objects.requireNonNullElse;
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
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.IntFunction;

import org.slf4j.Logger;

import com.google.errorprone.annotations.ForOverride;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;

import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.connections.model.SocketHolder;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

/**
 * A connection to SpiNNaker over UDP/IPv4.
 *
 * @param <T>
 *            The type of message to be received. It's possible for the received
 *            information to even be metadata about the message, and not the
 *            content of the message.
 */
public abstract class UDPConnection<T>
		implements Connection, SocketHolder {
	private static final Logger log = getLogger(UDPConnection.class);

	private static final int RECEIVE_BUFFER_SIZE = 1048576;

	private static final int PING_COUNT = 5;

	private static final int PACKET_MAX_SIZE = 300;

	/**
	 * The type of traffic being sent on a socket.
	 *
	 * @see DatagramSocket#setTrafficClass(int)
	 */
	public enum TrafficClass {
		/** Minimise cost. */
		IPTOS_LOWCOST(0x02),
		/** Maximise reliability. */
		IPTOS_RELIABILITY(0x04),
		/** Maximise throughput. */
		IPTOS_THROUGHPUT(0x08),
		/** Minimise delay. */
		IPTOS_LOWDELAY(0x10);

		private final int value;

		TrafficClass(int value) {
			this.value = value;
		}
	}

	private boolean canSend;

	private Inet4Address remoteIPAddress;

	private InetSocketAddress remoteAddress;

	private final DatagramSocket socket;

	private int receivePacketSize = PACKET_MAX_SIZE;

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
	 *            and will throw an exception if used for sending.
	 * @param remotePort
	 *            The remote port to send packets to. If {@code remoteHost} is
	 *            {@code null}, this is ignored. If {@code remoteHost} is
	 *            specified, this must also be specified as non-zero for the
	 *            connection to allow sending.
	 * @param trafficClass
	 *            What sort of traffic is this socket going to send. If
	 *            {@code null}, no traffic class will be used. Receive-only
	 *            sockets should leave this as {@code null}.
	 * @throws IOException
	 *             If there is an error setting up the communication channel
	 */
	public UDPConnection(InetAddress localHost, Integer localPort,
			InetAddress remoteHost, Integer remotePort,
			TrafficClass trafficClass) throws IOException {
		canSend = (remoteHost != null && remotePort != null && remotePort > 0);
		socket = initialiseSocket(localHost, localPort, remoteHost, remotePort,
				trafficClass);
		if (log.isDebugEnabled()) {
			log.debug("{} socket created ({} <--> {})", getClass().getName(),
					localAddr(), remoteAddr());
		}
	}

	/**
	 * Make a connection where actual operations on the socket will be
	 * delegated. If you use this constructor, you <strong>must</strong>
	 * override {@link #isClosed()}, and possibly {@link #isConnected()} as
	 * well.
	 *
	 * @param canSend
	 *            Whether this is a connection that can send messages.
	 */
	UDPConnection(boolean canSend) {
		this.canSend = canSend;
		socket = null;
	}

	/**
	 * Set the maximum size of packet that can be received. Packets larger than
	 * this will be truncated. The default is large enough for any packet that
	 * is sent by SCAMP.
	 *
	 * @param receivePacketSize
	 *            The new maximum packet size.
	 */
	protected void setReceivePacketSize(int receivePacketSize) {
		this.receivePacketSize = receivePacketSize;
	}

	private static final InetSocketAddress ANY =
			new InetSocketAddress((InetAddress) null, 0);

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
	 * @param trafficClass
	 *            Traffic class, at least for sending. If {@code null}, no
	 *            traffic class will be set.
	 * @return The configured, connected socket.
	 * @throws IOException
	 *             If anything fails.
	 */
	@ForOverride
	DatagramSocket initialiseSocket(InetAddress localHost, Integer localPort,
			InetAddress remoteHost, Integer remotePort,
			TrafficClass trafficClass) throws IOException {
		// SpiNNaker only speaks IPv4
		var sock = new DatagramSocket(createLocalAddress(localHost, localPort));
		if (trafficClass != null) {
			sock.setTrafficClass(trafficClass.value);
		}
		sock.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
		if (canSend) {
			remoteIPAddress = (Inet4Address) remoteHost;
			remoteAddress = new InetSocketAddress(remoteIPAddress, remotePort);
			sock.connect(remoteAddress);
		}
		return sock;
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

	/**
	 * Get the local socket address. (Sockets have two ends, one local, one
	 * remote.)
	 * <p>
	 * This operation is <em>delegatable</em>; see
	 * {@link DelegatingSCPConnection}.
	 *
	 * @return The socket's local address
	 * @throws IOException
	 *             If the socket is closed.
	 */
	@ForOverride
	protected InetSocketAddress getLocalAddress() throws IOException {
		return (InetSocketAddress) socket.getLocalSocketAddress();
	}

	private InetSocketAddress localAddr() {
		try {
			return requireNonNullElse(getLocalAddress(), ANY);
		} catch (IOException e) {
			return ANY;
		}
	}

	/**
	 * Get the remote socket address. (Sockets have two ends, one local, one
	 * remote.)
	 * <p>
	 * This operation is <em>delegatable</em>; see
	 * {@link DelegatingSCPConnection}.
	 *
	 * @return The socket's remote address
	 * @throws IOException
	 *             If the socket is closed.
	 */
	@ForOverride
	protected InetSocketAddress getRemoteAddress() throws IOException {
		return (InetSocketAddress) socket.getRemoteSocketAddress();
	}

	private InetSocketAddress remoteAddr() {
		try {
			return requireNonNullElse(getRemoteAddress(), ANY);
		} catch (IOException e) {
			return ANY;
		}
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
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	public final ByteBuffer receive(Integer timeout)
			throws SocketTimeoutException, IOException, InterruptedException {
		if (timeout != null) {
			return receive(convertTimeout(timeout));
		}
		// Want to wait forever but the underlying engine won't...
		while (true) {
			try {
				return receive(convertTimeout(timeout));
			} catch (SocketTimeoutException e) {
				continue;
			}
		}
	}

	/**
	 * Receive data from the connection.
	 *
	 * @param timeout
	 *            The timeout in milliseconds
	 * @return The data received, in a little-endian buffer
	 * @throws SocketTimeoutException
	 *             If a timeout occurs before any data is received
	 * @throws EOFException
	 *             If the connection is closed
	 * @throws IOException
	 *             If an error occurs receiving the data
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	public final ByteBuffer receive(int timeout)
			throws SocketTimeoutException, IOException, InterruptedException {
		if (isClosed()) {
			throw new EOFException();
		}
		return doReceive(timeout);
	}

	/**
	 * Receive data from the connection.
	 * <p>
	 * This operation is <em>delegatable</em>; see
	 * {@link DelegatingSCPConnection}.
	 *
	 * @param timeout
	 *            The timeout in milliseconds
	 * @return The data received, in a little-endian buffer
	 * @throws SocketTimeoutException
	 *             If a timeout occurs before any data is received
	 * @throws IOException
	 *             If an error occurs receiving the data
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@ForOverride
	protected ByteBuffer doReceive(int timeout)
			throws SocketTimeoutException, IOException, InterruptedException {
		socket.setSoTimeout(timeout);
		var buffer = allocate(receivePacketSize);
		var pkt = new DatagramPacket(buffer.array(), receivePacketSize);
		socket.receive(pkt);
		buffer.position(pkt.getLength()).flip();
		logRecv(buffer, pkt.getSocketAddress());
		return buffer.order(LITTLE_ENDIAN);
	}

	/**
	 * Receive data from the connection along with the address where the data
	 * was received from.
	 *
	 * @param timeout
	 *            The timeout in milliseconds
	 * @return The datagram packet received; caller is responsible for only
	 *         accessing the valid part of the buffer.
	 * @throws SocketTimeoutException
	 *             If a timeout occurs before any data is received
	 * @throws EOFException
	 *             If the connection is closed
	 * @throws IOException
	 *             If an error occurs receiving the data
	 */
	public final UDPPacket receiveWithAddress(int timeout)
			throws SocketTimeoutException, IOException {
		if (isClosed()) {
			throw new EOFException();
		}
		return doReceiveWithAddress(timeout);
	}

	/**
	 * Receive data from the connection along with the address where the data
	 * was received from.
	 * <p>
	 * This operation is <em>delegatable</em>; see
	 * {@link DelegatingSCPConnection}.
	 *
	 * @param timeout
	 *            The timeout in milliseconds
	 * @return The datagram packet received
	 * @throws SocketTimeoutException
	 *             If a timeout occurs before any data is received
	 * @throws IOException
	 *             If an error occurs receiving the data
	 */
	@ForOverride
	protected UDPPacket doReceiveWithAddress(int timeout)
			throws SocketTimeoutException, IOException {
		socket.setSoTimeout(timeout);
		var buffer = allocate(receivePacketSize);
		var pkt = new DatagramPacket(buffer.array(), receivePacketSize);
		socket.receive(pkt);
		buffer.position(pkt.getLength()).flip();
		logRecv(buffer, pkt.getSocketAddress());
		return new UDPPacket(buffer.order(LITTLE_ENDIAN),
				(InetSocketAddress) pkt.getSocketAddress());
	}

	/**
	 * Receives a SpiNNaker message from this connection. Blocks until a message
	 * has been received.
	 *
	 * @return the received message
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws IllegalArgumentException
	 *             If one of the fields of the SpiNNaker message is invalid
	 */
	public T receiveMessage() throws IOException, InterruptedException {
		return receiveMessage(0);
	}

	/**
	 * Receives a SpiNNaker message from this connection. Blocks until a message
	 * has been received, or a timeout occurs.
	 *
	 * @param timeout
	 *            The time in seconds to wait for the message to arrive, or
	 *            until the connection is closed.
	 * @return the received message
	 * @throws IOException
	 *             If there is an error receiving the message
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws SocketTimeoutException
	 *             If there is a timeout during receiving
	 * @throws IllegalArgumentException
	 *             If one of the fields of the SpiNNaker message is invalid
	 */
	public abstract T receiveMessage(int timeout)
			throws IOException, InterruptedException;

	/**
	 * Create the actual message to send.
	 *
	 * @param data
	 *            The content of the message
	 * @param remoteAddress
	 *            Where to send it to
	 * @return The full packet to send.
	 */
	private static DatagramPacket formSendPacket(
			ByteBuffer data, InetSocketAddress remoteAddress) {
		if (!data.hasArray()) {
			// Yuck; must copy because can't touch the backing array
			var buffer = new byte[data.remaining()];
			data.duplicate().get(buffer);
			return new DatagramPacket(buffer, 0, buffer.length, remoteAddress);
		} else {
			// Unsafe, but we can get away with it as we send immediately
			// and never actually write to the array
			return new DatagramPacket(
					data.array(), data.arrayOffset() + data.position(),
					data.remaining(), remoteAddress);
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
	 *             If the data packet doesn't hold a real message; zero-length
	 *             messages are not supported!
	 */
	public final void send(DatagramPacket data) throws IOException {
		send(wrap(data.getData(), data.getOffset(), data.getLength()));
	}

	/**
	 * Send data down this connection.
	 * <p>
	 * This operation is <em>delegatable</em>; see
	 * {@link DelegatingSCPConnection}.
	 *
	 * @param data
	 *            The data to be sent; the position in this buffer will
	 *            <em>not</em> be updated by this method
	 * @throws IOException
	 *             If there is an error sending the data
	 */
	@ForOverride
	protected void doSend(ByteBuffer data) throws IOException {
		if (log.isDebugEnabled()) {
			logSend(data, getRemoteAddress());
		}
		socket.send(formSendPacket(data, remoteAddress));
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
	 * <p>
	 * This operation is <em>delegatable</em>; see
	 * {@link DelegatingSCPConnection}.
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
	@ForOverride
	protected void doSendTo(ByteBuffer data, InetAddress address, int port)
			throws IOException {
		var addr = new InetSocketAddress(address, port);
		if (log.isDebugEnabled()) {
			logSend(data, addr);
		}
		socket.send(formSendPacket(data, addr));
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
	@OverridingMethodsMustInvokeSuper
	public void close() throws IOException {
		if (socket == null) {
			return;
		}
		try {
			socket.disconnect();
		} catch (Exception e) {
			log.debug("failed to disconnect prior to close", e);
		}
		socket.close();
	}

	@Override
	public boolean isClosed() {
		return socket.isClosed();
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
		var triggerMessage = new SDPMessage(new SDPHeader(REPLY_NOT_EXPECTED,
				new CoreLocation(0, 0, 0), RUNNING_COMMAND_SDP_PORT));
		sendTo(triggerMessage.getMessageData(null), host, SCP_SCAMP_PORT);
	}

	@Override
	public String toString() {
		return format("%s(%s <-%s-> %s)",
				getClass().getSimpleName().replaceAll("^.*\\.", ""),
				localAddr(), isClosed() ? "|" : "", remoteAddr());
	}
}
