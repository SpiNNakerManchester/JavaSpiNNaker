package uk.ac.manchester.spinnaker.transceiver;

import static java.net.InetAddress.getByName;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.NO_ROUTER_DIAGNOSTIC_FILTERS;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_DEFAULT_FILTERS_MAX_POSITION;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_DIAGNOSTIC_FILTER_SIZE;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_FILTER_CONTROLS_OFFSET;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_REGISTER_BASE_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.Constants.SYSTEM_VARIABLE_BASE_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_BOOT_CONNECTION_DEFAULT_PORT;
import static uk.ac.manchester.spinnaker.processes.FillProcess.DataType.WORD;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.connections.ConnectionListener;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.connections.model.Listenable;
import uk.ac.manchester.spinnaker.connections.model.MulticastSender;
import uk.ac.manchester.spinnaker.connections.model.SCPReceiver;
import uk.ac.manchester.spinnaker.connections.model.SCPSender;
import uk.ac.manchester.spinnaker.connections.model.SDPSender;
import uk.ac.manchester.spinnaker.connections.model.SpinnakerBootReceiver;
import uk.ac.manchester.spinnaker.connections.model.SpinnakerBootSender;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.machine.MulticastRoutingEntry;
import uk.ac.manchester.spinnaker.machine.RoutingEntry;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.ReverseIPTag;
import uk.ac.manchester.spinnaker.machine.tags.Tag;
import uk.ac.manchester.spinnaker.messages.Constants;
import uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter;
import uk.ac.manchester.spinnaker.messages.model.HeapElement;
import uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics;
import uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition;
import uk.ac.manchester.spinnaker.messages.scp.IPTagClear;
import uk.ac.manchester.spinnaker.messages.scp.IPTagSet;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory.Response;
import uk.ac.manchester.spinnaker.messages.scp.ReverseIPTagSet;
import uk.ac.manchester.spinnaker.messages.scp.RouterClear;
import uk.ac.manchester.spinnaker.messages.scp.WriteMemory;
import uk.ac.manchester.spinnaker.processes.DeallocSDRAMProcess;
import uk.ac.manchester.spinnaker.processes.FillProcess;
import uk.ac.manchester.spinnaker.processes.FillProcess.DataType;
import uk.ac.manchester.spinnaker.processes.GetHeapProcess;
import uk.ac.manchester.spinnaker.processes.GetMulticastRoutesProcess;
import uk.ac.manchester.spinnaker.processes.GetTagsProcess;
import uk.ac.manchester.spinnaker.processes.LoadFixedRouteEntryProcess;
import uk.ac.manchester.spinnaker.processes.LoadMulticastRoutesProcess;
import uk.ac.manchester.spinnaker.processes.MallocSDRAMProcess;
import uk.ac.manchester.spinnaker.processes.Process.Exception;
import uk.ac.manchester.spinnaker.processes.ReadFixedRouteEntryProcess;
import uk.ac.manchester.spinnaker.processes.ReadRouterDiagnosticsProcess;
import uk.ac.manchester.spinnaker.processes.SendSingleSCPCommandProcess;
import uk.ac.manchester.spinnaker.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.selectors.MostDirectConnectionSelector;
import uk.ac.manchester.spinnaker.selectors.RoundRobinConnectionSelector;
import uk.ac.manchester.spinnaker.utils.DefaultMap;

/**
 * An encapsulation of various communications with the SpiNNaker board.
 * <p>
 * The methods of this class are designed to be thread-safe; thus you can make
 * multiple calls to the same (or different) methods from multiple threads and
 * expect each call to work as if it had been called sequentially, although the
 * order of returns is not guaranteed. Note also that with multiple connections
 * to the board, using multiple threads in this way may result in an increase in
 * the overall speed of operation, since the multiple calls may be made
 * separately over the set of given connections.
 */
public class Transceiver {
	private static final Logger log = getLogger(Transceiver.class);
	/** The version of the board being connected to. */
	private int version;
	private Machine machine;
	private MachineDimensions dimensions;
	/**
	 * A set of chips to ignore in the machine. Requests for a "machine" will
	 * have these chips excluded, as if they never existed. The processor IDs of
	 * the specified chips are ignored.
	 */
	private final Set<ChipLocation> ignore_chips = new HashSet<>();
	/**
	 * A set of cores to ignore in the machine. Requests for a "machine" will
	 * have these cores excluded, as if they never existed.
	 */
	private final Set<CoreLocation> ignore_cores = new HashSet<>();
	/**
	 * A set of links to ignore in the machine. Requests for a "machine" will
	 * have these links excluded, as if they never existed.
	 */
	private final Set<Object> ignore_links = new HashSet<>();// FIXME
	/**
	 * The maximum core ID in any discovered machine. Requests for a "machine"
	 * will only have core IDs up to and including this value.
	 */
	private Integer max_core_id;
	/**
	 * The max size each chip can say it has for SDRAM. (This is mainly used for
	 * debugging purposes.)
	 */
	private Integer max_sdram_size;

	private Integer iobuf_size;
	private AppIdTracker app_id_tracker;
	/**
	 * A set of the original connections. Used to determine what can be closed.
	 */
	private final Set<Connection> original_connections = new HashSet<>();
	/** A set of all connections. Used for closing. */
	private final Set<Connection> all_connections = new HashSet<>();
	/**
	 * A boot send connection. There can only be one in the current system, or
	 * otherwise bad things can happen!
	 */
	private SpinnakerBootSender boot_send_connection;
	/**
	 * A list of boot receive connections. These are used to listen for the
	 * pre-boot board identifiers.
	 */
	private final List<SpinnakerBootReceiver> boot_receive_connections = new ArrayList<>();
	/**
	 * A map of port -> map of IP address -> (connection, listener) for UDP
	 * connections. Note listener might be <tt>null</tt> if the connection has
	 * not been listened to before.
	 * <p>
	 * Used to keep track of what connection is listening on what port to ensure
	 * only one type of traffic is received on any port for any interface
	 */
	private final Map<Integer, Map<InetAddress, Pair<UDPConnection, ConnectionListener<?>>>> udp_receive_connections_by_port = new DefaultMap<>(
			HashMap::new);
	/**
	 * A map of class -> list of (connection, listener) for UDP connections that
	 * are listenable. Note that listener might be <tt>null</tt> if the
	 * connection has not be listened to before.
	 */
	private final Map<Class<?>, List<Pair<UDPConnection, ConnectionListener<?>>>> udp_listenable_connections_by_class = new DefaultMap<>(
			ArrayList::new);
	/**
	 * A list of all connections that can be used to send SCP messages.
	 * <p>
	 * Note that some of these might not be able to receive SCP; this could be
	 * useful if they are just using SCP to send a command that doesn't expect a
	 * response.
	 */
	private final List<SCPSender> scp_sender_connections = new ArrayList<>();
	/** A list of all connections that can be used to send SDP messages */
	private final List<SDPSender> sdp_sender_connections = new ArrayList<>();
	/**
	 * A list of all connections that can be used to send Multicast messages.
	 */
	private final List<MulticastSender> multicast_sender_connections = new ArrayList<>();
	/**
	 * A map of IP address -> SCAMP connection. These are those that can be used
	 * for setting up IP Tags.
	 */
	private final Map<InetAddress, UDPConnection> udp_scamp_connections = new HashMap<>();
	/**
	 * A list of all connections that can be used to send and receive SCP
	 * messages for SCAMP interaction.
	 */
	private final List<SCPConnection> scamp_connections = new ArrayList<>();
	/** The BMP connections */
	private final List<BMPConnection> bmp_connections = new ArrayList<>();
	/** connection selectors for the BMP processes. */
	private final Map<BMPCoords, ConnectionSelector<?>> bmp_connection_selectors = new HashMap<>();
	/** connection selectors for the SCP processes. */
	private final ConnectionSelector<SCPConnection> scamp_connection_selector;
	/** The nearest neighbour start ID */
	private int nearest_neighbour_id = 1;
	/** The nearest neighbour lock */
	private final Object nearest_neighbour_lock = new Object();
	/**
	 * A lock against multiple flood fill writes. This is needed as SCAMP cannot
	 * cope with this
	 */
	private final Object flood_write_lock = new Object();
	/**
	 * Lock against single chip executions. The condition should be acquired
	 * before the locks are checked or updated.
	 * <p>
	 * The write lock condition should also be acquired to avoid a flood fill
	 * during an individual chip execute.
	 */
	private final Map<ChipLocation, Object> chip_execute_locks = new DefaultMap<>(
			Object::new);
	private final Object chip_execute_lock_condition = new Object();
	private int n_chip_execute_locks = 0;
	private boolean machine_off = false;

	public Transceiver(int version) throws IOException {
		this(version, null, null, null, null, null, null, null);
	}

	public Transceiver(int version, Collection<Connection> connections,
			Collection<ChipLocation> ignore_chips,
			Collection<CoreLocation> ignore_cores,
			Collection<Object> ignore_links, Integer max_core_id,
			Collection<ConnectionDescriptor> scamp_connections,
			Integer max_sdram_size) throws IOException {
		this.version = version;
		if (ignore_chips != null) {
			this.ignore_chips.addAll(ignore_chips);
		}
		if (ignore_cores != null) {
			this.ignore_cores.addAll(ignore_cores);
		}
		if (ignore_links != null) {
			this.ignore_links.addAll(ignore_links);
		}
		this.max_core_id = max_core_id;
		this.max_sdram_size = max_sdram_size;

		if (connections == null) {
			connections = emptyList();
		}
		original_connections.addAll(connections);
		all_connections.addAll(connections);
		// if there has been SCAMP connections given, build them
		if (scamp_connections != null) {
			for (ConnectionDescriptor desc : scamp_connections) {
				connections.add(new SCPConnection(desc.chip, desc.hostname,
						desc.portNumber));
			}
		}
		scamp_connection_selector = identifyConnections(connections);
		checkBMPConnections();
	}

	private ConnectionSelector<SCPConnection> identifyConnections(
			Collection<Connection> connections) {
		for (Connection conn : connections) {
			// locate the only boot send conn
			if (conn instanceof SpinnakerBootSender) {
				if (boot_send_connection != null) {
					throw new IllegalArgumentException(
							"Only a single SpinnakerBootSender can be specified");
				}
				boot_send_connection = (SpinnakerBootSender) conn;
			}

			// locate any boot receiver connections
			if (conn instanceof SpinnakerBootReceiver) {
				boot_receive_connections.add((SpinnakerBootReceiver) conn);
			}

			// Locate any connections listening on a UDP port
			if (conn instanceof UDPConnection) {
				UDPConnection udpc = (UDPConnection) conn;
				udp_receive_connections_by_port.get(udpc.getLocalPort())
						.put(udpc.getLocalIPAddress(), new Pair<>(udpc, null));
				if (conn instanceof Listenable) {
					udp_listenable_connections_by_class.get(conn.getClass())
							.add(new Pair<>(udpc, null));
				}
			}

			/*
			 * Locate any connections that can send SCP (that are not BMP
			 * connections)
			 */
			if (conn instanceof SCPSender && !(conn instanceof BMPConnection)) {
				scp_sender_connections.add((SCPSender) conn);
			}

			// Locate any connections that can send SDP
			if (conn instanceof SDPSender) {
				sdp_sender_connections.add((SDPSender) conn);
			}

			// Locate any connections that can send Multicast
			if (conn instanceof MulticastSender) {
				multicast_sender_connections.add((MulticastSender) conn);
			}

			// Locate any connections that can send and receive SCP
			if (conn instanceof SCPSender && conn instanceof SCPReceiver) {
				// If it is a BMP connection, add it here
				if (conn instanceof BMPConnection) {
					BMPConnection bmpc = (BMPConnection) conn;
					bmp_connections.add(bmpc);
					bmp_connection_selectors.put(
							new BMPCoords(bmpc.cabinet, bmpc.frame),
							new RoundRobinConnectionSelector<>(
									singletonList(bmpc)));
				} else {
					if (conn instanceof SCPConnection) {
						scamp_connections.add((SCPConnection) conn);
					}
					// If also a UDP connection, add it here (for IP tags)
					if (conn instanceof UDPConnection) {
						UDPConnection udpc = (UDPConnection) conn;
						udp_scamp_connections.put(udpc.getRemoteIPAddress(),
								udpc);
					}
				}
			}
		}

		// update the transceiver with the connection selectors.
		return new MostDirectConnectionSelector<SCPConnection>(machine,
				scamp_connections);
	}

	private void checkBMPConnections() {
		// TODO Auto-generated method stub

	}

	/**
	 * Get the connections for talking to a board.
	 *
	 * @param connection:
	 *            directly gives the connection to use. May be <tt>null</tt>
	 * @param board_address
	 *            the address of the board to talk to. May be <tt>null</tt>
	 * @return List of length 1 or 0 (the latter only if the search for the
	 *         given board address fails).
	 */
	private Collection<SCPConnection> get_connection_list(
			SCPConnection connection, InetAddress boardAddress) {
		if (connection != null) {
			return singletonList(connection);
		} else if (boardAddress == null) {
			return scamp_connections;
		}
		connection = locateSpinnakerConnection(boardAddress);
		if (connection == null) {
			return emptyList();
		}
		return singletonList(connection);
	}

	private int get_sv_data(HasChipLocation chip,
			SystemVariableDefinition data_item) throws IOException, Exception {
		// FIXME
		return struct.unpack_from(data_item.type.struct_code,
				readMemory(chip,
						SYSTEM_VARIABLE_BASE_ADDRESS + data_item.offset,
						data_item.type.value))[0];
	}

	/**
	 * Find a connection that matches the given board host name.
	 *
	 * @param hostname
	 *            The host name of the Ethernet connection on the board
	 * @return A connection for the given IP address, or <tt>null</tt> if no
	 *         such connection exists
	 */
	public SCPConnection locateSpinnakerConnection(String hostname) {
		try {
			return locateSpinnakerConnection(getByName(hostname));
		} catch (UnknownHostException e) {
			return null;
		}
	}

	/**
	 * Find a connection that matches the given board IP address
	 *
	 * @param boardAddress
	 *            The IP address of the Ethernet connection on the board
	 * @return A connection for the given IP address, or <tt>null</tt> if no
	 *         such connection exists
	 */
	public SCPConnection locateSpinnakerConnection(InetAddress boardAddress) {
		return (SCPConnection) udp_scamp_connections.get(boardAddress);
	}

	/**
	 * Set up an IP tag.
	 *
	 * @param ipTag
	 *            The tag to set up; note board address can be None, in which
	 *            case, the tag will be assigned to all boards
	 */
	public void setIPTag(IPTag ipTag) throws IOException, Exception {
		// Check that the tag has a port assigned
		if (ipTag.getPort() == null) {
			throw new IllegalArgumentException(
					"The tag port must have been set");
		}

		/*
		 * Get the connections. If the tag specifies a connection, use that,
		 * otherwise apply the tag to all connections
		 */
		Collection<SCPConnection> connections = get_connection_list(null,
				ipTag.getBoardAddress());
		if (connections == null || connections.isEmpty()) {
			throw new IllegalArgumentException(
					"The given board address is not recognised");
		}

		for (SCPConnection connection : connections) {
			// Convert the host string
			InetAddress host = ipTag.getBoardAddress();
			if (host == null || host.isAnyLocalAddress()
					|| host.isLoopbackAddress()) {
				host = connection.getLocalIPAddress();
			}

			SendSingleSCPCommandProcess process = new SendSingleSCPCommandProcess(
					scamp_connection_selector);
			process.execute(new IPTagSet(connection.getChip(),
					host.getAddress(), ipTag.getPort(), ipTag.getTag(),
					ipTag.isStripSDP()));
		}
	}

	/**
	 * Set up a reverse IP tag.
	 *
	 * @param reverse_ip_tag
	 *            The reverse tag to set up; note board_address can be None, in
	 *            which case, the tag will be assigned to all boards
	 */
	public void setReverseIPTag(ReverseIPTag reverse_ip_tag)
			throws IOException, Exception {
		if (requireNonNull(reverse_ip_tag).getPort() == SCP_SCAMP_PORT
				|| reverse_ip_tag
						.getPort() == UDP_BOOT_CONNECTION_DEFAULT_PORT) {
			throw new IllegalArgumentException(String.format(
					"The port number for the reverse IP tag conflicts with"
							+ " the SpiNNaker system ports ({} and {})",
					SCP_SCAMP_PORT, UDP_BOOT_CONNECTION_DEFAULT_PORT));
		}

		/*
		 * Get the connections. If the tag specifies a connection, use that,
		 * otherwise apply the tag to all connections
		 */
		Collection<SCPConnection> connections = get_connection_list(null,
				reverse_ip_tag.getBoardAddress());
		if (connections == null || connections.isEmpty()) {
			throw new IllegalArgumentException(
					"The given board address is not recognised");
		}

		for (SCPConnection connection : connections) {
			SendSingleSCPCommandProcess process = new SendSingleSCPCommandProcess(
					scamp_connection_selector);
			process.execute(new ReverseIPTagSet(connection.getChip(),
					reverse_ip_tag.getDestination(), reverse_ip_tag.getPort(),
					reverse_ip_tag.getTag(), reverse_ip_tag.getPort()));
		}
	}

	/**
	 * Clear the setting of an IP tag.
	 *
	 * @param tag
	 *            The tag ID
	 */
	public void clearIPTag(int tag) throws IOException, Exception {
		clearIPTag(tag, null, null);
	}

	/**
	 * Clear the setting of an IP tag.
	 *
	 * @param tag
	 *            The tag ID
	 * @param connection
	 *            Connection where the tag should be cleared. If not specified,
	 *            all SCPSender connections will send the message to clear the
	 *            tag
	 */
	public void clearIPTag(int tag, SCPConnection connection)
			throws IOException, Exception {
		clearIPTag(tag, requireNonNull(connection), null);
	}

	/**
	 * Clear the setting of an IP tag.
	 *
	 * @param tag
	 *            The tag ID
	 * @param board_address
	 *            Board address where the tag should be cleared.
	 */
	public void clearIPTag(int tag, InetAddress board_address)
			throws IOException, Exception {
		clearIPTag(tag, null, requireNonNull(board_address));
	}

	/**
	 * Clear the setting of an IP tag.
	 *
	 * @param tag
	 *            The tag ID
	 * @param connection
	 *            Connection where the tag should be cleared. If not specified,
	 *            all SCPSender connections will send the message to clear the
	 *            tag
	 * @param board_address
	 *            Board address where the tag should be cleared. If not
	 *            specified, all SCPSender connections will send the message to
	 *            clear the tag
	 */
	public void clearIPTag(int tag, SCPConnection connection,
			InetAddress board_address) throws IOException, Exception {
		for (SCPConnection conn : get_connection_list(connection,
				board_address)) {
			SendSingleSCPCommandProcess process = new SendSingleSCPCommandProcess(
					scamp_connection_selector);
			process.execute(new IPTagClear(conn.getChip(), tag));
		}
	}

	/**
	 * Get the current set of tags that have been set on the board using all
	 * SCPSender connections.
	 *
	 * @return An iterable of tags
	 */
	public List<Tag> getTags() throws IOException, Exception {
		List<Tag> all_tags = new ArrayList<>();
		for (SCPConnection conn : get_connection_list(null, null)) {
			GetTagsProcess process = new GetTagsProcess(
					scamp_connection_selector);
			all_tags.addAll(process.getTags(conn));
		}
		return all_tags;
	}

	/**
	 * Get the current set of tags that have been set on the board.
	 *
	 * @param connection
	 *            Connection from which the tags should be received.
	 * @return An iterable of tags
	 */
	public List<Tag> getTags(SCPConnection connection)
			throws IOException, Exception {
		List<Tag> all_tags = new ArrayList<>();
		for (SCPConnection conn : get_connection_list(connection, null)) {
			GetTagsProcess process = new GetTagsProcess(
					scamp_connection_selector);
			all_tags.addAll(process.getTags(conn));
		}
		return all_tags;
	}

	/**
	 * Allocates a chunk of SDRAM on a chip on the machine.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to allocate memory
	 * @param size
	 *            the amount of memory to allocate in bytes
	 * @return the base address of the allocated memory
	 */
	public int mallocSDRAM(HasChipLocation chip, int size)
			throws IOException, Exception {
		return mallocSDRAM(chip, size, 0, 0);
	}

	/**
	 * Allocates a chunk of SDRAM on a chip on the machine.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to allocate memory
	 * @param size
	 *            the amount of memory to allocate in bytes
	 * @param app_id
	 *            The ID of the application with which to associate the routes.
	 * @return the base address of the allocated memory
	 */
	public int mallocSDRAM(HasChipLocation chip, int size, int app_id)
			throws IOException, Exception {
		return mallocSDRAM(chip, size, app_id, 0);
	}

	/**
	 * Allocates a chunk of SDRAM on a chip on the machine
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to allocate memory
	 * @param size
	 *            the amount of memory to allocate in bytes
	 * @param app_id
	 *            The ID of the application with which to associate the routes.
	 * @param tag
	 *            the tag for the SDRAM, a 8-bit (chip-wide) tag that can be
	 *            looked up by a SpiNNaker application to discover the address
	 *            of the allocated block.
	 * @return the base address of the allocated memory
	 */
	public int mallocSDRAM(HasChipLocation chip, int size, int app_id, int tag)
			throws IOException, Exception {
		MallocSDRAMProcess process = new MallocSDRAMProcess(
				scamp_connection_selector);
		return process.mallocSDRAM(chip, size, app_id, tag);
	}

	/**
	 * Free allocated SDRAM.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to free memory
	 * @param base_address
	 *            The base address of the allocated memory
	 * @param app_id
	 *            The app ID of the allocated memory
	 */
	public void freeSDRAM(HasChipLocation chip, int base_address, int app_id)
			throws IOException, Exception {
		DeallocSDRAMProcess process = new DeallocSDRAMProcess(
				scamp_connection_selector);
		process.deallocSDRAM(chip, app_id, base_address);
	}

	/**
	 * Free all SDRAM allocated to a given application ID.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to free memory
	 * @param app_id
	 *            The app ID of the allocated memory
	 * @return The number of blocks freed
	 */
	public int freeSDRAMByAppID(HasChipLocation chip, int app_id)
			throws IOException, Exception {
		DeallocSDRAMProcess process = new DeallocSDRAMProcess(
				scamp_connection_selector);
		return process.deallocSDRAM(chip, app_id);
	}

	/**
	 * Load a set of multicast routes on to a chip associated with the default
	 * application ID.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to load the routes
	 * @param routes
	 *            An iterable of multicast routes to load
	 */
	public void loadMulticastRoutes(HasChipLocation chip,
			Collection<MulticastRoutingEntry> routes)
			throws IOException, Exception {
		LoadMulticastRoutesProcess process = new LoadMulticastRoutesProcess(
				scamp_connection_selector);
		process.loadRoutes(chip, routes, 0);
	}

	/**
	 * Load a set of multicast routes on to a chip
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to load the routes
	 * @param routes
	 *            An iterable of multicast routes to load
	 * @param app_id
	 *            The ID of the application with which to associate the routes.
	 */
	public void loadMulticastRoutes(HasChipLocation chip,
			Collection<MulticastRoutingEntry> routes, int app_id)
			throws IOException, Exception {
		LoadMulticastRoutesProcess process = new LoadMulticastRoutesProcess(
				scamp_connection_selector);
		process.loadRoutes(chip, routes, app_id);
	}

	/**
	 * Loads a fixed route routing table entry onto a chip's router.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to load the route
	 * @param fixed_route
	 *            the route for the fixed route entry on this chip
	 */
	public void loadFixedRoute(HasChipLocation chip, RoutingEntry fixed_route)
			throws IOException, Exception {
		LoadFixedRouteEntryProcess process = new LoadFixedRouteEntryProcess(
				scamp_connection_selector);
		process.loadFixedRoute(chip, fixed_route);
	}

	/**
	 * Loads a fixed route routing table entry onto a chip's router.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to load the route
	 * @param fixed_route
	 *            the route for the fixed route entry on this chip
	 * @param app_id
	 *            The ID of the application with which to associate the route.
	 */
	public void loadFixedRoute(HasChipLocation chip, RoutingEntry fixed_route,
			int app_id) throws IOException, Exception {
		LoadFixedRouteEntryProcess process = new LoadFixedRouteEntryProcess(
				scamp_connection_selector);
		process.loadFixedRoute(chip, fixed_route, app_id);
	}

	/**
	 * Reads a fixed route routing table entry from a chip's router.
	 *
	 * @param chip
	 *            The coordinate of the chip from which to read the route.
	 * @return the route as a fixed route entry
	 */
	public RoutingEntry readFixedRoute(HasChipLocation chip)
			throws IOException, Exception {
		ReadFixedRouteEntryProcess process = new ReadFixedRouteEntryProcess(
				scamp_connection_selector);
		return process.readFixedRoute(chip);
	}

	/**
	 * Reads a fixed route routing table entry from a chip's router.
	 *
	 * @param chip
	 *            The coordinate of the chip from which to read the route.
	 * @param app_id
	 *            The ID of the application associated the route.
	 * @return the route as a fixed route entry
	 */
	public RoutingEntry readFixedRoute(HasChipLocation chip, int app_id)
			throws IOException, Exception {
		ReadFixedRouteEntryProcess process = new ReadFixedRouteEntryProcess(
				scamp_connection_selector);
		return process.readFixedRoute(chip, app_id);
	}

	/**
	 * Get the current multicast routes set up on a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip from which to get the routes
	 * @return An iterable of multicast routes
	 */
	public List<MulticastRoutingEntry> getMulticastRoutes(HasChipLocation chip)
			throws IOException, Exception {
		int base_address = get_sv_data(chip,
				SystemVariableDefinition.router_table_copy_address);
		GetMulticastRoutesProcess process = new GetMulticastRoutesProcess(
				scamp_connection_selector);
		return process.getRoutes(chip, base_address);
	}

	/**
	 * Get the current multicast routes set up on a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip from which to get the routes
	 * @param appID
	 *            The ID of the application to filter the routes for.
	 * @return An iterable of multicast routes
	 */
	public List<MulticastRoutingEntry> getMulticastRoutes(HasChipLocation chip,
			int appID) throws IOException, Exception {
		int base_address = get_sv_data(chip,
				SystemVariableDefinition.router_table_copy_address);
		GetMulticastRoutesProcess process = new GetMulticastRoutesProcess(
				scamp_connection_selector, appID);
		return process.getRoutes(chip, base_address);
	}

	/**
	 * Remove all the multicast routes on a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip on which to clear the routes
	 */
	public void clearMulticastRoutes(HasChipLocation chip)
			throws IOException, Exception {
		SendSingleSCPCommandProcess process = new SendSingleSCPCommandProcess(
				scamp_connection_selector);
		process.execute(new RouterClear(chip));
	}

	/**
	 * Get router diagnostic information from a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip from which to get the information
	 * @return The router diagnostic information
	 */
	public RouterDiagnostics getRouterDiagnostics(HasChipLocation chip)
			throws IOException, Exception {
		ReadRouterDiagnosticsProcess process = new ReadRouterDiagnosticsProcess(
				scamp_connection_selector);
		return process.getRouterDiagnostics(chip);
	}

	/**
	 * Sets a router diagnostic filter in a router.
	 *
	 * @param chip
	 *            the address of the router in which this filter is being set
	 * @param position
	 *            the position in the list of filters where this filter is to be
	 *            added, between 0 and 15 (note that positions 0 to 11 are used
	 *            by the default filters, and setting these positions will
	 *            result in a warning).
	 * @param diagnosticFilter
	 *            the diagnostic filter being set in the position.
	 */
	public void setRouterDiagnosticFilter(HasChipLocation chip, int position,
			DiagnosticFilter diagnosticFilter) throws IOException, Exception {
		if (position < 0 || position > NO_ROUTER_DIAGNOSTIC_FILTERS) {
			throw new IllegalArgumentException(
					"the range of the position of a router filter is 0 and 16.");
		}
		if (position <= ROUTER_DEFAULT_FILTERS_MAX_POSITION) {
			log.warn("You are planning to change a filter which is set by "
					+ "default. By doing this, other runs occurring on this "
					+ "machine will be forced to use this new configuration "
					+ "until the machine is reset. Please also note that "
					+ "these changes will make the the reports from ybug not "
					+ "correct. This has been executed and is trusted that "
					+ "the end user knows what they are doing.");
		}

		int memory_position = (ROUTER_REGISTER_BASE_ADDRESS
				+ ROUTER_FILTER_CONTROLS_OFFSET
				+ position * ROUTER_DIAGNOSTIC_FILTER_SIZE);
		SendSingleSCPCommandProcess process = new SendSingleSCPCommandProcess(
				scamp_connection_selector);
		ByteBuffer b = ByteBuffer.allocate(4).order(LITTLE_ENDIAN)
				.putInt(diagnosticFilter.getFilterWord());
		process.execute(new WriteMemory(chip, memory_position, b));
	}

	/**
	 * Gets a router diagnostic filter from a router.
	 *
	 * @param chip
	 *            the address of the router from which this filter is being
	 *            retrieved
	 * @param position
	 *            the position in the list of filters where this filter is to be
	 *            read from
	 * @return The diagnostic filter read
	 */
	public DiagnosticFilter getRouterDiagnosticFilter(HasChipLocation chip,
			int position) throws IOException, Exception {
		if (position < 0 || position > NO_ROUTER_DIAGNOSTIC_FILTERS) {
			throw new IllegalArgumentException(
					"the range of the position of a router filter is 0 and 16.");
		}
		int memory_position = ROUTER_REGISTER_BASE_ADDRESS
				+ ROUTER_FILTER_CONTROLS_OFFSET
				+ position * ROUTER_DIAGNOSTIC_FILTER_SIZE;
		SendSingleSCPCommandProcess process = new SendSingleSCPCommandProcess(
				scamp_connection_selector);
		Response response = process
				.execute(new ReadMemory(chip, memory_position, 4));
		return new DiagnosticFilter(response.data.getInt());
	}

	/**
	 * Clear router diagnostic information on a chip. Resets and enables all
	 * diagnostic counters.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 */
	public void clearRouterDiagnosticCounters(HasChipLocation chip)
			throws IOException, Exception {
		clearRouterDiagnosticCounters(chip, false,
				range(0, 16).boxed().collect(toList()));
	}

	/**
	 * Clear router diagnostic information on a chip. Resets all diagnostic
	 * counters.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @param enable
	 *            True (default) if the counters should be enabled
	 */
	public void clearRouterDiagnosticCounters(HasChipLocation chip,
			boolean enable) throws IOException, Exception {
		clearRouterDiagnosticCounters(chip, enable,
				range(0, 16).boxed().collect(toList()));
	}

	/**
	 * Clear router diagnostic information on a chip. Resets and enables all the
	 * numbered counters.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @param counterIDs
	 *            The IDs of the counters to reset and enable; each must be
	 *            between 0 and 15
	 */
	public void clearRouterDiagnosticCounters(HasChipLocation chip,
			Iterable<Integer> counterIDs) throws IOException, Exception {
		clearRouterDiagnosticCounters(chip, false, counterIDs);
	}

	/**
	 * Clear router diagnostic information on a chip
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @param enable
	 *            True (default) if the counters should be enabled
	 * @param counterIDs
	 *            The IDs of the counters to reset and enable if enable is True;
	 *            each must be between 0 and 15
	 */
	public void clearRouterDiagnosticCounters(HasChipLocation chip,
			boolean enable, Iterable<Integer> counterIDs)
			throws IOException, Exception {
		int clear_data = 0;
		for (int counter_id : requireNonNull(counterIDs)) {
			if (counter_id < 0 || counter_id > 15) {
				throw new IllegalArgumentException(
						"Diagnostic counter IDs must be between 0 and 15");
			}
			clear_data |= 1 << counter_id;
		}
		if (enable) {
			for (int counter_id : counterIDs) {
				clear_data |= 1 << counter_id + 16;
			}
		}
		SendSingleSCPCommandProcess process = new SendSingleSCPCommandProcess(
				scamp_connection_selector);
		ByteBuffer b = ByteBuffer.allocate(4).order(LITTLE_ENDIAN)
				.putInt(clear_data);
		process.execute(new WriteMemory(chip, 0xf100002c, b));
	}

	/**
	 * Get the contents of the SDRAM heap on a given chip.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 */
	public List<HeapElement> getHeap(HasChipLocation chip)
			throws IOException, Exception {
		return getHeap(chip, SystemVariableDefinition.sdram_heap_address);
	}

	/**
	 * Get the contents of the given heap on a given chip.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @param heap
	 *            The SystemVariableDefinition which is the heap to read
	 */
	public List<HeapElement> getHeap(HasChipLocation chip,
			SystemVariableDefinition heap) throws IOException, Exception {
		GetHeapProcess process = new GetHeapProcess(scamp_connection_selector);
		return process.getBlocks(chip, heap);
	}

	/**
	 * Fill some memory with repeated data.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @param baseAddress
	 *            The address at which to start the fill
	 * @param repeatValue
	 *            The data to repeat
	 * @param size
	 *            The number of bytes to fill. Must be compatible with the data
	 *            type i.e. if the data type is WORD, the number of bytes must
	 *            be divisible by 4
	 * @throws IOException
	 * @throws Exception
	 */
	public void fillMemory(HasChipLocation chip, int baseAddress,
			int repeatValue, int size) throws Exception, IOException {
		fillMemory(chip, baseAddress, repeatValue, size, WORD);
	}

	/**
	 * Fill some memory with repeated data.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @param baseAddress
	 *            The address at which to start the fill
	 * @param repeatValue
	 *            The data to repeat
	 * @param size
	 *            The number of bytes to fill. Must be compatible with the data
	 *            type i.e. if the data type is WORD, the number of bytes must
	 *            be divisible by 4
	 * @param dataType
	 *            The type of data to fill.
	 * @throws IOException
	 * @throws Exception
	 */
	public void fillMemory(HasChipLocation chip, int baseAddress,
			int repeatValue, int size, DataType dataType)
			throws Exception, IOException {
		FillProcess process = new FillProcess(scamp_connection_selector);
		process.fillMemory(chip, baseAddress, repeatValue, size, dataType);
	}

	public static class ConnectionDescriptor {
		String hostname;
		Integer portNumber;
		ChipLocation chip;
	}

	static final class BMPCoords {
		final int cabinet;
		final int frame;

		BMPCoords(int cabinet, int frame) {
			this.cabinet = cabinet;
			this.frame = frame;
		}

		@Override
		public int hashCode() {
			return cabinet << 16 | frame;
		}

		@Override
		public boolean equals(Object o) {
			if (o != null && o instanceof BMPCoords) {
				BMPCoords b = (BMPCoords) o;
				return cabinet == b.cabinet && frame == b.frame;
			}
			return false;
		}
	}

	static class Pair<A, B> {
		final A a;
		final B b;

		public Pair(A a, B b) {
			this.a = a;
			this.b = b;
		}
	}
}
