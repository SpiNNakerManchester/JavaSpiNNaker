/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.String.format;
import static java.net.InetAddress.getByAddress;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.IPV4_SIZE;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.ConnectionListener;
import uk.ac.manchester.spinnaker.connections.EIEIOConnection;
import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.connections.model.MessageHandler;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOHeader;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessage;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessageHandler;
import uk.ac.manchester.spinnaker.utils.DefaultMap;

/**
 * A simple transceiver for UDP connections. In particular, handles managing the
 * connection listeners for EIEIO connections and ensuring that connections are
 * not double-registered.
 *
 * @author Donal Fellows
 */
public abstract class UDPTransceiver implements Closeable {
	private static final Logger log = getLogger(UDPTransceiver.class);

	/**
	 * A map of port &rarr; map of IP address &rarr; (connection, listener) for
	 * UDP connections. Note listener might be {@code null} if the connection
	 * has not been listened to before.
	 * <p>
	 * Used to keep track of what connection is listening on what port to ensure
	 * only one type of traffic is received on any port for any interface
	 */
	private final Map<Integer, Map<InetAddress, Pair<?>>> connectionsByPort =
			new DefaultMap<>(HashMap::new);

	/**
	 * A list of (connection, listener) for EIEIO connections. Note that
	 * listener might be {@code null} if the connection has not be listened to
	 * before.
	 */
	private final List<Pair<EIEIOMessage<?>>> eieioListeners =
			new ArrayList<>();

	/**
	 * Add a connection to the collection of general connections managed by this
	 * class.
	 *
	 * @param connection
	 *            the connection to add
	 */
	protected abstract void addConnection(Connection connection);

	/**
	 * Add a connection to the list managed for UDP reception. Does not add a
	 * listener... yet.
	 *
	 * @param connection
	 *            the connection to add
	 */
	@SuppressWarnings("unchecked")
	final void registerConnection(UDPConnection<?> connection) {
		var pair = new Pair<>(connection);
		var addr = pair.addr();
		log.info("registering connection {} for {}", connection, addr);
		connectionsByPort.get(connection.getLocalPort()).put(addr, pair);
		if (connection instanceof EIEIOConnection) {
			eieioListeners.add((Pair<EIEIOMessage<?>>) pair);
		}
	}

	private final class Pair<T> implements AutoCloseable {
		final UDPConnection<T> connection;

		private ConnectionListener<T> listener;

		Pair(UDPConnection<T> connection) {
			this.connection = Objects.requireNonNull(connection);
		}

		private Pair(UDPConnection<T> connection, boolean register) {
			this(connection);
			if (register) {
				addConnection(this.connection);
			}
		}

		@SuppressWarnings("MustBeClosed")
		synchronized void initListener(InetAddress addr,
				MessageHandler<T> callback) {
			if (listener == null) {
				listener = new ConnectionListener<>(connection);
				log.info("launching listener for {}:{}",
						connection.getLocalIPAddress(),
						connection.getLocalPort());
				listener.start();
				connectionsByPort.get(connection.getLocalPort()).put(addr,
						this);
			}
			listener.addCallback(callback);
		}

		InetAddress addr() {
			return normalize(connection.getLocalIPAddress());
		}

		@Override
		public synchronized void close() {
			// Close the listener, not the connection
			if (listener != null) {
				listener.close();
				listener = null;
			}
		}
	}

	/**
	 * Shut down any {@linkplain ConnectionListener listeners} registered for
	 * the EIEIO connections. The connections are not closed; caller (or
	 * subclass) is responsible for that.
	 *
	 * @throws IOException
	 *             Not thrown. Subclasses may (do!) throw it.
	 */
	@Override
	public void close() throws IOException {
		for (var connections : connectionsByPort.values()) {
			for (var p : connections.values()) {
				p.close();
			}
		}
	}

	private static final InetAddress WILDCARD_ADDRESS;

	static {
		try {
			WILDCARD_ADDRESS = getByAddress(new byte[IPV4_SIZE]);
			if (!WILDCARD_ADDRESS.isAnyLocalAddress()) {
				throw new RuntimeException(
						"wildcard address is not wildcard address?");
			}
			if (!(WILDCARD_ADDRESS instanceof Inet4Address)) {
				throw new RuntimeException("wildcard address is not IPv4?");
			}
		} catch (UnknownHostException e) {
			throw new RuntimeException("unexpected failure to initialise", e);
		}
	}

	private static InetAddress normalize(InetAddress addr) {
		if (addr == null || addr.isAnyLocalAddress()) {
			return WILDCARD_ADDRESS;
		}
		return addr;
	}

	/** Like a {@link Supplier} but can throw. */
	@FunctionalInterface
	private interface ThrowingSupplier<T> {
		T get() throws IOException;
	}

	/**
	 * Wrap any IOException thrown into an UncheckedIOException.
	 * @param <T> Type of result
	 * @param s Produces result or throws.
	 * @return Result of s
	 * @throws UncheckedIOException If supplier throws IOException
	 */
	private static <T> T wrap(ThrowingSupplier<T> s) {
		try {
			return s.get();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Unwrap any UncheckedIOException thrown as its IOException.
	 * @param <T> Type of result
	 * @param s Produces result or throws.
	 * @return Result of s
	 * @throws IOException If supplier throws UncheckedIOException
	 */
	private static <T> T unwrap(Supplier<T> s) throws IOException {
		try {
			return s.get();
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	/**
	 * Register a callback for EIEIO traffic to be received.
	 *
	 * @param callback
	 *            Function to be called when a packet is received.
	 *            Must not be {@code null}.
	 * @param localPort
	 *            The optional port number to listen on; if not specified, an
	 *            existing connection will be used if possible, otherwise a
	 *            random free port number will be used
	 * @param localHost
	 *            The optional hostname or IP address to listen on; if not
	 *            specified, all interfaces will be used for listening
	 * @return The connection to be used
	 * @throws IllegalArgumentException
	 *             If basic sanity checks fail.
	 * @throws IOException
	 *             If the networking fails.
	 */
	public final EIEIOConnection registerEIEIOListener(
			EIEIOMessageHandler callback, int localPort,
			InetAddress localHost) throws IOException {
		Objects.requireNonNull(callback);
		// normalise local_host to the IP address
		var addr = normalize(localHost);
		var port = localPort == 0 ? null : localPort;

		// Look up or create the connection (and its containing pair)
		log.info("finding/creating connection listening on {}:{}",
				addr.getHostAddress(), localPort);
		var pair = unwrap(() -> lookup(addr, localPort).orElseGet(
				() -> new Pair<>(wrap(() -> newEieioConnection(addr, port)),
						true)));

		// Launch a listener if one is required
		pair.initListener(addr, callback);
		eieioListeners.add(pair);
		return (EIEIOConnection) pair.connection;
	}

	/**
	 * Create an EIEIO connection only available for listening (or directed
	 * sending towards a SpiNNaker board).
	 *
	 * @param localHost
	 *            The local IP address to bind to. If {@code null}, it defaults
	 *            to binding to all interfaces or a system-specified interface.
	 * @param localPort
	 *            The local port to bind to, {@code null} or between 1025 and
	 *            65535.
	 * @return The listen-only EIEIO connection.
	 * @throws IOException
	 *             If there is an error setting up the communication channel
	 */
	protected EIEIOConnection newEieioConnection(InetAddress localHost,
			Integer localPort) throws IOException {
		return new EIEIOConnection(localHost, localPort);
	}

	/**
	 * Register a callback for EIEIO traffic to be received.
	 *
	 * @param callback
	 *            Function to be called when a packet is received.
	 *            Must not be {@code null}.
	 * @param localPort
	 *            The local UDP port to bind.
	 * @return The connection to be used
	 * @throws IllegalArgumentException
	 *             If basic sanity checks fail.
	 * @throws IOException
	 *             If the networking fails.
	 */
	public final EIEIOConnection registerEIEIOListener(
			EIEIOMessageHandler callback, int localPort) throws IOException {
		return registerEIEIOListener(callback, localPort, null);
	}

	/**
	 * Register a callback for EIEIO traffic to be received.
	 *
	 * @param callback
	 *            Function to be called when a packet is received.
	 *            Must not be {@code null}.
	 * @return The connection to be used
	 * @throws IllegalArgumentException
	 *             If basic sanity checks fail.
	 * @throws IOException
	 *             If the networking fails.
	 */
	public final EIEIOConnection registerEIEIOListener(
			EIEIOMessageHandler callback) throws IOException {
		return registerEIEIOListener(callback, 0, null);
	}

	private Optional<Pair<EIEIOMessage<? extends EIEIOHeader>>> lookup(
			InetAddress addr, int port) {
		if (port == 0) {
			for (var a : eieioListeners) {
				if (a.addr().equals(addr)) {
					return Optional.of(a);
				}
			}
			return Optional.empty();
		}
		var receivers = connectionsByPort.get(port);
		if (receivers.isEmpty()) {
			return Optional.empty();
		}
		// Something is already listening on this port
		if (addr.isAnyLocalAddress()) {
			/*
			 * If we are to listen on all interfaces and the listener is not on
			 * all interfaces, this is an error
			 */
			if (!receivers.containsKey(WILDCARD_ADDRESS)) {
				throw new IllegalArgumentException(
						"Another connection is already listening on this port");
			}
		} else {
			/*
			 * If we are to listen to a specific interface, and the listener is
			 * on all interfaces, this is an error
			 */
			if (receivers.containsKey(WILDCARD_ADDRESS)) {
				throw new RuntimeException("port " + port
						+ " already has conflicting wildcard listener");
			}
		}

		if (receivers.containsKey(addr)) {
			@SuppressWarnings("unchecked")
			var p = (Pair<EIEIOMessage<? extends EIEIOHeader>>) receivers
					.get(addr);
			// This is the check for the unsafe cast above
			if (p.connection instanceof EIEIOConnection) {
				return Optional.of(p);
			}
			// If the type of an existing connection was wrong, this is an error
			throw new IllegalArgumentException(format(
					"A connection of class %s is already "
							+ "listening on this port on all interfaces",
					p.connection.getClass()));
		}
		return Optional.empty();
	}
}
