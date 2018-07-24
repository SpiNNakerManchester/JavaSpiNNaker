package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.net.InetAddress.getByName;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.machine.CPUState.RUN_TIME_EXCEPTION;
import static uk.ac.manchester.spinnaker.machine.CPUState.WATCHDOG;
import static uk.ac.manchester.spinnaker.messages.Constants.NO_ROUTER_DIAGNOSTIC_FILTERS;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_DEFAULT_FILTERS_MAX_POSITION;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_DIAGNOSTIC_FILTER_SIZE;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_FILTER_CONTROLS_OFFSET;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_REGISTER_BASE_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.Constants.SYSTEM_VARIABLE_BASE_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_BOOT_CONNECTION_DEFAULT_PORT;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.router_table_copy_address;
import static uk.ac.manchester.spinnaker.processes.FillProcess.DataType.WORD;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

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
import uk.ac.manchester.spinnaker.machine.CPUState;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.machine.MulticastRoutingEntry;
import uk.ac.manchester.spinnaker.machine.RoutingEntry;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.ReverseIPTag;
import uk.ac.manchester.spinnaker.machine.tags.Tag;
import uk.ac.manchester.spinnaker.messages.Constants;
import uk.ac.manchester.spinnaker.messages.bmp.GetBMPVersion;
import uk.ac.manchester.spinnaker.messages.bmp.ReadADC;
import uk.ac.manchester.spinnaker.messages.bmp.ReadFPGARegister;
import uk.ac.manchester.spinnaker.messages.bmp.WriteFPGARegister;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.CPUInfo;
import uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter;
import uk.ac.manchester.spinnaker.messages.model.HeapElement;
import uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics;
import uk.ac.manchester.spinnaker.messages.model.Signal;
import uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.messages.scp.ApplicationStop;
import uk.ac.manchester.spinnaker.messages.scp.IPTagClear;
import uk.ac.manchester.spinnaker.messages.scp.IPTagSet;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory.Response;
import uk.ac.manchester.spinnaker.messages.scp.ReverseIPTagSet;
import uk.ac.manchester.spinnaker.messages.scp.RouterClear;
import uk.ac.manchester.spinnaker.messages.scp.SendSignal;
import uk.ac.manchester.spinnaker.messages.scp.SetLED;
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
import uk.ac.manchester.spinnaker.processes.ReadMemoryProcess;
import uk.ac.manchester.spinnaker.processes.ReadRouterDiagnosticsProcess;
import uk.ac.manchester.spinnaker.processes.SendSingleBMPCommandProcess;
import uk.ac.manchester.spinnaker.processes.SendSingleSCPCommandProcess;
import uk.ac.manchester.spinnaker.processes.WriteMemoryFloodProcess;
import uk.ac.manchester.spinnaker.processes.WriteMemoryProcess;
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

	private Object get_sv_data(HasChipLocation chip,
			SystemVariableDefinition data_item) throws IOException, Exception {
		ByteBuffer buffer = readMemory(chip,
				SYSTEM_VARIABLE_BASE_ADDRESS + data_item.offset,
				data_item.type.value);
		switch (data_item.type) {
		case BYTE:
			return buffer.get();
		case SHORT:
			return buffer.getShort();
		case INT:
			return buffer.getInt();
		case LONG:
			return buffer.getLong();
		case BYTE_ARRAY:
			byte[] dst = (byte[]) data_item.getDefault();
			buffer.get(dst);
			return dst;
		default:
			// Unreachable
			return null;
		}
	}

	private ConnectionSelector<BMPConnection> bmpConnection(int cabinet,
			int frame) {
		// TODO Auto-generated method stub
		return null;
	}

	private byte get_next_nearest_neighbour_id() {
		// TODO Auto-generated method stub
		return 0;
	}

	private static final Integer TIMEOUT_DISABLED = null;
	private static final int DEFAULT_POLL_INTERVAL = 100;
	private static final Set<CPUState> DEFAULT_ERROR_STATES = unmodifiableSet(
			new HashSet<>(asList(RUN_TIME_EXCEPTION, WATCHDOG)));
	private static final int DEFAULT_CHECK_INTERVAL = 100;

	/**
	 * Read a register on a FPGA of a board. The meaning of the register's
	 * contents will depend on the FPGA's configuration.
	 *
	 * @param fpga_num
	 *            FPGA number (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register address to read to (will be rounded down to the
	 *            nearest 32-bit word boundary).
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 */
	public int readFPGARegister(int fpga_num, int register, int cabinet,
			int frame, int board) throws IOException, Exception {
		SendSingleBMPCommandProcess process = new SendSingleBMPCommandProcess(
				bmpConnection(cabinet, frame));
		return process.execute(
				new ReadFPGARegister(fpga_num, register, board)).fpgaRegister;
	}

	/**
	 * Write a register on a FPGA of a board. The meaning of setting the
	 * register's contents will depend on the FPGA's configuration.
	 *
	 * @param fpga_num
	 *            FPGA number (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register address to read to (will be rounded down to the
	 *            nearest 32-bit word boundary).
	 * @param value
	 *            the value to write into the FPGA register
	 * @param cabinet
	 *            cabinet: the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to write the FPGA register to
	 */
	public void writeFPGARegister(int fpga_num, int register, int value,
			int cabinet, int frame, int board) throws IOException, Exception {
		SendSingleBMPCommandProcess process = new SendSingleBMPCommandProcess(
				bmpConnection(cabinet, frame));
		process.execute(
				new WriteFPGARegister(fpga_num, register, value, board));
	}

	/**
	 * Read the ADC data.
	 *
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to request the ADC data from
	 * @return the FPGA's ADC data object
	 */
	public ADCInfo readADCData(int board, int cabinet, int frame)
			throws IOException, Exception {
		return new SendSingleBMPCommandProcess(bmpConnection(cabinet, frame))
				.execute(new ReadADC(board)).adcInfo;
	}

	/**
	 * Read the BMP version.
	 *
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to request the data from
	 * @return the sver from the BMP
	 */
	public VersionInfo readBMPVersion(int board, int cabinet, int frame)
			throws IOException, Exception {
		return new SendSingleBMPCommandProcess(bmpConnection(cabinet, frame))
				.execute(new GetBMPVersion(board)).versionInfo;
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param chip
	 *            The coordinates of the chip where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataStream
	 *            The stream of data that is to be written.
	 * @param numBytes
	 *            The amount of data to be written in bytes.
	 */
	public void writeMemory(HasChipLocation chip, int baseAddress,
			InputStream dataStream, int numBytes)
			throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		process.writeMemory(chip.getScampCore(), baseAddress, dataStream,
				numBytes);
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataStream
	 *            The stream of data that is to be written.
	 * @param numBytes
	 *            The amount of data to be written in bytes.
	 */
	public void writeMemory(HasCoreLocation core, int baseAddress,
			InputStream dataStream, int numBytes)
			throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		process.writeMemory(core, baseAddress, dataStream, numBytes);
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param chip
	 *            The coordinates of the chip where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataFile
	 *            The name of the file holding the data that is to be written.
	 */
	public void writeMemory(HasChipLocation chip, int baseAddress,
			File dataFile) throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		process.writeMemory(chip.getScampCore(), baseAddress, dataFile);
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataFile
	 *            The name of the file holding the data that is to be written.
	 */
	public void writeMemory(HasCoreLocation core, int baseAddress,
			File dataFile) throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		process.writeMemory(core, baseAddress, dataFile);
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param chip
	 *            The coordinates of the chip where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataWord
	 *            The word that is to be written.
	 */
	public void writeMemory(HasChipLocation chip, int baseAddress, int dataWord)
			throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		ByteBuffer b = allocate(4).order(LITTLE_ENDIAN);
		b.putInt(dataWord);
		process.writeMemory(chip.getScampCore(), baseAddress, b);
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataWord
	 *            The word that is to be written.
	 */
	public void writeMemory(HasCoreLocation core, int baseAddress, int dataWord)
			throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		ByteBuffer b = allocate(4).order(LITTLE_ENDIAN);
		b.putInt(dataWord);
		process.writeMemory(core, baseAddress, b);
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param chip
	 *            The coordinates of the core where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written. Should be a
	 *            {@link ByteBuffer}, positioned at the point where the data is
	 */
	public void writeMemory(HasChipLocation chip, int baseAddress,
			ByteBuffer data) throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		process.writeMemory(chip.getScampCore(), baseAddress, data);
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written. Should be a
	 *            {@link ByteBuffer}, positioned at the point where the data is
	 */
	public void writeMemory(HasCoreLocation core, int baseAddress,
			ByteBuffer data) throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		process.writeMemory(core, baseAddress, data);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be written
	 *            to
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataStream
	 *            The stream of data that is to be written.
	 * @param numBytes
	 *            The amount of data to be written in bytes.
	 */
	public void writeNeighbourMemory(HasChipLocation chip, int link,
			int baseAddress, InputStream dataStream, int numBytes)
			throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		process.writeLink(chip.getScampCore(), link, baseAddress, dataStream,
				numBytes);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param core
	 *            The coordinates of the core whose neighbour is to be written
	 *            to; the CPU to use is typically 0 (or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataStream
	 *            The stream of data that is to be written.
	 * @param numBytes
	 *            The amount of data to be written in bytes.
	 */
	public void writeNeighbourMemory(HasCoreLocation core, int link,
			int baseAddress, InputStream dataStream, int numBytes)
			throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		process.writeLink(core, link, baseAddress, dataStream, numBytes);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be written
	 *            to
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataFile
	 *            The name of the file holding the data that is to be written.
	 */
	public void writeNeighbourMemory(HasChipLocation chip, int link,
			int baseAddress, File dataFile) throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		process.writeLink(chip.getScampCore(), link, baseAddress, dataFile);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param core
	 *            The coordinates of the core whose neighbour is to be written
	 *            to; the CPU to use is typically 0 (or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataFile
	 *            The name of the file holding the data that is to be written.
	 */
	public void writeNeighbourMemory(HasCoreLocation core, int link,
			int baseAddress, File dataFile) throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		process.writeLink(core, link, baseAddress, dataFile);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be written
	 *            to
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataWord
	 *            The word that is to be written.
	 */
	public void writeNeighbourMemory(HasChipLocation chip, int link,
			int baseAddress, int dataWord) throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		ByteBuffer b = allocate(4).order(LITTLE_ENDIAN);
		b.putInt(dataWord);
		process.writeLink(chip.getScampCore(), link, baseAddress, b);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param core
	 *            The coordinates of the core whose neighbour is to be written
	 *            to; the CPU to use is typically 0 (or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataWord
	 *            The word that is to be written.
	 */
	public void writeNeighbourMemory(HasCoreLocation core, int link,
			int baseAddress, int dataWord) throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		ByteBuffer b = allocate(4).order(LITTLE_ENDIAN);
		b.putInt(dataWord);
		process.writeLink(core, link, baseAddress, b);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be written
	 *            to
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written. Should be a
	 *            {@link ByteBuffer}, positioned at the point where the data is
	 */
	public void writeNeighbourMemory(HasChipLocation chip, int link,
			int baseAddress, ByteBuffer data) throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		process.writeLink(chip.getScampCore(), link, baseAddress, data);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param core
	 *            The coordinates of the core whose neighbour is to be written
	 *            to; the CPU to use is typically 0 (or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written. Should be a
	 *            {@link ByteBuffer}, positioned at the point where the data is
	 */
	public void writeNeighbourMemory(HasCoreLocation core, int link,
			int baseAddress, ByteBuffer data) throws IOException, Exception {
		WriteMemoryProcess process = new WriteMemoryProcess(
				scamp_connection_selector);
		process.writeLink(core, link, baseAddress, data);
	}

	/**
	 * Write to the SDRAM of all chips.
	 *
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataStream
	 *            The stream of data that is to be written.
	 * @param numBytes
	 *            The amount of data to be written in bytes.
	 */
	public void writeMemoryFlood(int baseAddress, InputStream dataStream,
			int numBytes) throws IOException, Exception {
		WriteMemoryFloodProcess process = new WriteMemoryFloodProcess(
				scamp_connection_selector);
		// Ensure only one flood fill occurs at any one time
		synchronized (flood_write_lock) {
			// Start the flood fill
			byte nearest_neighbour_id = get_next_nearest_neighbour_id();
			process.writeMemory(nearest_neighbour_id, baseAddress, dataStream,
					numBytes);
		}
	}

	/**
	 * Write to the SDRAM of all chips.
	 *
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataFile
	 *            The name of the file holding the data that is to be written.
	 */
	public void writeMemoryFlood(int baseAddress, File dataFile)
			throws IOException, Exception {
		WriteMemoryFloodProcess process = new WriteMemoryFloodProcess(
				scamp_connection_selector);
		// Ensure only one flood fill occurs at any one time
		synchronized (flood_write_lock) {
			// Start the flood fill
			byte nearest_neighbour_id = get_next_nearest_neighbour_id();
			process.writeMemory(nearest_neighbour_id, baseAddress, dataFile);
		}
	}

	/**
	 * Write to the SDRAM of all chips.
	 *
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataWord
	 *            The word that is to be written.
	 */
	public void writeMemoryFlood(int baseAddress, int dataWord)
			throws IOException, Exception {
		WriteMemoryFloodProcess process = new WriteMemoryFloodProcess(
				scamp_connection_selector);
		// Ensure only one flood fill occurs at any one time
		synchronized (flood_write_lock) {
			// Start the flood fill
			byte nearest_neighbour_id = get_next_nearest_neighbour_id();
			ByteBuffer b = allocate(4).order(LITTLE_ENDIAN);
			b.putInt(dataWord);
			process.writeMemory(nearest_neighbour_id, baseAddress, b);
		}
	}

	/**
	 * Write to the SDRAM of all chips.
	 *
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written. Should be a
	 *            {@link ByteBuffer}, positioned at the point where the data is
	 */
	public void writeMemoryFlood(int baseAddress, ByteBuffer data)
			throws IOException, Exception {
		WriteMemoryFloodProcess process = new WriteMemoryFloodProcess(
				scamp_connection_selector);
		// Ensure only one flood fill occurs at any one time
		synchronized (flood_write_lock) {
			// Start the flood fill
			byte nearest_neighbour_id = get_next_nearest_neighbour_id();
			process.writeMemory(nearest_neighbour_id, baseAddress, data);
		}
	}

	/**
	 * Read some areas of SDRAM from the board.
	 *
	 * @param chip
	 *            The coordinates of the chip where the memory is to be read
	 *            from
	 * @param base_address
	 *            The address in SDRAM where the region of memory to be read
	 *            starts
	 * @param length
	 *            The length of the data to be read in bytes
	 * @return A little-endian buffer of data read
	 */
	public ByteBuffer readMemory(HasChipLocation core, int base_address,
			int length) throws IOException, Exception {
		return readMemory(core.getScampCore(), base_address, length);
	}

	/**
	 * Read some areas of SDRAM from the board.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is to be read
	 *            from
	 * @param base_address
	 *            The address in SDRAM where the region of memory to be read
	 *            starts
	 * @param length
	 *            The length of the data to be read in bytes
	 * @return A little-endian buffer of data read
	 */
	public ByteBuffer readMemory(HasCoreLocation core, int base_address,
			int length) throws IOException, Exception {
		ReadMemoryProcess process = new ReadMemoryProcess(
				scamp_connection_selector);
		return process.readMemory(core, base_address, length);
	}

	/**
	 * Read some areas of memory on a neighbouring chip using a LINK_READ SCP
	 * command.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be read from
	 * @param link
	 *            The link index to send the request to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory to be read
	 *            starts
	 * @param length
	 *            The length of the data to be read in bytes
	 * @return The data that has been read
	 */
	public ByteBuffer readNeighbourMemory(HasChipLocation chip, int link,
			int baseAddress, int length) throws IOException, Exception {
		return readNeighbourMemory(chip.getScampCore(), link, baseAddress,
				length);
	}

	/**
	 * Read some areas of memory on a neighbouring chip using a LINK_READ SCP
	 * command. If sent to a BMP, this command can be used to communicate with
	 * the FPGAs' debug registers.
	 *
	 * @param core
	 *            The coordinates of the chip whose neighbour is to be read
	 *            from, plus the CPU to use (typically 0, or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory to be read
	 *            starts
	 * @param length
	 *            The length of the data to be read in bytes
	 * @return The data that has been read
	 */
	public ByteBuffer readNeighbourMemory(HasCoreLocation core, int link,
			int baseAddress, int length) throws IOException, Exception {
		ReadMemoryProcess process = new ReadMemoryProcess(
				scamp_connection_selector);
		return process.readLink(core, link, baseAddress, length);
	}

	/**
	 * Sends a stop request for an app_id.
	 *
	 * @param app_id
	 *            The ID of the application to send to
	 */
	public void stopApplication(int app_id) throws IOException, Exception {
		if (!machine_off) {
			SendSingleSCPCommandProcess process = new SendSingleSCPCommandProcess(
					scamp_connection_selector);
			process.execute(new ApplicationStop(app_id));
		} else {
			log.warn("You are calling a app stop on a turned off machine. "
					+ "Please fix and try again");
		}
	}

	/**
	 * Waits for the specified cores running the given application to be in some
	 * target state or states. Handles failures.
	 *
	 * @param all_core_subsets
	 *            the cores to check are in a given sync state
	 * @param app_id
	 *            the application ID that being used by the simulation
	 * @param cpu_states
	 *            The expected states once the applications are ready; success
	 *            is when each application is in one of these states
	 */
	public void waitForCoresToBeInState(CoreSubsets all_core_subsets,
			int app_id, Set<CPUState> cpu_states) {
		waitForCoresToBeInState(all_core_subsets, app_id, cpu_states,
				TIMEOUT_DISABLED, DEFAULT_POLL_INTERVAL, DEFAULT_ERROR_STATES,
				DEFAULT_CHECK_INTERVAL);
	}

	/**
	 * Waits for the specified cores running the given application to be in some
	 * target state or states. Handles failures.
	 *
	 * @param all_core_subsets
	 *            the cores to check are in a given sync state
	 * @param app_id
	 *            the application ID that being used by the simulation
	 * @param cpu_states
	 *            The expected states once the applications are ready; success
	 *            is when each application is in one of these states
	 * @param timeout
	 *            The amount of time to wait in milliseconds for the cores to
	 *            reach one of the states, or <tt>null</tt> to wait for an
	 *            unbounded amount of time.
	 * @param time_between_polls
	 *            Time between checking the state, in milliseconds
	 * @param error_states
	 *            Set of states that the application can be in that indicate an
	 *            error, and so should raise an exception
	 * @param counts_between_full_check
	 *            The number of times to use the count signal before instead
	 *            using the full CPU state check
	 */
	public void waitForCoresToBeInState(CoreSubsets all_core_subsets,
			int app_id, Set<CPUState> cpu_states, Integer timeout,
			int time_between_polls, Set<CPUState> error_states,
			int counts_between_full_check) {
		// check that the right number of processors are in the states
		int processors_ready = 0;
		Long timeout_time = (timeout == null ? null
				: currentTimeMillis() + timeout);
		int tries = 0;
		while (processors_ready < all_core_subsets.size()
				&& (timeout_time == null
						|| currentTimeMillis() < timeout_time)) {
			// Get the number of processors in the ready states
			processors_ready = 0;
			for (CPUState cpu_state : cpu_states) {
				processors_ready += get_core_state_count(app_id, cpu_state);
			}

			// If the count is too small, check for error states
			if (processors_ready < all_core_subsets.size()) {
				for (CPUState cpu_state : error_states) {
					int error_cores = get_core_state_count(app_id, cpu_state);
					if (error_cores > 0) {
						throw new SpinnmanException(String.format(
								"%d cores have reached an error state %s",
								error_cores, cpu_state));
					}
				}

				/*
				 * If we haven't seen an error, increase the tries, and do a
				 * full check if required
				 */
				tries++;
				if (tries >= counts_between_full_check) {
					CoreSubsets cores_in_state = getCoresInState(
							all_core_subsets, cpu_states);
					processors_ready = cores_in_state.size();
					tries = 0;
				}

				// If we're still not in the correct state, wait a bit
				if (processors_ready < all_core_subsets.size()) {
					sleep(time_between_polls);
				}
			}
		}

		// If we haven't reached the final state, do a final full check
		if (processors_ready < all_core_subsets.size()) {
			CoreSubsets cores_in_state = getCoresInState(all_core_subsets,
					cpu_states);

			/*
			 * If we are sure we haven't reached the final state, report a
			 * timeout error
			 */
			if (cores_in_state.size() != all_core_subsets.size()) {
				throw new SpinnmanTimeoutException(
						format("waiting for cores {} to reach one of {}",
								all_core_subsets, cpu_states),
						timeout);
			}
		}
	}

	/**
	 * Get all cores that are in a given state.
	 *
	 * @param all_core_subsets
	 *            The cores to filter
	 * @param state
	 *            The states to filter on
	 * @return Core subsets object containing cores in the given state
	 */
	public CoreSubsets getCoresInState(CoreSubsets all_core_subsets,
			CPUState state) {
		return getCoresInState(all_core_subsets, singleton(state));
	}

	/**
	 * Get all cores that are in a given set of states.
	 *
	 * @param all_core_subsets
	 *            The cores to filter
	 * @param states
	 *            The states to filter on
	 * @return Core subsets object containing cores in the given states
	 */
	public CoreSubsets getCoresInState(CoreSubsets all_core_subsets,
			Set<CPUState> states) {
		Collection<CPUInfo> core_infos = getCPUInformation(all_core_subsets);
		CoreSubsets cores_in_state = new CoreSubsets();
		for (CPUInfo core_info : core_infos) {
			if (states.contains(core_info.getState())) {
				cores_in_state.addCore(core_info.asCoreLocation());
			}
		}
		return cores_in_state;
	}

	/**
	 * Get all cores that are not in a given state.
	 *
	 * @param all_core_subsets
	 *            The cores to filter
	 * @param state
	 *            The state to filter on
	 * @return Core subsets object containing cores not in the given state
	 */
	public Map<CoreLocation, CPUInfo> getCoresNotInState(
			CoreSubsets all_core_subsets, CPUState state) {
		return getCoresNotInState(all_core_subsets, singleton(state));
	}

	/**
	 * Get all cores that are not in a given set of states.
	 *
	 * @param all_core_subsets
	 *            The cores to filter
	 * @param states
	 *            The states to filter on
	 * @return Core subsets object containing cores not in the given states
	 */
	public Map<CoreLocation, CPUInfo> getCoresNotInState(
			CoreSubsets all_core_subsets, Set<CPUState> states) {
		Collection<CPUInfo> core_infos = getCPUInformation(all_core_subsets);
		Map<CoreLocation, CPUInfo> cores_not_in_state = new TreeMap<>();
		for (CPUInfo core_info : core_infos) {
			if (!states.contains(core_info.getState())) {
				cores_not_in_state.put(core_info.asCoreLocation(), core_info);
			}
		}
		return cores_not_in_state;
	}

	/**
	 * Send a signal to an application.
	 *
	 * @param appID
	 *            The ID of the application to send to
	 * @param signal
	 *            The signal to send
	 */
	public void sendSignal(int appID, Signal signal)
			throws IOException, Exception {
		SendSingleSCPCommandProcess process = new SendSingleSCPCommandProcess(
				scamp_connection_selector);
		process.execute(new SendSignal(appID, signal));
	}

	/**
	 * Set LED states.
	 *
	 * @param core
	 *            The coordinates of the core on which to set the LEDs
	 * @param led_states
	 *            A map from LED index to state with 0 being off, 1 on and 2
	 *            inverted.
	 */
	public void setLEDs(HasCoreLocation core, Map<Integer, Integer> led_states)
			throws IOException, Exception {
		SendSingleSCPCommandProcess process = new SendSingleSCPCommandProcess(
				scamp_connection_selector);
		process.execute(new SetLED(core, led_states));
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
		int base_address = (int) get_sv_data(chip, router_table_copy_address);
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
		int base_address = (int) get_sv_data(chip, router_table_copy_address);
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
