package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.String.format;
import static java.net.InetAddress.getByName;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.manchester.spinnaker.connections.ConnectionListener;
import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.connections.model.MessageHandler;
import uk.ac.manchester.spinnaker.utils.DefaultMap;

/**
 * A simple transceiver for UDP connections.
 *
 * @author Donal Fellows
 */
public abstract class UDPTransceiver implements AutoCloseable {
	/**
	 * A map of port -> map of IP address -> (connection, listener) for UDP
	 * connections. Note listener might be <tt>null</tt> if the connection has
	 * not been listened to before.
	 * <p>
	 * Used to keep track of what connection is listening on what port to ensure
	 * only one type of traffic is received on any port for any interface
	 */
	private final Map<Integer, Map<InetAddress, Pair<?>>> connectionsByPort =
			new DefaultMap<>(HashMap::new);
	/**
	 * A map of class -> list of (connection, listener) for UDP connections that
	 * are listenable. Note that listener might be <tt>null</tt> if the
	 * connection has not be listened to before.
	 */
	private final Map<Class<?>, List<Pair<?>>> connectionsByClass =
			new DefaultMap<>(ArrayList::new);

	/**
	 * Add a connection to the collection of general connections managed by this
	 * class.
	 *
	 * @param connection
	 *            the connection to add
	 */
	protected abstract void addConnection(Connection connection);

	/**
	 * Add a connection to the list managed for UDP reception.
	 *
	 * @param connection
	 *            the connection to add
	 */
	final void registerConnection(UDPConnection<?> connection) {
		Pair<?> pair = new Pair<>(connection, null);
		connectionsByPort.get(connection.getLocalPort())
				.put(connection.getLocalIPAddress(), pair);
		connectionsByClass.get(connection.getClass()).add(pair);
	}

	private static final class Pair<MessageType> implements Cloneable {
		UDPConnection<MessageType> connection;
		ConnectionListener<MessageType> listener;

		Pair(UDPConnection<MessageType> connection,
				ConnectionListener<MessageType> listener) {
			this.connection = connection;
			this.listener = listener;
		}

		@Override
		public Pair<MessageType> clone() {
			return new Pair<>(connection, listener);
		}
	}

	/**
	 * How to manufacture new instances of a connection as required.
	 *
	 * @author Donal Fellows
	 * @param <Conn>
	 *            The type of connection being created.
	 */
	public interface UDPConnectionFactory<Conn extends UDPConnection<?>> {
		/**
		 * @return The type of connection this factory actually makes.
		 */
		Class<Conn> getClassKey();

		/**
		 * Make an instance with an OS-selected local port.
		 *
		 * @param localAddress
		 *            the local address to bind.
		 */
		Conn getInstance(String localAddress);

		/**
		 * Make an instance with a caller-selected local port.
		 *
		 * @param localAddress
		 *            the local address to bind.
		 * @param localPort
		 *            the local port to bind.
		 */
		Conn getInstance(String localAddress, int localPort);
	}

	@Override
	public void close() throws Exception {
		for (Map<?, Pair<?>> connections : connectionsByPort.values()) {
			for (Pair<?> p : connections.values()) {
				if (p.listener != null) {
					p.listener.close();
				}
			}
		}
	}

	private static final InetAddress WILDCARD_ADDRESS;
	static {
		try {
			WILDCARD_ADDRESS = InetAddress
					.getByAddress(new byte[] { 0, 0, 0, 0 });
			if (!WILDCARD_ADDRESS.isAnyLocalAddress()) {
				throw new RuntimeException(
						"wildcard address is not wildcard address?");
			}
		} catch (UnknownHostException e) {
			throw new RuntimeException("unexpected failure to initialise", e);
		}
	}

	/**
	 * Register a callback for a certain type of traffic to be received via UDP.
	 *
	 * @param callback
	 *            Function to be called when a packet is received
	 * @param connection_class
	 *            The class of connection to receive using
	 * @return The connection to be used
	 * @throws IllegalArgumentException
	 *             If basic sanity checks fail.
	 * @throws IOException
	 *             If the networking goes wrong.
	 */
	public final <T> UDPConnection<T> registerUDPListener(
			MessageHandler<T> callback,
			UDPConnectionFactory<? extends UDPConnection<T>> connection_factory)
			throws UnknownHostException {
		return registerUDPListener(callback, connection_factory, null, null);
	}

	/**
	 * Register a callback for a certain type of traffic to be received via UDP.
	 *
	 * @param callback
	 *            Function to be called when a packet is received
	 * @param connection_class
	 *            The class of connection to receive using
	 * @param local_port
	 *            The local UDP port to bind.
	 * @return The connection to be used
	 * @throws IllegalArgumentException
	 *             If basic sanity checks fail.
	 * @throws IOException
	 *             If the networking goes wrong.
	 */
	public final <T> UDPConnection<T> registerUDPListener(
			MessageHandler<T> callback,
			UDPConnectionFactory<? extends UDPConnection<T>> connection_factory,
			int local_port) throws UnknownHostException {
		return registerUDPListener(callback, connection_factory, local_port,
				null);
	}

	/**
	 * Register a callback for a certain type of traffic to be received via UDP.
	 *
	 * @param callback
	 *            Function to be called when a packet is received
	 * @param connection_class
	 *            The class of connection to receive using
	 * @param local_port
	 *            The optional port number to listen on; if not specified, an
	 *            existing connection will be used if possible, otherwise a
	 *            random free port number will be used
	 * @param localHost
	 *            The optional hostname or IP address to listen on; if not
	 *            specified, all interfaces will be used for listening
	 * @return The connection to be used
	 * @throws IllegalArgumentException
	 *             If basic sanity checks fail.
	 * @throws UnknownHostException
	 *             If the hostname is unresolvable.
	 */
	public final <T> UDPConnection<T> registerUDPListener(
			MessageHandler<T> callback,
			UDPConnectionFactory<? extends UDPConnection<T>> connection_factory,
			Integer local_port, String localHost) throws UnknownHostException {
		if (!UDPConnection.class
				.isAssignableFrom(connection_factory.getClassKey())) {
			throw new IllegalArgumentException(
					"the connection class must be a UDPConnection");
		}

		// normalise local_host to the IP address
		InetAddress addr = getByName(localHost == null ? "0.0.0.0" : localHost);

		// If the local port was specified
		Pair<T> pair;
		if (local_port != null) {
			pair = lookup(connection_factory.getClassKey(), addr, local_port);

			// Create a connection if there isn't already one
			if (pair.connection == null) {
				pair.connection = connection_factory
						.getInstance(addr.getHostAddress(), local_port);
				addConnection(pair.connection);
			}
		} else {
			pair = lookup(connection_factory.getClassKey(), addr);

			// Create a connection if there isn't already one
			if (pair.connection == null) {
				pair.connection = connection_factory
						.getInstance(addr.getHostAddress());
				addConnection(pair.connection);
			}
		}

		// Launch a listener if one is required
		if (pair.listener == null) {
			// Caller has guaranteed the type constraint
			@SuppressWarnings("resource")
			ConnectionListener<T> listener = new ConnectionListener<>(
					pair.connection);
			listener.start();
			pair.listener = listener;
			connectionsByPort.get(pair.connection.getLocalPort()).put(addr,
					pair);
		}
		connectionsByClass.get(connection_factory.getClassKey()).add(pair);
		pair.listener.addCallback(callback);
		return pair.connection;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> List<Pair<T>> getConnections(
			Class<? extends UDPConnection<T>> clazz) {
		return (List) connectionsByClass.get(clazz);
	}

	private <T> Pair<T> lookup(Class<? extends UDPConnection<T>> clazz,
			InetAddress addr) {
		List<Pair<T>> connections_of_class = getConnections(clazz);
		for (Pair<T> a : connections_of_class) {
			if (a.connection.getLocalIPAddress().equals(addr)) {
				if (a.listener == null) {
					a = a.clone();
				}
				return a;
			}
		}
		return new Pair<>(null, null);
	}

	@SuppressWarnings("unchecked")
	private <T> Pair<T> getPair(Map<InetAddress, Pair<?>> receivers,
			InetAddress addr, Class<? extends UDPConnection<T>> clazz) {
		Pair<?> p = receivers.get(addr);
		if (!clazz.isInstance(p.connection)) {
			throw new IllegalArgumentException(format(
					"A connection of class %s is already "
							+ "listening on this port on all " + "interfaces",
					p.connection.getClass()));
		}
		return (Pair<T>) p;
	}

	private <T> Pair<T> lookup(Class<? extends UDPConnection<T>> clazz,
			InetAddress addr, int port) {
		Map<InetAddress, Pair<?>> receivers = connectionsByPort.get(port);
		// If something is already listening on this port
		if (!receivers.isEmpty()) {
			if (addr.isAnyLocalAddress()) {
				/*
				 * If we are to listen on all interfaces and the listener is not
				 * on all interfaces, this is an error
				 */
				if (!receivers.containsKey(WILDCARD_ADDRESS)) {
					throw new IllegalArgumentException(
							"Another connection is already listening on this"
									+ " port");
				}
			} else {
				/*
				 * If we are to listen to a specific interface, and the listener
				 * is on all interfaces, this is an error
				 */
				if (receivers.containsKey(WILDCARD_ADDRESS)) {
					throw new RuntimeException("port " + port
							+ " already has conflicting wildcard listener");
				}
			}

			// If the type of an existing connection is wrong, this is an error
			if (receivers.containsKey(addr)) {
				Pair<T> p = getPair(receivers, addr, clazz);
				if (p.listener == null) {
					p = p.clone();
				}
				return p;
			}
		}
		return new Pair<>(null, null);
	}
}
